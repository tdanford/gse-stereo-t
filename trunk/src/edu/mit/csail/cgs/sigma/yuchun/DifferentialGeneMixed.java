package edu.mit.csail.cgs.sigma.yuchun;

import java.util.*;
import java.util.logging.*;
import java.io.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import Jama.Matrix;

import edu.mit.csail.cgs.cgstools.oldregression.Datapoints;
import edu.mit.csail.cgs.cgstools.oldregression.MappedValuation;
import edu.mit.csail.cgs.cgstools.oldregression.PredictorSet;
import edu.mit.csail.cgs.cgstools.oldregression.Regression;
import edu.mit.csail.cgs.datasets.chipchip.ChipChipData;
import edu.mit.csail.cgs.datasets.chipchip.SQLData;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.StacieExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.SudeepExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.models.DiffGeneModel;
import edu.mit.csail.cgs.sigma.expression.regression.*;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.sigma.genes.GeneNameAssociation;
import edu.mit.csail.cgs.sigma.genes.GeneOrthologyEntry;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.viz.charting.JFreeChartPaintableAdapter;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;

/**
 * modified based on  DifferentialGeneRegression.java
 * 
 * 
 */
public class DifferentialGeneMixed {
    
    public static void main(String[] args) { 
    	run_all_expts_with_replicate("s288c_array");
        //run_all_expts(args);
    	//create_ranked_lists();
    	
//    	BaseExpressionProperties eprops = new SudeepExpressionProperties();     
//    	DifferentialGeneMixed reg = new DifferentialGeneMixed(eprops, "s288c_diploid");
//    	reg.loadData();
    	//reg.countProbes();
    	//reg.printData("YLR158C");

        
/*    	BaseExpressionProperties eprops = new SudeepExpressionProperties();
        
    	DifferentialGeneMixed reg = new DifferentialGeneMixed(eprops, "sigma_mat_a");
        reg.loadData();
        
        GeneAnnotationProperties gprops = new GeneAnnotationProperties();
        GeneNameAssociation assoc = gprops.getGeneNameAssociation("sigma");
        
        Set<String> names = new TreeSet<String>();
        names.add("PHO84");
        names.add("PHD1");
        names.add("YAP6");
        names.add("NDD1");
        names.add("GTS1");
        
        for(String name : names) { 
            for(String id : assoc.getIDs(name)) { 
                reg.viewGeneRegression(reg.genes.get(id));
            }
        }
        */
    }
    
    public static void create_ranked_lists() { 
        //BaseExpressionProperties eprops = new StacieExpressionProperties();
    	SudeepExpressionProperties eprops = new SudeepExpressionProperties();
/*        for(String exptKey : eprops.getExptKeys()) { 
        	DifferentialGeneAnalysis reg = new DifferentialGeneMixed(eprops, exptKey);
*/
    	String exptKey = "sigma_diploid";
    	int flanking = 300;
    	DifferentialGeneMixed reg = new DifferentialGeneMixed(eprops, exptKey);
    	reg.loadData();
        	
        	Vector<Pair<Gene,Double>> ranked = reg.getRankedGenes();
        	try { 
        		File rankedFile = eprops.getRankedDifferentialFile(exptKey);
        		PrintStream ps = new PrintStream(new FileOutputStream(rankedFile));
        		int i = 0;
        		for(Pair<Gene,Double> pair : ranked) {
        			i += 1;
        			Gene g=pair.getFirst();
        			String gCoord=g.getChrom()+":"+(g.getStart()-flanking)+"-"+(g.getEnd()+flanking);
        			ps.println(gCoord);
        		}
        		ps.close();
        	} catch(IOException ie) { 
        		ie.printStackTrace(System.err);
        	}
        	reg.closeData();
       // }    	
    }
	
