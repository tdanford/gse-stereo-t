/*
 * Author: tdanford
 * Date: Apr 12, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.assessment;

import java.util.*;
import java.awt.Color;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.SerialExpander;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.models.data.*;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.eye.ModelPaintableWrapper;
import edu.mit.csail.cgs.viz.eye.ModelScatter;
import edu.mit.csail.cgs.viz.eye.OverlayModelPaintable;
import edu.mit.csail.cgs.viz.paintable.HorizontalScalePainter;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;
import edu.mit.csail.cgs.viz.paintable.VerticalScalePainter;

/**
 * Assess the expression of two kinds of segments -- non-transcribed (flat) segments, and 
 * transcribed non-coding segments.  
 * 
 * @author tdanford
 */
public class SegmentExpression {
	
	public static void main(String[] args) {
		String[] keys = new String[] { "matalpha" };
		examineExpression("txns288c", keys);
		examineExpression("txnsigma", keys);
	}
	
	public static void examineExpression(String key, String[] expts) { 
		WorkflowProperties props = new WorkflowProperties();
		String expt = "matalpha";
		
		try {
			String strain = props.parseStrainFromKey(key);
			SegmentExpression expr = new SegmentExpression(props, key, expts);
			
			scatter(expr, expt);
		
			File f = props.getDirectory();
			f = new File(f, String.format("%s-%s-fig2-noncoding.txt", key, expt));
			PrintStream ps = new PrintStream(new FileOutputStream(f));
			
			//expr.printSegmentInformation(ps);
			expr.printSegmentClassification(expt, ps);
			
			ps.close();
			System.out.println(String.format("Output %s", f.getName()));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void scatter(SegmentExpression expr, String expt) { 
		Iterator<SegExpr> cexprs = expr.codingIntensityLengthRelation(expt, Coloring.clearer(Color.blue));
		Iterator<SegExpr> nexprs = expr.noncodingIntensityLengthRelation(expt, Coloring.clearer(Color.red));

		ModelScatter scatter = new ModelScatter();

		PaintableScale yscale = scatter.getPropertyValue(ModelScatter.yScaleKey);
		PaintableScale xscale = scatter.getPropertyValue(ModelScatter.xScaleKey);
		
		//scatter.setProperty(ModelScatter.xScaleKey, scale);
		//scatter.setProperty(ModelScatter.yScaleKey, scale);
		scatter.setProperty(ModelScatter.radiusKey, 2);
		
		scatter.addModels(cexprs);
		scatter.addModels(nexprs);
		OverlayModelPaintable overlay = new OverlayModelPaintable(
				new ModelPaintableWrapper(new VerticalScalePainter(yscale).drawZero(true)), 
				new ModelPaintableWrapper(new HorizontalScalePainter(xscale).drawZero(true)), 
				scatter);
		
		PaintableFrame pf = new PaintableFrame(String.format("%s:length (x) vs. intensity (y)", expt), overlay);
	}
	
	private WorkflowProperties props;
	private WorkflowIndexing indexing;
	private String key, strain, oppStrain; 
	private Genome genome;
	private WholeGenome whole;
	private Expander<Region,Gene> genes;
	
	private Map<String,SegmentInformation> segInfo;
	
	private Map<String,Integer[]> strainChs, oppStrainChs;
	
	public void printSegmentInformation(PrintStream ps) { 
		printHeader(ps);
		for(String id : segInfo.keySet()) { 
			ps.println(segInfo.get(id).toString());
		}
	}

	public SegmentExpression(WorkflowProperties ps, String k, String[] keys) throws IOException { 
		props = ps; 
		key = k;
		strain = props.parseStrainFromKey(key);
		oppStrain = strain.equals("s288c") ? "sigma" : "s288c";
		
		genome = props.getSigmaProperties().getGenome(strain);
		indexing = props.getIndexing(key);
		genes = strain.equals("s288c") ? 
				props.getSigmaProperties().getGeneGenerator(strain) : 
				new RefGeneGenerator(genome, "s288cMapped");

		if(strain.equals("sigma")) { 
			genes = new SerialExpander<Region,Gene>(genes,
					new RefGeneGenerator(genome, "sgdGene"));
		}

		String oppStrain = strain.equals("s288c") ? "sigma" : "s288c";
		
		strainChs = new TreeMap<String,Integer[]>();
		oppStrainChs = new TreeMap<String,Integer[]>();

		for(String str : keys) { 
			strainChs.put(str, indexing.findChannels(strain, str));
			oppStrainChs.put(str, indexing.findChannels(oppStrain, str));
		}
		
		segInfo = new TreeMap<String,SegmentInformation>();
		
		whole = WholeGenome.loadWholeGenome(ps, key);

		loadDataSegments();
	}
	
	public void loadDataSegments() throws IOException {
		whole.loadIterators();
		handleDataSegments(whole.getWatsonSegments());
		handleDataSegments(whole.getCrickSegments());
	}
	
	public void handleDataSegments(Iterator<DataSegment> segs) {
		double pvalue = props.getDifferentialPValue();
		
		while(segs.hasNext()) { 
			DataSegment s = segs.next();
			StrandedRegion segRegion = 
				new StrandedRegion(genome, s.chrom, s.firstLocation()-1, 
						s.lastLocation()+1, s.strand.charAt(0));
			
			SegmentInformation info = new SegmentInformation(segRegion);
			String id = info.id;

			info.probes = s.dataLocations.length;

			//System.out.println(String.format("%s (%d)", id, info.probes));

			int threep = segRegion.getStrand() == '+' ? 
					segRegion.getEnd() : 
					segRegion.getStart();
					
			Iterator<Gene> overGenes = genes.execute(segRegion);
			while(overGenes.hasNext()) { 
				Gene gene = overGenes.next();
				if(gene.getStrand() == segRegion.getStrand()) { 
					info.codingGenes.add(gene.getID());
				} else { 
					info.nonCodingGenes.add(gene.getID());
				}
			}

			for(String key : strainChs.keySet()) { 
				if(s.hasConsistentType(Segment.LINE, strainChs.get(key))) {
					
					double predicted = averagePredicted(s, threep, strainChs.get(key));
					double oppPredicted = averagePredicted(s, threep, oppStrainChs.get(key));
					double differential = s.getExpectedDifferential(key);
					
					if(s.isDifferential(key, pvalue)) { 
						info.diffs.add(key);
					}

					info.keyExpr.put(key, predicted);
					info.oppExpr.put(key, oppPredicted);
					info.keyDiffs.put(key, differential);

					double slope = s.segmentParameters[strainChs.get(key)[0]][1];
					if(segRegion.getStrand() == '-') { slope = -slope; }
					
					info.keySlope.put(key, slope);

					//System.out.println(String.format("\t%s %.2f / %.2f, slope: %.2f", key, predicted, oppPredicted, slope));
				}
			}
			
			segInfo.put(id, info);
		}
	}
	
	private double averagePredicted(DataSegment s, int loc, Integer[] chans) { 
		double sum = 0.0;
		int c = 0;
		for(int i = 0; i < chans.length; i++) { 
			double pred = s.predicted(chans[i], loc);
			sum += pred; 
			c += 1;
		}
		return sum / (double)Math.max(1, c);
	}
	
	public Iterator<SegExpr> codingIntensityLengthRelation(String key, Color c) { 
		ArrayList<SegExpr> exps = new ArrayList<SegExpr>();
		for(String id : segInfo.keySet()) { 
			SegmentInformation info = segInfo.get(id);
			if(info.keyExpr.containsKey(key) && !info.codingGenes.isEmpty()) { 
				SegExpr e = new SegExpr(id, (double)info.region.getWidth(), info.keyExpr.get(key));
				e.color = c;
				exps.add(e);
			}
		}
		return exps.iterator();
	}
	
	public Iterator<SegExpr> noncodingIntensityLengthRelation(String key, Color c) { 
		ArrayList<SegExpr> exps = new ArrayList<SegExpr>();
		for(String id : segInfo.keySet()) { 
			SegmentInformation info = segInfo.get(id);
			if(info.keyExpr.containsKey(key) && info.codingGenes.isEmpty()) { 
				SegExpr e = new SegExpr(id, (double)info.region.getWidth(), info.keyExpr.get(key));
				e.color = c;
				exps.add(e);
			}
		}
		return exps.iterator();
	}
	
	public void printSegmentClassification(String expt, PrintStream ps) { 
		for(String id : segInfo.keySet()) { 
			SegmentInformation info = segInfo.get(id);
			boolean expr = info.keyExpr.containsKey(expt);
			boolean diff = info.diffs.contains(expt);
			//double diffValue = expr ? info.keyExpr.get(expt) - info.oppExpr.get(expt) : 0.0;
			double diffValue = expr ? info.keyDiffs.get(expt) : 0.0;
			
			String coding = "";
			
			if(!info.codingGenes.isEmpty()) { 
				coding += "C";
			} 
			
			if (!info.nonCodingGenes.isEmpty()) { 
				coding += "A";
			}
			if(coding.length() == 0) { coding = "I"; }
			
			String exprString = expr ? "E" : "N";
			String typeString = "same";
			if(diff) { 
				if(diffValue >= 0.0) { 
					typeString = "s288c-diff";
				} else { 
					typeString = "sigma-diff";
				}
			}
			
			ps.println(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%f", 
					id, exprString, coding, typeString, 
					combine(info.codingGenes), combine(info.nonCodingGenes), diffValue));
		}
	}
	
	private class SegmentInformation {
		
		public StrandedRegion region;
		public String id;
		public Set<String> codingGenes, nonCodingGenes;
		public Map<String,Double> keyExpr, oppExpr, keyDiffs;
		public Map<String,Double> keySlope;
		public Set<String> diffs;
		public int probes; 
		
		public SegmentInformation(StrandedRegion g) { 
			region = g;
			id = String.format("%s:%d-%d:%c", 
					region.getChrom(), region.getStart(), 
					region.getEnd(), region.getStrand());
			probes = 0;
			keyExpr = new TreeMap<String,Double>();
			oppExpr = new TreeMap<String,Double>();
			keySlope = new TreeMap<String,Double>();
			keyDiffs = new TreeMap<String,Double>();
			
			codingGenes = new TreeSet<String>();
			nonCodingGenes = new TreeSet<String>();
			diffs = new TreeSet<String>();
		}
		
		public String toString() { 
			return String.format("%s\t%d\t%s\t%s\t%s", 
					id, probes, combine(codingGenes), combine(nonCodingGenes),
					combine(diffs)); 
		}
	}

	private static String combine(Set<String> s) { 
		StringBuilder sb = new StringBuilder();
		for(String ss : s) { 
			if(sb.length() > 0) { 
				sb.append(",");
			}
			sb.append(ss);
		}
		return sb.toString();
	}

	public static void printHeader(PrintStream ps) { 
		ps.println("Location:\t#Probes:\tCoding:\tNoncoding:\tDiff:\tUnique:");
	}
	
	public static class SegExpr extends Model { 

		public Double x, y; 
		public String id; 
		public Color color;
		
		public SegExpr(String i, Double x, Double y) { 
			id = i; 
			this.x = x; 
			this.y = y;
			
			color = Color.red;
			if(Math.abs(x - y) >= Math.log(2.0)) { 
				color = Color.blue;
			}
		}
		
		public SegExpr() {}
		
		public String toString() { return String.format("%s: %.2f / %2.f", id, x, y); } 
 		
		public int hashCode() { return id.hashCode(); }
		public boolean equals(Object o) { 
			if(!(o instanceof SegExpr)) { return false; }
			return id.equals(((SegExpr)o).id);
		}
	}
}


