package edu.mit.csail.cgs.sigma.yuchun;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.*;

import edu.mit.csail.cgs.datasets.alignments.AlignBlock;
import edu.mit.csail.cgs.datasets.alignments.Alignment;
import edu.mit.csail.cgs.datasets.alignments.AlignmentLoader;
import edu.mit.csail.cgs.datasets.alignments.AlignmentVersion;
import edu.mit.csail.cgs.datasets.alignments.GappedAlignmentString;
import edu.mit.csail.cgs.datasets.alignments.MultipleAlignment;
import edu.mit.csail.cgs.datasets.general.NamedRegion;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.motifs.WeightMatrix;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.CastingMapper;
import edu.mit.csail.cgs.ewok.verbs.ChromRegionIterator;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.ExpanderIterator;
import edu.mit.csail.cgs.ewok.verbs.MapperIterator;
import edu.mit.csail.cgs.ewok.verbs.RefGeneGenerator;
import edu.mit.csail.cgs.ewok.verbs.RefGenePromoterGenerator;
import edu.mit.csail.cgs.ewok.verbs.SequenceGenerator;
import edu.mit.csail.cgs.ewok.verbs.motifs.WeightMatrixExpander;
import edu.mit.csail.cgs.ewok.verbs.motifs.WeightMatrixHit;
import edu.mit.csail.cgs.sigma.GeneGenerator;
import edu.mit.csail.cgs.sigma.Parser;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.alignments.AlignmentProperties;
import edu.mit.csail.cgs.sigma.expression.SudeepExpressionProperties;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.sigma.genes.GeneNameAssociation;
import edu.mit.csail.cgs.sigma.genes.GeneOrthologyEntry;
import edu.mit.csail.cgs.sigma.motifs.MotifProperties;
import edu.mit.csail.cgs.sigma.networks.RegulatoryUnit;
import edu.mit.csail.cgs.sigma.networks.RegulatoryNetwork;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.SetTools;

public class RegulatoryNetworkAnalysis {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RegulatoryNetworkAnalysis analysis = new RegulatoryNetworkAnalysis();
		//analysis.printSingleCell();
		//analysis.printAllCells();
		analysis.printSingleCell2();
		//analysis.getMutatedTFs();
		
//		analysis.loadMacIsaacSites(args[0]);
//		analysis.printAlignmentSubstrings();
//		analysis.scoreMacIsaacSites("sigma");
//		analysis.scoreMacIsaacSites("rm11");
//		analysis.scoreMacIsaacSites("yjm789");
		