	public static void run_all_expts(String[] args) { 
		SudeepExpressionProperties eprops = new SudeepExpressionProperties();
        //BaseExpressionProperties eprops = new StacieExpressionProperties();
        
        for(String exptKey : eprops.getExptKeys()) { 
        	DifferentialGeneMixed reg = new DifferentialGeneMixed(eprops, exptKey);
        	reg.loadData();
        	reg.closeData();
        }
	}
	public static void run_all_expts_with_replicate(String repKey) { 
		SudeepExpressionProperties eprops = new SudeepExpressionProperties();
        
        for(String exptKey : eprops.getStrainExptKeys("s288c")) { 
        	DifferentialGeneMixed reg = new DifferentialGeneMixed(eprops, exptKey);
        	reg.getReplicateData(repKey);
        	reg.loadData();
        	reg.closeData();
        }
	}
	private SudeepExpressionProperties eprops;
	private SigmaProperties sprops;
	private Logger logger;
	private String strain, exptKey;
	private ChipChipData data;
	private boolean ipData;
	private int segmentNumProbes;
	
	private Map<String,double[]> geneFGCoeffs, geneBGCoeffs;
	private Map<String,Gene> genes;
	
	public DifferentialGeneMixed(SudeepExpressionProperties ep, String key) { 
		eprops = ep;
		sprops = eprops.getSigmaProperties();
		logger = eprops.getLogger("DifferentialGeneMixed");
		exptKey = key;
		strain = eprops.parseStrainFromExptKey(exptKey);
		String cells = eprops.parseCellsFromExptKey(exptKey);
		//ChipChipLocator loc = eprops.getLocator(exptKey);
		ChipChipLocator loc = eprops.getLocator(strain, cells, "s288c_array");
		data = loc.createObject();
		
		ipData = eprops.isIPData(exptKey);
		segmentNumProbes = eprops.getMinSegmentProbes();

		geneFGCoeffs = new TreeMap<String,double[]>();
		geneBGCoeffs = new TreeMap<String,double[]>();
		genes = new TreeMap<String,Gene>();
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.analysis.DifferentialGeneAnalysis#loadData()
	 */
	public void loadData() { 
	    loadGenes();
	    File diffFile = eprops.getDifferentialRegressionFile(exptKey);
	    try {
	        if(diffFile.exists()) { 
	            loadCoeffs(diffFile);
	        } else { 
	            runRegressions();
	            saveCoeffs(diffFile);
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	        logger.log(Level.SEVERE, String.format("IOException: %s", e.getMessage()));
	    }
	}
	
	/**
	 * Writes the coefficients to a given file, so that I don't have to re-create them
	 * every time I want to re-run my analysis.  
	 * 
	 * @param f  The file into which the coefficients are saved.
	 * @throws IOException
	 */
	public void saveCoeffs(File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));

		logger.log(Level.INFO, "Load Orthologous Genes ...");
		GeneAnnotationProperties gp = new GeneAnnotationProperties(sprops, "default");
		File orthFile = gp.getOrthologousGenesFile();
		Map<String, GeneOrthologyEntry> orthGenes = new HashMap<String, GeneOrthologyEntry>();
		try {
			Parser<GeneOrthologyEntry> goes = new Parser<GeneOrthologyEntry>(orthFile, 
					new GeneOrthologyEntry.ParsingMapper());
			
			while(goes.hasNext()) { 
				GeneOrthologyEntry goe = goes.next();
				orthGenes.put(goe.getGeneName(), goe);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		logger.log(Level.INFO, "Saving data ...");
		for(String g : geneFGCoeffs.keySet()) { 
			ps.print(g);
			double[] coeffs = geneFGCoeffs.get(g);
			for(int i = 0; i < coeffs.length; i++) { 
				ps.print("\t" + coeffs[i]);
			}
			coeffs = geneBGCoeffs.get(g);
			for(int i = 0; i < coeffs.length; i++) { 
				ps.print(" \t" + coeffs[i]);
			}
			Gene gene = genes.get(g);
			ps.print(" \t" +convertFlagsToInt(orthGenes.get(g)));
			ps.print(" \t" +(gene.getEnd()-gene.getStart()));
			
			ps.print(" \t" +gp.getGeneNameAssociation(strain).getName(g));
			
			int flanking = 300;
			String gCoord=gene.getChrom()+":"+(gene.getStart()-flanking)+"-"+(gene.getEnd()+flanking);
			ps.print(" \t" +gCoord);
			ps.println();
		}
		
		ps.close();
	}
	private int convertFlagsToInt(GeneOrthologyEntry orth){
		if (orth==null){
			return -1;
		}
		int flag=2;	// other flags
		if (orth.hasFlag(GeneOrthologyEntry.Flag.PERFECT)){
			flag = 0;
		}else if (orth.hasFlag(GeneOrthologyEntry.Flag.UNKNOWN)){
			flag = 1;
		}
		return flag;
	}
	/**
	 * Loads the coefficients from the file.  Assumes that the genes have already been
	 * loaded (presumably with loadGenes()).  
	 * 
	 * @param f
	 * @throws IOException
	 */
	public void loadCoeffs(File f) throws IOException {
		geneFGCoeffs.clear();
		geneBGCoeffs.clear();
		
		Parser<Pair<String,double[]>> parser = new Parser<Pair<String,double[]>>(f, 
				new Mapper<String,Pair<String,double[]>>() {
					public Pair<String, double[]> execute(String line) {
						String[] a = line.split("\\s+");
						int paramNum = 6;
						double[] c = new double[paramNum];
						String name = a[0];
						for(int i = 1; i <=paramNum; i++) {
							c[i-1] = Double.parseDouble(a[i]);
						}
						return new Pair<String,double[]>(name, c);
					} 
		});
		
		while(parser.hasNext()) { 
			Pair<String,double[]> pc = parser.next();
			double[] tc = pc.getLast();
			double[] fg = new double[tc.length/2];
			double[] bg = new double[tc.length/2];
			
			for(int i = 0; i < fg.length; i++) { fg[i] = tc[i]; }
			for(int i = 0; i < bg.length; i++) { bg[i] = tc[i+fg.length]; }
			
			geneFGCoeffs.put(pc.getFirst(), fg);
			geneBGCoeffs.put(pc.getFirst(), bg);
		}
	}
    
    /* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.analysis.DifferentialGeneAnalysis#hasGene(java.lang.String)
	 */
    public boolean hasGene(String id) { 
        return genes.containsKey(id);
    }
	
	public boolean hasGeneRegression(String id) { 
		return geneFGCoeffs.containsKey(id);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.analysis.DifferentialGeneAnalysis#getGeneDiffExpression(java.lang.String)
	 */
	public Double getGeneDiffExpression(String id) { 
		double[] fg = geneFGCoeffs.get(id), bg = geneBGCoeffs.get(id);
		if (fg==null||bg==null){
			return 0.0;
		}
		double fg_b = fg[0], bg_b = bg[0];
		double fg_m = fg[1], bg_m = bg[1];
		//Gene g = genes.get(id);
		//compare the (gene coordinate) midpoint value on the line
		// x is normalized to (0 to 1)
		return (fg_b+fg_m*.5) - (bg_b+bg_m*.5);
	}

	public double[] getGeneDiffComponents(String id) {
		if(!geneFGCoeffs.containsKey(id)) { 
			throw new IllegalArgumentException(id);
		}
		double[] components = new double[3];
		double[] fg = geneFGCoeffs.get(id), bg = geneBGCoeffs.get(id);
		double fg_b = fg[0], bg_b = bg[0];
		double diffExpr = getGeneDiffExpression(id);
		
		components[0] = fg_b; 
		components[1] = bg_b;
		components[2] = diffExpr;
		
		return components;
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.analysis.DifferentialGeneAnalysis#getRankedGenes()
	 */
    public Vector<Pair<Gene,Double>> getRankedGenes() { 
        Vector<Pair<Gene,Double>> v = new Vector<Pair<Gene,Double>>();
        TreeSet<ValuedGene> vgenes = new TreeSet<ValuedGene>();
        for(String id : geneFGCoeffs.keySet()) { 
            Gene gene = genes.get(id);
            vgenes.add(new ValuedGene(gene, getGeneDiffExpression(id)));
        }
        
        for(ValuedGene vg : vgenes) { 
            v.add(new Pair<Gene,Double>(vg.gene, vg.value));
        }
        
        return v;
    }
    
    public Vector<Pair<Gene,Double>> getChangedGenes(double lower, double upper){
    	Vector<Pair<Gene,Double>> changedGenes =getUpGenes(upper);
    	changedGenes.addAll(getDownGenes(lower));
    	return changedGenes;
    }
    public Vector<Pair<Gene,Double>> getDownGenes(double lower){
        Vector<Pair<Gene,Double>> v = new Vector<Pair<Gene,Double>>();
        TreeSet<ValuedGene> vgenes = new TreeSet<ValuedGene>();
        for(String id : geneFGCoeffs.keySet()) { 
            Gene gene = genes.get(id);
            double diff = getGeneDiffExpression(id);
            if (diff<lower){
            	vgenes.add(new ValuedGene(gene, diff));
            }
        }
        
        for(ValuedGene vg : vgenes) { 
            v.add(new Pair<Gene,Double>(vg.gene, vg.value));
        }
        return v;
    }

    public Vector<Pair<Gene,Double>> getUpGenes(double upper){
        Vector<Pair<Gene,Double>> v = new Vector<Pair<Gene,Double>>();
        TreeSet<ValuedGene> vgenes = new TreeSet<ValuedGene>();
        for(String id : geneFGCoeffs.keySet()) { 
            Gene gene = genes.get(id);
            double diff = getGeneDiffExpression(id);
            if (diff>upper){
            	vgenes.add(new ValuedGene(gene, diff));
            }
        }
        
        for(ValuedGene vg : vgenes) { 
            v.add(new Pair<Gene,Double>(vg.gene, vg.value));
        }
        return v;
    }
    /**
     * Loads the genes and their coordinates from the database.  Stores them by 
     * their String ID, which should be unique to each gene.
     */
	public void loadGenes() { 
		genes.clear();

		GeneGenerator gener = sprops.getGeneGenerator(strain);
		ChromRegionIterator chromer = new ChromRegionIterator(sprops.getGenome(strain));
		Iterator<Region> chroms = 
			new MapperIterator<NamedRegion,Region>(new CastingMapper<NamedRegion,Region>(), chromer);
		Iterator<Gene> gitr = new ExpanderIterator<Region,Gene>(gener, chroms);
		while(gitr.hasNext()) { 
			Gene g = gitr.next();
			genes.put(g.getID(), g);
		}
	}
	
	public void countProbes() {

	    File f = eprops.getDifferentialRegressionFile(exptKey);
	    try{
		PrintStream ps = new PrintStream(new FileOutputStream(f));

		for(String gname : genes.keySet()) { 
			Gene g = genes.get(gname);
	        try {
	            data.window(g.getChrom(), g.getStart(), g.getEnd());
		        ps.print(String.format("%s\t%d\n", gname, data.getCount()));
		        
	        } catch (NotFoundException e) {
	            e.printStackTrace();
	        }
		}
	    }catch( IOException e){
            e.printStackTrace();
        }
	    
	}
	
	public void printData(String geneID){
        try {
        	Gene g = genes.get(geneID);
            data.window(g.getChrom(), g.getStart(), g.getEnd());
            char strand = g.getStrand();
            if(data.getCount() < segmentNumProbes) {
                return ;
            }
            
            double width = (double)g.getWidth();
            for(int i = 0; i < data.getCount(); i++) { 
                int pos = data.getPos(i);

                for(int j = 0; j < data.getReplicates(i); j++) {
                    
                    if(data.getStrand(i, j) == strand) {
                        
                        String name = String.format("%s:%d:rep%d:%s", g.getChrom(), pos, j, strand);
                        int offsetDiff = strand == '+' ? pos-g.getStart() : g.getEnd()-pos;
                        double xvalue = (double)offsetDiff / width;
                        boolean fg=true;
                        double yvalue = ipData == fg ? data.getIP(i, j) : data.getWCE(i, j);
						yvalue = Math.log(yvalue);
                        
                        if(!Double.isNaN(yvalue) && !Double.isInfinite(yvalue)) {
                            System.out.println(geneID+"\t"+ name+"\t"+data.getIP(i, j)+"\t"+data.getWCE(i, j));
                        }
                    }
                }
            }
            
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
	}
	public void getReplicateData(String repKey){
		String cells = eprops.parseCellsFromExptKey(exptKey);
		ChipChipLocator loc = eprops.getLocator(strain, cells, repKey);
		data = loc.createObject();
	}
	
	/**
	 * Runs all the regressions, ab initio, for each gene.  Clears out the geneCoeffs 
	 * table if it was already filled, and then re-fills it.
	 */
	public void runRegressions() {
		geneFGCoeffs.clear();
		geneBGCoeffs.clear();
		
		for(String gname : genes.keySet()) { 
			Gene g = genes.get(gname);
			Regression fgreg = createRegression(g, true);
			Regression bgreg = createRegression(g, false);				
			
			if(fgreg != null && bgreg != null) { 
				String id = g.getID();
				
				// foreground
		        Matrix betaHat = fgreg.calculateBetaHat();
		        double s2 = fgreg.calculateS2(betaHat);
		        double rms = s2 / (double)fgreg.getSize();
		        
		        double intercept = betaHat.get(0,0);
		        double slope = betaHat.get(1, 0);
		        
		        double[] params = new double[3];
		        params[0] = intercept; params[1] = slope; params[2] = rms;
		        
		        geneFGCoeffs.put(id, params);		    		        

				// background
		        betaHat = bgreg.calculateBetaHat();
		        s2 = bgreg.calculateS2(betaHat);
		        rms = s2 / (double)bgreg.getSize();
		        
		        intercept = betaHat.get(0,0);
		        slope = betaHat.get(1, 0);
		        
		        params = new double[3];
		        params[0] = intercept; params[1] = slope; params[2] = rms;
		        
		        geneBGCoeffs.put(id, params);
		        
		        logger.log(Level.INFO, String.format("%s: \t%.3f", id, getGeneDiffExpression(id)));
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.analysis.DifferentialGeneAnalysis#closeData()
	 */
	public void closeData() { 
		if(data instanceof SQLData) { 
			((SQLData)data).close();
		}
		data = null;
	}
	
	private void addSeries(DefaultXYDataset ds, String name, Vector<RegressionPoint> pts) { 
        double[][] array = new double[2][pts.size()];
        int i = 0;
        for(RegressionPoint pt : pts) { 
            array[0][i] = pt.x;
            array[1][i] = pt.y;
            i++;
        }
        ds.addSeries(name, array);		
	}
    
    public void viewGeneRegression(Gene g) { 
        Vector<RegressionPoint> fgpts = createRegressionPoints(g, true);
        Vector<RegressionPoint> bgpts = createRegressionPoints(g, false);
        DefaultXYDataset ds = new DefaultXYDataset();
        addSeries(ds, String.format("%s FG", g.getID()), fgpts);
        addSeries(ds, String.format("%s BG", g.getID()), bgpts);
        
        JFreeChart chart = ChartFactory.createScatterPlot(String.format("%s Regression", g.getID()), 
                ipData ? "WCE" : "IP", ipData ? "IP" : "WCE", ds, PlotOrientation.HORIZONTAL, 
                true, false, false);
        
        XYPlot plot = chart.getXYPlot();
        ValueAxis axis0 = plot.getDomainAxis();
        ValueAxis axis1 = plot.getRangeAxis();
        
        double lower = 0.0;
        double upper = 1.0;
        lower = Math.min(lower, axis0.getLowerBound());
        lower = Math.min(lower, axis1.getLowerBound());
        upper = Math.max(upper, axis0.getUpperBound());
        upper = Math.max(upper, axis1.getUpperBound());
        
        axis0.setRange(lower, upper);
        axis1.setRange(lower, upper);
        
        JFreeChartPaintableAdapter adapter = new JFreeChartPaintableAdapter(chart);
        PaintableFrame pf = new PaintableFrame(g.getID(), adapter);
    }
    
    public Vector<RegressionPoint> createRegressionPoints(Gene g, boolean fg) { 
        Vector<RegressionPoint> pts = new Vector<RegressionPoint>();
        char strand = g.getStrand();

        int count = 0;
        try {
            data.window(g.getChrom(), g.getStart(), g.getEnd());
            
            if(data.getCount() < segmentNumProbes) {
                return null;
            }
            
            double width = (double)g.getWidth();
            
            for(int i = 0; i < data.getCount(); i++) { 
                int pos = data.getPos(i);

                for(int j = 0; j < data.getReplicates(i); j++) {
                    
                    if(data.getStrand(i, j) == strand) {
                        
                        String name = String.format("%s:%d:%d", g.getChrom(), pos, j);
                        int offsetDiff = strand == '+' ? pos-g.getStart() : g.getEnd()-pos;
                        double xvalue = (double)offsetDiff / width;
                        double yvalue = ipData == fg ? data.getIP(i, j) : data.getWCE(i, j);
						yvalue = Math.log(yvalue);
                        
                        if(!Double.isNaN(yvalue) && !Double.isInfinite(yvalue)) {
                            RegressionPoint pt = new RegressionPoint(name, xvalue, yvalue);;
                            pts.add(pt);
                            
                            count += 1;
                        }
                    }
                }
            }
            
            if(count < segmentNumProbes) { 
                return null;
            }
            
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
       
        return pts;
    }
	
    public Regression createRegression(Gene g, boolean fg) {
        char strand = g.getStrand();
        
    	logger.log(Level.FINEST, String.format("Differential Regression: (%s) %s", 
    			g.getID(), g.getLocationString()));
        
        Vector<RegressionPoint> pts = createRegressionPoints(g, fg);
        if(pts == null) { return null; }
    	
    	Datapoints dps = new Datapoints();
    	PredictorSet preds = new PredictorSet();
    	MappedValuation<Double> xexpr = new MappedValuation<Double>("x");
    	MappedValuation<Double> yexpr = new MappedValuation<Double>("strain-expr");
        
        for(RegressionPoint pt : pts) { 
            dps.addDatapoint(pt.name);
            xexpr.addValue(pt.name, pt.x);
            yexpr.addValue(pt.name, pt.y);
        }
    	
		preds.addConstantPredictor();
		preds.addQuantitativePredictor(xexpr);
    	
		return new Regression(yexpr, preds, dps);
    }
    
    private static class ValuedGene implements Comparable<ValuedGene> {
    	
    	public Gene gene;
    	public double value;
    	
    	public ValuedGene(Gene g, double v) { 
    		gene = g;
    		value = v;
    	}
    	
    	public int compareTo(ValuedGene g) { 
    		if(value > g.value) { return -1; }
    		if(value < g.value) { return 1; }
    		return gene.compareTo(g.gene);
    	}
    	
    	public String toString() { return String.format("%.3f  \t%s", value, gene.getID()); }
    	
    	public int hashCode() { return gene.hashCode(); }
    	
    	public boolean equals(Object o) { 
    		if(!(o instanceof ValuedGene)) { return false; }
    		ValuedGene g = (ValuedGene)o;
    		return gene.equals(g.gene) && value == g.value;
    	}
    }

	public void permute() {
		// Yuchun, this is a new method in DifferentialGeneAnalysis, so 
		// I added it for you.  - Tim
	}

	public Vector<DiffGeneModel> getModels() {
		// Yuchun -- this is *another* new method in DifferentialGeneAnalysis, 
		// so I've added it for you as well.  -Tim
		return null;
	}

}

 class RegressionPoint {
    
    public String name;
    public double x, y;
    
    public RegressionPoint(String n, double x, double y) { 
        name = n;
        this.x = x;
        this.y = y;
    }
    
    public int hashCode() { return name.hashCode(); }
    
    public boolean equals(Object o) { 
        if(!(o instanceof RegressionPoint)) { return false; }
        RegressionPoint rp = (RegressionPoint)o;
        return name.equals(rp.name);
    }
}