		//analysis.printBindingMatrix("s288c");
	}

	private SigmaProperties sp;
	private MotifProperties mp;
	private GeneAnnotationProperties gp;
	private SudeepExpressionProperties ep;
	private Logger logger;
	private RegulatoryNetwork s288cNetwork;
	private RegulatoryNetwork sigmaNetwork;
	private Map<String, GeneOrthologyEntry> orthGenes;
    private Map<String,Gene> genes;
    private Vector<MacIsaacSite> bindingSites;
    private Map<String, Set<Integer>> targetBindingSites; // gene name --> binding site id
    private DifferentialGeneMixed diffexpr_deploid;
    private DifferentialGeneMixed diffexpr_mat_a;
    private DifferentialGeneMixed diffexpr_mat_alpha;
    //TODO: file output
    private String exptKey="s288c_diploid";
	
	public RegulatoryNetworkAnalysis(){
		sp = new SigmaProperties();
		mp = new MotifProperties(sp, "default");
		gp = new GeneAnnotationProperties(sp, "default");
        ep = new SudeepExpressionProperties(sp, "sudeep");
        logger = ep.getLogger("RegulatoryNetworkAnalysis");
	}
	private void loadRegulatoryNetworks(){
        logger.log(Level.INFO, "Building s288c Network ...");
		s288cNetwork = new RegulatoryNetwork(sp, mp, gp, "s288c");
		s288cNetwork.loadData();
		
        logger.log(Level.INFO, "Building sigma Network ...");
		sigmaNetwork = new RegulatoryNetwork(sp, mp, gp, "sigma");
		sigmaNetwork.loadData();
		
        logger.log(Level.INFO, "Making network comparison(s) ...");
		s288cNetwork.compareToNetwork(sigmaNetwork);
		//sigmaNetwork.compareToNetwork(s288cNetwork);
	}
	
	private void loadOrthGenes(){
        logger.log(Level.INFO, "Load Orthologous Genes ...");
		File f = gp.getOrthologousGenesFile();
		orthGenes = new HashMap<String, GeneOrthologyEntry>();
		try {
			Parser<GeneOrthologyEntry> goes = new Parser<GeneOrthologyEntry>(f, 
					new GeneOrthologyEntry.ParsingMapper());
			
			while(goes.hasNext()) { 
				GeneOrthologyEntry goe = goes.next();
				orthGenes.put(goe.getGeneName(), goe);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadGenes() { 
		genes.clear();
		String strain = "s288c";
		GeneGenerator gener = sp.getGeneGenerator(strain);
		ChromRegionIterator chromer = new ChromRegionIterator(sp.getGenome(strain));
		Iterator<Region> chroms = 
			new MapperIterator<NamedRegion,Region>(new CastingMapper<NamedRegion,Region>(), chromer);
		Iterator<Gene> gitr = new ExpanderIterator<Region,Gene>(gener, chroms);
		while(gitr.hasNext()) { 
			Gene g = gitr.next();
			genes.put(g.getID(), g);
		}
	}
	
	private DifferentialGeneMixed loadDiffExpression(String exptKey){
        logger.log(Level.INFO, "Load Diff Expression "+exptKey);
        DifferentialGeneMixed diffexpr = new DifferentialGeneMixed(ep, exptKey);
		diffexpr.loadData();
		return diffexpr;
	}
	
	private void getMutatedTFs(){
		StringBuffer sb = new StringBuffer();
		for (String tf : s288cNetwork.getTFNames()){
			for (String id: s288cNetwork.getGeneNameAssociation().getIDs(tf)){
				if (orthGenes.containsKey(id)){
					GeneOrthologyEntry goe = orthGenes.get(id);
					if (!goe.hasFlag(GeneOrthologyEntry.Flag.PERFECT)){
						sb.append(tf+"\t"+id+"\t");
						sb.append(goe.getPctIdentity()+"%\t");
						for (GeneOrthologyEntry.Flag flag:goe.getFlags()){
							sb.append(flag+" ");
						}
						sb.append("\n");
					}
				}
			}
		}
		System.out.print(sb.toString());
	}
	// To combine information from 1. diff expr, 2. TF motif scan diff
	// into one big table.	
	// positive number in diff_expr: higher expression in s288c
	// up arrow "^" in TF expression: higher expression of TF in s288c
	// motif "+": motif gain in sigma
	private void printSingleCell() {
        double exprDiffCutoff = 0.5;
        
		loadRegulatoryNetworks();
		loadOrthGenes();
		diffexpr_deploid=loadDiffExpression("s288c_diploid");
		
        DifferentialGeneMixed diffexpr=diffexpr_deploid;
        //TODO: file
		File rankedFile = ep.getRankedDifferentialFile(exptKey); 
		SortedSet<String> set_missingGenes = new TreeSet<String>();
		SortedSet<String> set_diffMotifCount = new TreeSet<String>();
		SortedSet<String> set_diffMotifPos = new TreeSet<String>();
		SortedSet<String> set_TfDiffExpr = new TreeSet<String>();
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(rankedFile));
			
	        Vector<Pair<Gene,Double>> sorted = diffexpr.getRankedGenes();
	        Vector<Pair<Gene,Double>> diffGenes = diffexpr.getChangedGenes(-exprDiffCutoff, exprDiffCutoff);
	   	        
	        StringBuffer sb = new StringBuffer();
	        sb.append("Name\tORF\tDiff expression\ts288c coord\tsigma coord\tgenome\tTF expr");
	        for(Pair<Gene,Double> p : diffGenes) { 
	        	Gene gene = p.getFirst();
	        	String geneId = gene.getID();
	        	sb.append("\n")
	        		.append(s288cNetwork.getGeneNameAssociation().getName(geneId)).append("\t")
	        		.append(geneId).append("\t")
	        		.append(diffexpr.getGeneDiffExpression(geneId)+"\t")
	        		.append(gene.getLocationString()+"\t");
	        	if (orthGenes.containsKey(geneId)){
	        		sb.append(orthGenes.get(geneId).getSigmaCoords());
	        	}
	        	sb.append("\t");
				RegulatoryUnit ru = s288cNetwork.getRegulatoryUnit(geneId);
				RegulatoryUnit ru2 = sigmaNetwork.getRegulatoryUnit(geneId);
				if (ru==null){
					logger.log(Level.INFO, "gene not in 288c: "+ geneId);
					continue;
				}	
				
				// gene compare
				if (orthGenes.containsKey(geneId)){
					if (orthGenes.get(geneId).hasFlag(GeneOrthologyEntry.Flag.MISSING)){
						sb.append("MISSING");
						set_missingGenes.add(geneId);
					}else{
						// motif changed
						if (!ru.getChanges().isEmpty()){
							for (RegulatoryUnit.RegulatoryChange change : ru.getChanges()){
								sb.append(change.toString()).append(" ");
							}
							set_diffMotifCount.add(geneId);
						}
						sb.append("\t");
						// detail compare of motif positions
/*						String details = compareMotifPositions(ru, ru2);
						if (!details.equals("")){
							set_diffMotifPos.add(geneId);
							sb.append(details);
						}*/
						
						// TF changes
						boolean tf_diffExpr = false;
						for (String tf : ru.getRegulatingTFs()){
							for (String id: s288cNetwork.getGeneNameAssociation().getIDs(tf)){
								// expression of TF changed
								if (diffexpr.getGeneDiffExpression(id)>exprDiffCutoff){
									sb.append(tf+"^ ");
									tf_diffExpr =true;
								}
								else if (diffexpr.getGeneDiffExpression(id)<-exprDiffCutoff){
									sb.append(tf+"v ");
									tf_diffExpr =true;
								}

			/*					// TF mutated
								if (orthGenes.containsKey(id)){
									GeneOrthologyEntry goe = orthGenes.get(id);
									if (!goe.hasFlag(GeneOrthologyEntry.Flag.PERFECT)){
										sb.append(tf+" ");
										for (GeneOrthologyEntry.Flag flag:goe.getFlags()){
											sb.append(flag+" ");
										}
										sb.append(goe.getPctIdentity()+"% ");
										sb.append("\n");
									}
								}*/
							}
						}
						sb.append("\t");
						if (tf_diffExpr){
							set_TfDiffExpr.add(geneId);
						}
						// TODO: Core promoter TATA box change						
					}
					
				}else{
					//System.out.println("Not in orthGenes: " + geneId);
				}
		    	ps.print(sb.toString());
		    	sb=new StringBuffer();
			}
	        
	        // overall statistics
	        StringBuffer report = new StringBuffer("================================\n");
	        report.append("S288c genes:\t"+sorted.size()+"\n");
	        report.append("Diff expressed genes:\t"+diffGenes.size()+"\n");
	        report.append("Missing in Sigma:\t"+set_missingGenes.size()+"\n");
	        report.append("TF motif count changed:\t"+set_diffMotifCount.size()+"\n");
	        report.append("TF motif position changed:\t"+set_diffMotifPos.size()+"\n");
	        report.append("TF expression changed:\t"+set_TfDiffExpr.size()+"\n");
	        
			SetTools<String> set_tools = new SetTools<String>();
			Set<String> set_tfExpr_only = set_tools.subtract(set_TfDiffExpr, set_diffMotifPos);
			report.append("TF expr changed (no motif change) :\t"+set_tfExpr_only.size()+"\n");
			
			// up genes
	        report = new StringBuffer("================================\n");
			report.append("Differetial expression genes (mid line cutoff="+exprDiffCutoff+")\n");
	        for(Pair<Gene,Double> p : diffGenes) { 
	        	Gene gene = p.getFirst();
	        	String geneId = gene.getID();
	        	report.append(s288cNetwork.getGeneNameAssociation().getName(geneId)).append("\t");
	        }
	        ps.print(report);
	        ps.close();
    	} catch(IOException ie) { 
    		ie.printStackTrace(System.err);
    	}
	}

	// To combine information from 1. diff expr, 2. MacIsaac site scan diff
	// into one big table.
	// positive number in diff_expr: higher expression in s288c
	// up arrow "^" in TF expression: higher expression of TF in s288c
	// motif "+": motif gain in s288c
	private void printSingleCell2() {
        double exprDiffCutoff = 0.693147;	//ln 2
        double confidenceCutoff = 0.1;
        
        logger.log(Level.INFO, "Building s288c macIsaacSite Network ...");
		s288cNetwork = new RegulatoryNetwork(sp, mp, gp, "s288c"); //macIsaacSite network
		s288cNetwork.loadData();
        GeneNameAssociation gna = gp.getGeneNameAssociation("s288c");
		loadOrthGenes();
		
        loadMacIsaacSites("macIsaac_sites_SGDv1.coord");
        loadBindingTargets(sp.getS288cGenome());
		loadMacIsaacSitesDiffScore(new File("macIsaac_sites_motif_score_FSA_sigma_5.txt"));
		SortedMap<String, DiffExpression> diffExprs= loadDiffExpressions(exptKey, exprDiffCutoff);
		
		SortedSet<String> set_missingGenes = new TreeSet<String>();
		SortedSet<String> set_diffMotifCount = new TreeSet<String>();
		SortedSet<String> set_diffMotifPos = new TreeSet<String>();
		SortedSet<String> set_TfDiffExpr = new TreeSet<String>();
		
		try {
			File output = new File("replicate_s288c_diploid_expr_BS.txt"); 
			PrintStream ps = new PrintStream(new FileOutputStream(output));
			   	        
	        StringBuilder sb = new StringBuilder();
	        sb.append("Name\tORF\tDiff expression\tVariance\tSignificance\tBS change\tTF expr\ts288c coord\tMacIsaac sites");
	        for(String g: diffExprs.keySet()) { 
	        	sb.append("\n")
	        		.append(gna.getName(g)).append("\t")
	        		.append(g).append("\t")
	        		.append(diffExprs.get(g).diff+"\t")
	        		.append(diffExprs.get(g).variance+"\t")
	        		.append(diffExprs.get(g).significance+"\t");
	        	
				// binding site changed
				if (targetBindingSites.containsKey(g)){
					for (int id: targetBindingSites.get(g)){
						MacIsaacSite site = bindingSites.elementAt(id-1);
						if (site.diffScore!=0){
							sb.append(site.tf).append(":"+site.diffScore);
							sb.append(" ");
							set_diffMotifCount.add(g);
						}
					}
				}
				sb.append("\t");	
				
				// gene compare
				if (orthGenes.containsKey(g)){
					if (orthGenes.get(g).hasFlag(GeneOrthologyEntry.Flag.MISSING)){
						sb.append("MISSING");
						set_missingGenes.add(g);
					}else{
						// TF changes
						boolean tf_diffExpr = false;
						Set<String> tfs_showed=new HashSet<String>();
//						if (targetBindingSites.containsKey(g)){
//							for (int id: targetBindingSites.get(g)){
//								MacIsaacSite site = bindingSites.elementAt(id-1);
//								if (!tfs_showed.contains(site.tf_sysName)){
//									tfs_showed.add(site.tf_sysName);
//									if (diffExprs.containsKey(site.tf_sysName)){
//										if (diffExprs.get(site.tf_sysName).sign!=null){
//											sb.append(site.tf).append(diffExprs.get(site.tf_sysName).sign+" ");
//											tf_diffExpr =true;
//										}
//									}else{
//										System.err.println("Not in diff expr:"+site.tf_sysName);
//									}
//								}
//							}
//						}
						RegulatoryUnit ru = s288cNetwork.getRegulatoryUnit(g);
						for (String tf : ru.getRegulatingTFs()){
							for (String id: s288cNetwork.getGeneNameAssociation().getIDs(tf)){
								// expression of TF changed
								if (diffExprs.containsKey(id)){
									if (diffExprs.get(id).sign!=null){
										sb.append(tf).append(diffExprs.get(id).sign+" ");
										tf_diffExpr =true;
									}
								}
								else{
									System.err.println("Not in diff expr:"+id);
								}
							}
						}
						if (tf_diffExpr){
							set_TfDiffExpr.add(g);
						}
						// TODO: Core promoter TATA box change						
					}
					sb.append("\t");
					
		        	sb.append(orthGenes.get(g).getS288CCoords()).append("\t");
				}else{
					System.err.println("Not in orthGenes: " + g);
				}
				// all motif
				sb.append(s288cNetwork.getRegulatoryUnit(g).getMotifOffsetsString());
				
		    	ps.print(sb.toString());
		    	sb=new StringBuilder();
			}
	        
	        // overall statistics
	        StringBuffer report = new StringBuffer("\n================================\n");
	        report.append("S288c genes:\t"+diffExprs.keySet().size()+"\n");
	        report.append("Missing in Sigma:\t"+set_missingGenes.size()+"\n");
	        report.append("TF motif count changed:\t"+set_diffMotifCount.size()+"\n");
	        report.append("TF motif position changed:\t"+set_diffMotifPos.size()+"\n");
	        report.append("TF expression changed:\t"+set_TfDiffExpr.size()+"\n");
	        
			SetTools<String> set_tools = new SetTools<String>();
			Set<String> set_tfExpr_only = set_tools.subtract(set_TfDiffExpr, set_diffMotifPos);
			report.append("TF expr changed (no motif change) :\t"+set_tfExpr_only.size()+"\n");
			
	        ps.print(report);
	        ps.close();
		} catch(IOException ie) { 
    		ie.printStackTrace(System.err);
    	}
	}
	
	private SortedMap<String, DiffExpression> loadDiffExpressions(String expKeyStr, double exprDiffCutoff){
		String line="";
		File rankedFile = ep.getRankedDifferentialFile(exptKey); 
		SortedMap<String, DiffExpression> diffExprs= new TreeMap<String, DiffExpression>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(rankedFile));
			br.readLine();//header
			while((line = br.readLine()) != null) { 
                line = line.trim();
                DiffExpression diffExpr = new DiffExpression();
                String[] f=line.split("\t");
                diffExpr.gene=f[0];
                diffExpr.diff=Double.parseDouble(f[1]);
                diffExpr.variance=Double.parseDouble(f[2]);
                diffExpr.significance=Double.parseDouble(f[3]);
                if (diffExpr.diff>=exprDiffCutoff){
                	diffExpr.sign="^";
                }
                else if (diffExpr.diff<=-exprDiffCutoff){
                	diffExpr.sign="v";
                }
                diffExprs.put(diffExpr.gene, diffExpr);
			}
		}catch(IOException ie) { 
    		ie.printStackTrace(System.err);
    	}
		return diffExprs;
	}
	
	// positive number in diff_expr: higher expression in s288c
	// up arrow "^" in TF expression: higher expression of TF in s288c
	// motif "+": motif gain in sigma
	private void printAllCells() {
        double exprDiffCutoff = 0.5;
        //TODO: file
		File rankedFile = ep.getRankedDifferentialFile(exptKey); 
		
		loadRegulatoryNetworks();
		loadOrthGenes();
		diffexpr_deploid=loadDiffExpression("s288c_diploid");
		diffexpr_mat_a=loadDiffExpression("s288c_mat_a");
		diffexpr_mat_alpha=loadDiffExpression("s288c_mat_alpha");
		
		SetTools<String> set_tools = new SetTools<String>();

		try {
			PrintStream ps = new PrintStream(new FileOutputStream(rankedFile));
			
	        Vector<Pair<Gene,Double>> sorted = diffexpr_deploid.getRankedGenes();
	   	        
	        StringBuffer sb = new StringBuffer();
	        sb.append("Name\tORF\tDiff deploid\tDiff mat_a\tDiff_mat_alpha\tgenome\tTF deploid\tTF mat_a\tTF_mat_alpha\ts288c coord\tsigma coord");
	        for(Pair<Gene,Double> p : sorted) { 
	        	Gene gene = p.getFirst();
	        	String geneId = gene.getID();
	        	sb.append("\n")
	        		.append(s288cNetwork.getGeneNameAssociation().getName(geneId)).append("\t")
	        		.append(geneId).append("\t")
	        		.append(diffexpr_deploid.getGeneDiffExpression(geneId)+"\t")
	        		.append(diffexpr_mat_a.getGeneDiffExpression(geneId)+"\t")
	        		.append(diffexpr_mat_alpha.getGeneDiffExpression(geneId)+"\t");

				RegulatoryUnit ru = s288cNetwork.getRegulatoryUnit(geneId);
				//RegulatoryUnit ru2 = sigmaNetwork.getRegulatoryUnit(geneId);
				if (ru==null){
					logger.log(Level.INFO, "gene not in 288c: "+ geneId);
					continue;
				}	
				String tf_d="", tf_mat_a="", tf_mat_alpha="";
				if (orthGenes.containsKey(geneId)){
					// gene compare
					if (orthGenes.get(geneId).hasFlag(GeneOrthologyEntry.Flag.MISSING)){
						sb.append("MISSING");
						sb.append("\t");
					}else{
						// motif changed
						Map<String, Set<Integer>> motifOffsets=ru.getMotifOffsets();
				        for(String m : motifOffsets.keySet()) { 
				            for(int off : motifOffsets.get(m)) { 
				                sb.append(String.format(" %s:%d", m, off));
				            }
				        }
				        //sb.append("|");
						if (!ru.getChanges().isEmpty()){
							for (RegulatoryUnit.RegulatoryChange change : ru.getChanges()){
								sb.append(" ").append(change.toString());
							}
						}
						sb.append("\t");
						// detail compare of motif positions
/*						String details = compareMotifPositions(ru, ru2);
						if (!details.equals("")){
							set_diffMotifPos.add(geneId);
							sb.append(details);
						}*/
						
						// TF changes
						for (String tf : ru.getRegulatingTFs()){
							for (String id: s288cNetwork.getGeneNameAssociation().getIDs(tf)){
								// expression of TF changed
								if (diffexpr_deploid.getGeneDiffExpression(id)>exprDiffCutoff){
									tf_d = tf_d+ tf+"^ ";
								}
								else if (diffexpr_deploid.getGeneDiffExpression(id)<-exprDiffCutoff){
									tf_d = tf_d+ tf+"v ";
								}
								if (diffexpr_mat_a.getGeneDiffExpression(id)>exprDiffCutoff){
									tf_mat_a = tf_mat_a+ tf+"^ ";
								}
								else if (diffexpr_mat_a.getGeneDiffExpression(id)<-exprDiffCutoff){
									tf_mat_a = tf_mat_a+ tf+"v ";
								}
								if (diffexpr_mat_alpha.getGeneDiffExpression(id)>exprDiffCutoff){
									tf_mat_alpha = tf_mat_alpha+ tf+"^ ";
								}
								else if (diffexpr_mat_alpha.getGeneDiffExpression(id)<-exprDiffCutoff){
									tf_mat_alpha = tf_mat_alpha+ tf+"v ";
								}
			/*					// TF mutated
								if (orthGenes.containsKey(id)){
									GeneOrthologyEntry goe = orthGenes.get(id);
									if (!goe.hasFlag(GeneOrthologyEntry.Flag.PERFECT)){
										sb.append(tf+" ");
										for (GeneOrthologyEntry.Flag flag:goe.getFlags()){
											sb.append(flag+" ");
										}
										sb.append(goe.getPctIdentity()+"% ");
										sb.append("\n");
									}
								}*/
							}
						}
						// TODO: Core promoter TATA box change						
					}
					
				}else{
					//System.out.println("Not in orthGenes: " + geneId);
					sb.append("\t");
				}
				sb.append(tf_d).append("\t")
				.append(tf_mat_a).append("\t")
				.append(tf_mat_alpha).append("\t");
				
				// gene location strings
	        	sb.append(gene.getLocationString()+"\t");
	        	if (orthGenes.containsKey(geneId)){
	        		sb.append(orthGenes.get(geneId).getSigmaCoords());
	        	}
	        	sb.append("\t");
	        	
		    	ps.print(sb.toString());
		    	sb=new StringBuffer();
			}
	        
	        ps.close();
    	} catch(IOException ie) { 
    		ie.printStackTrace(System.err);
    	}
	}
	// report detailed motif info, such as motif position changes
	private String compareMotifPositions(RegulatoryUnit first, RegulatoryUnit other) { 
		Map<Integer,Set<String>> offsets = buildOffsets(first.getMotifOffsets());
		if (other==null){
			//return "Regulatory unit not found in the 2nd network.\n";
			return "";
		}
		boolean changed=false;
		Map<Integer,Set<String>> offsets_other = buildOffsets(other.getMotifOffsets());
		StringBuffer sb_offset = new StringBuffer("Offset\t");
		StringBuffer sb_this = new StringBuffer("s288c\t");
		StringBuffer sb_other = new StringBuffer("sigma\t");
		SetTools<Integer> tools = new SetTools<Integer>();
		Set<Integer> allOffsets = tools.union(offsets.keySet(), offsets_other.keySet());
		SortedSet<Integer>sortedOffsets = new TreeSet<Integer>();
		for (int off: allOffsets){
			sortedOffsets.add(off);
		}
		for(int i : sortedOffsets) { 
			sb_offset.append(i).append("\t");
			if (offsets.containsKey(i)){
				for (String tf:offsets.get(i)){
					sb_this.append(tf+" ");
				}
			}else{
				sb_this.append("XXX");
				changed = true;
			}
			sb_this.append("\t");
			if (offsets_other.containsKey(i)){
				for (String tf:offsets_other.get(i)){
					sb_other.append(tf+" ");
				}
			}else{
				sb_other.append("XXX");
				changed = true;
			}
			sb_other.append("\t");
		}
		if (changed){
			return sb_offset.append("\n").append(sb_this).append("\n")
				.append(sb_other).append("\n").toString();
		}else{
			return "";
		}
	}
	
	private Map<Integer,Set<String>> buildOffsets(Map<String,Set<Integer>> motifs){
		Map<Integer,Set<String>> offsets = new TreeMap<Integer,Set<String>>();
        for(String motif : motifs.keySet()) { 
            for(int offset : motifs.get(motif)) { 
                if(!offsets.containsKey(offset)) { offsets.put(offset, new TreeSet<String>()); }
                offsets.get(offset).add(motif); 
            }
        }
        return offsets;
	}
	
    // print potential binding matrix of TF calculating from motif
    // row-gene, column-TF, entry-1 for binding
    private void printBindingMatrix(String strain){
    	RegulatoryNetwork network = null;
    	loadRegulatoryNetworks();
    	if (strain.equals("s288c")){
    		network = s288cNetwork;
    	}
    	
    	String[] geneIds = new String[6429];
		File file = new File("gene_ids.txt");
		String line="";
		int gene_count = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
            while((line = br.readLine()) != null) { 
                line = line.trim();
                geneIds[gene_count]=line;
                gene_count++;
            }
            br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
    	HashMap<String, Integer> tfIndices = new HashMap<String, Integer>();
    	Set<String> tfs = network.getTFNames();
    	StringBuilder sb_header = new StringBuilder();
    	sb_header.append("Gene Id\t");
    	int tf_index = 0;
    	for (String tf : tfs){
    		tfIndices.put(tf, tf_index);
    		sb_header.append(tf+"\t");
    		tf_index++;
    	}
    	
    	boolean[][] matrix = new boolean[geneIds.length][tfs.size()]; 
		for (int i=0;i<geneIds.length;i++)
		{
            line = geneIds[i];
            RegulatoryUnit ru = network.getRegulatoryUnit(line);
            if (ru==null){
            	continue;
            }
            Collection<String> ru_tfs = ru.getRegulatingTFs();
            for(String tf:ru_tfs){
            	matrix[i][tfIndices.get(tf)]=true;
            }
		}
		
		File output = new File("s288c_MacIsaacBindingMatrix.txt");
		StringBuilder sb = new StringBuilder();
		try{
			PrintStream ps = new PrintStream(new FileOutputStream(output));
			ps.print(sb_header.toString()+"\n");
            for (int i=0;i<geneIds.length;i++){
            	sb.append(geneIds[i]+"\t");
            	for (int j=0;j<tfs.size();j++){
            		sb.append((matrix[i][j]?1:0)+"\t");
            	}
            	ps.print(sb.toString()+"\n");
            	sb = new StringBuilder();
            }
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
	private void loadMacIsaacSites(String file){
		String line;
        //SequenceGenerator seqgen = new SequenceGenerator();
        Genome g = sp.getS288cGenome();
	    bindingSites = new Vector<MacIsaacSite>();	
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(file)));
			while((line = br.readLine()) != null) { 
                line = line.trim();
                if (line.charAt(0)=='#'){ //comment line
                	continue;
                }

                MacIsaacSite site = new MacIsaacSite();
                String[] f=line.split("\t");
                site.id=Integer.valueOf(f[0]);
                site.tf= f[1];
                site.tf_sysName = f[2];
                site.sequence = f[3];
                //TODO: end position - 1  because Kenzie's original end position is exclusive;
                // update: this is true only for some sites, not for all. To ensure full site, we do not make the end position adjustment.
//                site.sequence = site.sequence.substring(0, site.sequence.length()-1);
//                site.region = new Region(g, f[4], new Integer(f[5]).intValue(), new Integer(f[6]).intValue()-1);
                site.region = new Region(g, f[4], new Integer(f[5]).intValue(), new Integer(f[6]).intValue());
        		site.strand = f[7];
                // check sequence
/*                String seq = seqgen.execute(r);
                if (f[7].equals("-")){
                	seq = reverseComplement(seq);
                }
                if (!seq.equalsIgnoreCase(f[3])){
                	System.err.printf("%s\t%s\tq=%s\t%s\n",f[0],seq,f[3],r.toString());
                }*/
                bindingSites.add(site);
           }
            br.close();

		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	private void loadBindingTargets(Genome g){
		//TODO: upstream 500bps
        int upstream=500, downstream=0;
        targetBindingSites = new HashMap<String, Set<Integer>> ();
	    RefGenePromoterGenerator gen = new RefGenePromoterGenerator(g, "sgdGene", upstream, downstream);
	    //StringBuilder sb = new StringBuilder();
		for (MacIsaacSite site: bindingSites){
            Iterator<Gene> genes = gen.execute(site.region);
            while (genes.hasNext()) {
                Gene gene = genes.next();
                Region promoter = gene.expand(upstream,downstream);
                if (site.region.overlaps(promoter)) {
                	site.targetGenes.add(gene);
                	
                	if (targetBindingSites.containsKey(gene.getName())){
                		targetBindingSites.get(gene.getName()).add(site.id);
                	}
                	else{
                		Set<Integer> ids = new HashSet<Integer>();
                		ids.add(site.id);
                		targetBindingSites.put(gene.getName(), ids);
                	}
                	// generate regulatoryUnits ...	
//            		int middle = (site.region.getStart()+ site.region.getEnd())/2;
//            		int offset = gene.getStrand() == '+' ? gene.getStart()-middle : middle-gene.getEnd();
//                	sb.append(f[0]).append("\t").append(f[1]).append("\t")
//                	  .append(gene.getID()).append("\t").append(offset).append("\n");
                }
            }			
		}
        
//      PrintStream ps = new PrintStream(new FileOutputStream(new File("MacIsaac_regNetwork.txt")));
//      ps.println(sb.toString());
//      ps.close();
	}
	
	private void loadMacIsaacSitesDiffScore(File file){
		String line;
		try {
		
			// load the different motif scores to the site	
			BufferedReader br=new BufferedReader(new FileReader(file));
			br.readLine();
			while((line = br.readLine()) != null) { 
	            line = line.trim();
	            String[] f=line.split("\t");
	            int id=Integer.parseInt(f[0]);
	            bindingSites.elementAt(id).diffScore=Double.parseDouble(f[11]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	}
	private void scoreMacIsaacSites(String otherStrain){
		loadRegulatoryNetworks();	// to get WeightMatrix
		AlignmentProperties props = new AlignmentProperties();
        Genome g = props.getGenome("s288c");
        Genome g_strain2 = props.getGenome(otherStrain);
        int flanking=5; // to extend the binding site sequence
        String header = "Id\tTF\tS288c coord\tS288c seq\tS288c score\tS288c pos\tS288c strand\tS2 seq\tS2 score\tS2 pos\tS2 strand\tDiff score";
		try {
	        PrintStream ps = new PrintStream(new FileOutputStream(new File("test_macIsaac_sites_motif_score_FSA_"+otherStrain+"_5.txt")));
	        ps.println(header);
            AlignmentLoader loader = new AlignmentLoader();
    		String versionName = props.getAlignmentVersionName();
    		AlignmentVersion version = loader.loadAlignmentVersion(versionName);
    		for (MacIsaacSite site: bindingSites){
                //use sequence alignment to get corresponding sigma site
    			Region r = site.region;
    			
//    			WeightMatrix matrix = s288cNetwork.getWeightMatrix(site.tf);
//    			if (matrix==null){
//            		System.err.println(site.tf+" weightmatrix not found!");
//            		continue;
//            	}
//    			System.err.println(site.sequence.length()+" "+matrix.length());
    			
        		Collection<AlignBlock> blocks = version.getAlignBlocks(r);
        		Set<Alignment> alignments = new LinkedHashSet<Alignment>();
        		for(AlignBlock block : blocks) { 
        			Alignment a = block.getAlignment();
        			alignments.add(a);
        		}
        		LinkedList<MultipleAlignment> aligns = new LinkedList<MultipleAlignment>();
        		for(Alignment a : alignments) { 
        			aligns.add(new MultipleAlignment(a));
        		}
        		for (MultipleAlignment ma: aligns){
        			String[] spec = ma.species();
        			String name="", name_strain2="";
        			for (int i=0; i<spec.length;i++){
        				if (spec[i].indexOf(g.getVersion())!=-1){
        					name = spec[i];
        				}
        				else if (spec[i].indexOf(g_strain2.getVersion())!=-1){
        					name_strain2 = spec[i];
        				}
        			}
        			
        			GappedAlignmentString sgd_algn = ma.getGappedAlignment(name);
                    //minus one: 1-based coord in blat --> 0-based coord in cgs code
        			int start=sgd_algn.ungappedToGapped(r.getStart()-1);
        			int end=sgd_algn.ungappedToGapped(r.getEnd()-1);
        			String s288c_site = ma.getGappedAlignment(name).substring(start-flanking, end+1+flanking).ungappedString();
        			String strain2_site = ma.getGappedAlignment(name_strain2).substring(start-flanking, end+1+flanking).ungappedString();
//        			System.err.print(s288c_site+" ");
        			if (site.strand.equals("-")){
        				strain2_site = reverseComplement(strain2_site);
        				s288c_site = reverseComplement(s288c_site);
                    }
//        			System.err.println(s288c_site);
                    if (!s288c_site.equalsIgnoreCase(strain2_site)){
                    	WeightMatrix matrix = s288cNetwork.getWeightMatrix(site.tf);
                    	if (matrix==null){
                    		System.err.println(site.tf+" weightmatrix not found!");
                    		continue;
                    	}
                    	BestScore score_s288c = scoreBestSite(matrix, s288c_site);
                    	BestScore score_strain2 = scoreBestSite(matrix, strain2_site);
                    	if (score_s288c.sequence.equals(score_strain2.sequence)){
                    		continue;
                    	}
                    	float score_diff = score_s288c.score-score_strain2.score;

                    	StringBuilder sb = new StringBuilder();
                    	sb.append(site.id).append("\t");
                    	sb.append(site.tf).append("\t");
                    	sb.append(r.getLocationString()).append("\t");
                    	sb.append(score_s288c.sequence).append("\t");
                    	sb.append(score_s288c.score).append("\t");
                    	sb.append(score_s288c.position).append("\t");
                    	sb.append(score_s288c.reverseCompliment?"-":"+").append("\t");
                    	sb.append(score_strain2.sequence).append("\t");
                    	sb.append(score_strain2.score).append("\t");
                    	sb.append(score_strain2.position).append("\t");
                    	sb.append(score_strain2.reverseCompliment?"-":"+").append("\t");
                    	sb.append(score_diff).append("\n");
                    	ps.print(sb.toString());
                    	//score_diff = scoreSingleSite(matrix, strain2_site)-scoreSingleSite(matrix, s288c_site);
                    	//System.err.printf("%s:\t%s\t%s\t%f\t%s\n", f[0],s288c_site, strain2_site, score_diff, r.toString());
                    	//System.out.printf("%s:\t%s\t%s\t%.3f\t%.3f\t%.3f\t%s\n", f[0],site, strain2_site, score_strain2,score_s288c, score_diff, r.toString());
                    }        			
        		}
            }
    		ps.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void printAlignmentSubstrings(){
		AlignmentProperties props = new AlignmentProperties();
        Genome g = props.getGenome("s288c");
        int flanking=5; // to extend the binding site sequence
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(new File("macIsaac_sites_FSA_sequences.txt")));
            AlignmentLoader loader = new AlignmentLoader();
    		String versionName = props.getAlignmentVersionName();
    		AlignmentVersion version = loader.loadAlignmentVersion(versionName);
    		for (MacIsaacSite site: bindingSites){
                //use sequence alignment to get corresponding sigma site
    			Region r = site.region;

        		Collection<AlignBlock> blocks = version.getAlignBlocks(r);
        		Set<Alignment> alignments = new LinkedHashSet<Alignment>();
        		for(AlignBlock block : blocks) { 
        			Alignment a = block.getAlignment();
        			alignments.add(a);
        		}
        		LinkedList<MultipleAlignment> aligns = new LinkedList<MultipleAlignment>();
        		for(Alignment a : alignments) { 
        			aligns.add(new MultipleAlignment(a));
        		}
        		for (MultipleAlignment ma: aligns){
        			String[] spec = ma.species();
        			String name="";
        			for (int i=0; i<spec.length;i++){
        				if (spec[i].indexOf(g.getVersion())!=-1){
        					name = spec[i];
        				}
        			}
        			
        			GappedAlignmentString sgd_algn = ma.getGappedAlignment(name);
                    //minus one: 1-based coord in blat --> 0-based coord in cgs code
        			int start=sgd_algn.ungappedToGapped(r.getStart()-1);
        			int end=sgd_algn.ungappedToGapped(r.getEnd()-1);
        			StringBuilder sb = new StringBuilder();
        			sb.append(site.id).append("\n");
        			for (int i=0; i<spec.length;i++){
        				sb.append(spec[i]).append("\t");
        				sb.append(ma.getGappedAlignment(spec[i]).substring(start-flanking, end+1+flanking).gappedString()).append("\n");
        			}
        			ps.print(sb.toString());
        		}
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    private String reverseComplement(String dna){
    	StringBuilder result=new StringBuilder();
    	for(int i = dna.length()-1; i >= 0 ;i--){ 
    		char nucleotide = dna.charAt(i);
            if (nucleotide == 'A')      nucleotide ='T';
            else if (nucleotide == 'T') nucleotide ='A';
            else if (nucleotide == 'G') nucleotide ='C';
            else if (nucleotide == 'C') nucleotide ='G';
            else if (nucleotide == 'a') nucleotide ='t';
            else if (nucleotide == 't') nucleotide ='a';
            else if (nucleotide == 'g') nucleotide ='c';
            else if (nucleotide == 'c') nucleotide ='g';
            else if (nucleotide == 'N') nucleotide ='N';
            else if (nucleotide == 'n') nucleotide ='n';
            result.append(nucleotide);
        }
    	return result.toString();
    }

    private float scoreSingleSite(WeightMatrix matrix, String seq){
    	int length = matrix.length();
    	char[] sequence = seq.toCharArray();
    	if (seq.length()!=length){
    		System.err.printf("Sequence (%s) length does not match motif length: %d\n", seq, length);
    		return 0;
    	}
        float score = (float)0.0;
        for (int j = 0; j < length; j++) {
            score += matrix.matrix[j][sequence[j]];
        }
        return score;
    }
    
    // scan the sequence and reverse compliment for best score
    private BestScore scoreBestSite(WeightMatrix matrix, String seq){
    	BestScore best = new BestScore();
    	best.score=java.lang.Float.NEGATIVE_INFINITY;
    	if (seq.length()<matrix.length()){
        	return best;
    	}
    	
    	WeightMatrixExpander exp = new WeightMatrixExpander(matrix, (float)matrix.getMaxScore());
    	float[] scores=exp.score(matrix, seq.toCharArray());
    	for (int i=0;i<scores.length-matrix.length()+1;i++){
    		if (scores[i]>best.score){
    			best.score=scores[i];
				best.position=i;
				best.reverseCompliment=false;
				best.sequence = seq.substring(i, i+matrix.length());
			}
    	}
    	String seq_r=reverseComplement(seq);
    	scores=exp.score(matrix, seq_r.toCharArray());
    	for (int i=0;i<scores.length-matrix.length()+1;i++){
    		if (scores[i]>best.score){
    			best.score=scores[i];
				best.position=i;
				best.reverseCompliment=true;
				best.sequence = seq_r.substring(i, i+matrix.length());
			}
    	}
        return best;
    }
    
    private class BestScore{
    	float score;
    	int position;
    	boolean reverseCompliment;
    	String sequence;
    }
    private class MacIsaacSite{
    	int id;
    	String tf;
    	String tf_sysName;
    	String sequence;
    	Region region;
    	String strand;
    	Set<Gene>targetGenes=new HashSet<Gene>();
    	double diffScore=0;	//s288c-sigma
    }
    private class DiffExpression{
    	String gene;
    	double diff;
    	double variance;
    	double significance;
    	String sign=null;
    }
}
