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
import edu.mit.csail.cgs.sigma.IteratorCacher;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.SerialExpander;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.sigma.genes.GeneNameAssociation;
import edu.mit.csail.cgs.sigma.motifs.MotifProperties;
import edu.mit.csail.cgs.sigma.motifs.TFList;
import edu.mit.csail.cgs.utils.SetTools;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;
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
 * Assigns an expression value to each particular gene.  
 * 
 * @author tdanford
 */
public class GeneExpression {

	public static void main(String[] args) { 
		WorkflowProperties props = new WorkflowProperties();
		SetTools<String> tools = new SetTools<String>();
		GeneAnnotationProperties gaps = new GeneAnnotationProperties();
		MotifProperties mps = new MotifProperties();

		try {
			System.out.println("loading gene-name associations...");
			GeneNameAssociation assoc = gaps.getGeneNameAssociation("s288c");
			TFList tfs = new TFList(mps);
			Set<String> tforfs = new TreeSet<String>();
			for(String tf : tfs.getTFs()) { 
				tforfs.addAll(assoc.getIDs(tf));
			}

			System.out.println("Loading sigma expression...");
			GeneExpression sigmaExpr = new GeneExpression(props, "txnsigma");
			System.out.println("Loading s288c expression...");
			GeneExpression s288cExpr = new GeneExpression(props, "txns288c");

			String expt = "matalpha";

			File output = props.getDirectory();
			output = new File(output, String.format("fig2-coding.txt"));

			System.out.println(String.format("Outputting : %s", output.getAbsolutePath()));
			printFig2File(s288cExpr, sigmaExpr, expt, output);

			Collection<GeneExpr> saValues = s288cExpr.senseAntisenseValues(expt);
			Collection<GeneExpr> saDiffs = s288cExpr.senseAntisenseDiffs(expt);

			ModelScatter valueScatter = new ModelScatter();
			ModelScatter diffScatter = new ModelScatter();

			valueScatter.synchronizeProperty(ModelScatter.xScaleKey, valueScatter, ModelScatter.yScaleKey);
			diffScatter.synchronizeProperty(ModelScatter.xScaleKey, diffScatter, ModelScatter.yScaleKey);

			valueScatter.addModels(saValues.iterator());
			diffScatter.addModels(saDiffs.iterator());
			
			ModelScatter.InteractiveFrame pfDiffs = new ModelScatter.InteractiveFrame(diffScatter, "Sense/Antisense Diffs");

			PaintableFrame pfValues = new PaintableFrame("Sense/Antisense Values", valueScatter);
			//PaintableFrame pfDiffs = new PaintableFrame("Sense/Antisense Diffs", diffScatter);

		} catch (IOException e) {
			e.printStackTrace();
		}

		//SegmentExpression.main(args);
	}

	public static void printFig2File(GeneExpression s288c, GeneExpression sigma, 
			String expt, File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		printFig2File(s288c, sigma, expt, ps);
		ps.close();
	}

	public static void printFig2File(
			GeneExpression s288c, GeneExpression sigma, 
			String expt, PrintStream ps) { 

		Set<String> genes = new TreeSet<String>();
		genes.addAll(s288c.genes());
		genes.addAll(sigma.genes());

		Set<String> arrayed = tools.union(s288c.arrayed(true, null), sigma.arrayed(true, null));

		Set<String> s288cOnly = s288c.conserved(false, null);
		Set<String> sigmaOnly = sigma.conserved(false, null);

		Set<String> s288cExpr = s288c.expressed(expt, true, null);
		Set<String> sigmaExpr = sigma.expressed(expt, true, null);

		Set<String> expr = new TreeSet<String>();
		expr.addAll(s288cExpr);
		expr.addAll(sigmaExpr);

		Map<String,Double> diffValue = new TreeMap<String,Double>();

		Set<String> s288cDiff = 
			s288c.overExpressed(expt, true, 
					s288c.differential(expt, true, null));

		Set<String> sigmaDiff = 
			sigma.overExpressed(expt, true, 
					sigma.differential(expt, true, null));

		s288cDiff = tools.subtract(s288cDiff, sigmaDiff);
		sigmaDiff = tools.subtract(sigmaDiff, s288cDiff);

		for(String gene : expr) { 

			Double s288cValue = s288c.geneInfo.containsKey(gene) ? s288c.geneInfo.get(gene).keyDiff.get(expt) : null;
			Double sigmaValue = sigma.geneInfo.containsKey(gene) ? sigma.geneInfo.get(gene).keyDiff.get(expt) : null;

			Double s1 = s288cValue; 
			Double s2 = sigmaValue;
			if(s1 == null) { s1 = s2; }
			if(s2 == null) { s2 = s1; }

			if(s1 != null && s2 != null) {
				double avg = (s1 + (1.0-s2)) / 2.0;
				diffValue.put(gene, avg);
			}
		}

		for(String gene : genes) { 
			String v1 = "both";
			if(s288cOnly.contains(gene)) { v1 = "s288c"; }
			if(sigmaOnly.contains(gene)) { v1 = "sigma"; }

			String v2 = "?";
			if(arrayed.contains(gene)) { 
				v2 = expr.contains(gene) ? "E" : "N";
			}
			String v3 = "same";
			if(s288cDiff.contains(gene)) { 
				v3 = "s288c-diff"; 
			} else if (sigmaDiff.contains(gene)) { 
				v3 = "sigma-diff";
			}

			double v4 = diffValue.containsKey(gene) ? diffValue.get(gene): 0.0;

			ps.println(String.format("%s %s %s %s (%f)", gene, v1, v2, v3, v4));
		}
	}

	private static SetTools<String> tools = new SetTools<String>();

	private static void printList(PrintStream ps, String title, Collection<String> lst) { 
		ps.println(String.format("\n-- %s ---------------------", title));
		for(String v : lst) { 
			ps.println(v);
		}
	}

	private WorkflowProperties props;
	private WorkflowIndexing indexing;
	private String key, strain, oppStrain;
	private Genome genome;
	private Map<String,GeneInformation> geneInfo;
	private OverlappingRegionFinder<Gene> geneFinder;
	private SharedSequenceAnnotations annots;

	private Map<String,Integer[]> keyedChannels;

	public GeneExpression(WorkflowProperties ps, String k) throws IOException { 
		props = ps; 
		key = k;
		this.strain = props.parseStrainFromKey(key);
		oppStrain = strain.equals("s288c") ? "sigma" : "s288c";

		genome = props.getSigmaProperties().getGenome(strain);
		indexing = props.getIndexing(key);

		System.out.println("\tLoading shared-annotation information...");
		annots = new SharedSequenceAnnotations();

		keyedChannels = new TreeMap<String,Integer[]>();
		for(String str : new String[] { "mata", "matalpha", "diploid" }) { 
			keyedChannels.put(str, indexing.findChannels(strain, str));
		}

		System.out.println("\tLoading genes...");
		Expander<Region,Gene> geneGen = props.getSigmaProperties().getGeneGenerator(strain);
		if(strain.equals("sigma")) { 
			geneGen = new SerialExpander<Region,Gene>(geneGen,
					new RefGeneGenerator(genome, "sgdGene"));
		}
		GenomeExpander<Gene> expander = new GenomeExpander<Gene>(geneGen);
		IteratorCacher<Gene> cacher = new IteratorCacher<Gene>(expander.execute(genome));
		System.out.println("\t# Genes: " + cacher.size());

		System.out.println("\tLoading GeneInformation objects...");
		Iterator<Gene> gs = cacher.iterator();
		geneInfo = new TreeMap<String,GeneInformation>();
		while(gs.hasNext()) { 
			Gene g = gs.next();
			GeneInformation info = new GeneInformation(g);
			geneInfo.put(g.getID(), info);
			info.conserved = annots.isMappedGene(g.getID());
		}

		geneFinder = new OverlappingRegionFinder(cacher.iterator());

		System.out.println("\tLoading data segments...");
		loadDataSegments();

		System.out.println(String.format("%s : loaded %d gene information.", key, geneInfo.size()));
	}

	public Iterator<GeneExpr> comparativeExpression(GeneExpression expr, String key) { 
		ArrayList<GeneExpr> exprs = new ArrayList<GeneExpr>();
		for(String id : geneInfo.keySet()) { 
			if(expr.geneInfo.containsKey(id)) { 
				GeneInformation infox = geneInfo.get(id), infoy = expr.geneInfo.get(id);
				if(infox.keyExpr.containsKey(key) && 
						infoy.keyExpr.containsKey(key)) { 

					GeneExpr e = new GeneExpr(id, infox.keyExpr.get(key), 
							infoy.keyExpr.get(key));
					e.expressed = infox.expressed.contains(key) && infoy.expressed.contains(key);

					exprs.add(e);
				}
			}
		}

		return exprs.iterator();
	}

	public ArrayList<GeneExpr> senseAntisenseValues(String key) { 
		ArrayList<GeneExpr> vs = new ArrayList<GeneExpr>();
		for(String id : geneInfo.keySet()) { 
			GeneInformation info = geneInfo.get(id);
			if(!info.keyExpr.isEmpty() && !info.keyAntiExpr.isEmpty()) {
				GeneExpr pt = info.senseAntisenseValues(key);
				if(pt.color == Color.red) { 
					vs.add(info.senseAntisenseValues(key));
				}
			}
		}
		return vs;
	}

	public ArrayList<GeneExpr> senseAntisenseDiffs(String key) { 
		ArrayList<GeneExpr> vs = new ArrayList<GeneExpr>();
		int[] c = new int[4];
		for(int i = 0; i < c.length; i++) { c[i] = 0; }
		
		for(String id : geneInfo.keySet()) { 
			GeneInformation info = geneInfo.get(id);
			if(info.expressed.contains(key) && info.antiExpressed.contains(key)) {
				GeneExpr pt = info.senseAntisenseDiffs(key);
				if(pt.color==Color.red) { 
					vs.add(pt);
				}
				if(info.differential.contains(key) && info.antiDifferential.contains(key)) {
					int category = info.diffCategory(key);
					c[category] += 1;
					System.out.println(String.format("%d %s", category, id));
				}
			}
		}
		
		System.out.println(String.format("# Sense Down, Anti Down: %d", c[0]));
		System.out.println(String.format("# Sense Up, Anti Down: %d", c[1]));
		System.out.println(String.format("# Sense Down, Anti Up: %d", c[2]));
		System.out.println(String.format("# Sense Up, Anti Up: %d", c[3]));
		
		return vs;
	}

	public int size() { return geneInfo.size(); }

	public Set<String> genes() { return geneInfo.keySet(); }

	public Set<String> conserved(boolean cons, Set<String> superset) { 
		TreeSet<String> ids = new TreeSet<String>();
		for(String id : geneInfo.keySet()) { 
			GeneInformation info = geneInfo.get(id);
			if(superset == null || superset.contains(id)) { 
				if(info.conserved == cons) { 
					ids.add(id);
				}
			}
		}
		return ids;
	}

	public Set<String> arrayed(boolean arr, Set<String> superset) { 
		TreeSet<String> ids = new TreeSet<String>();
		for(String id : geneInfo.keySet()) { 
			GeneInformation info = geneInfo.get(id);
			if(superset == null || superset.contains(id)) { 
				if((info.segments > 0) == arr) { 
					ids.add(id);
				}
			}
		}
		return ids;
	}

	public Set<String> expressed(String expt, boolean expr, Set<String> superset) { 
		TreeSet<String> ids = new TreeSet<String>();
		for(String id : geneInfo.keySet()) { 
			GeneInformation info = geneInfo.get(id);
			if(superset == null || superset.contains(id)) { 
				if(info.expressed.contains(expt) == expr) { 
					ids.add(id);
				}
			}
		}
		return ids;
	}

	public Set<String> differential(String expt, boolean diff, Set<String> superset) { 
		TreeSet<String> ids = new TreeSet<String>();
		for(String id : geneInfo.keySet()) { 
			GeneInformation info = geneInfo.get(id);
			if(superset == null || superset.contains(id)) {
				if(info.differential.contains(expt) == diff) { 					
					ids.add(id);
				}
			}
		}
		return ids;
	}

	public Set<String> overExpressed(String expt, boolean up, Set<String> superset) { 
		TreeSet<String> ids = new TreeSet<String>();
		for(String id : geneInfo.keySet()) { 
			GeneInformation info = geneInfo.get(id);
			if(superset == null || superset.contains(id)) {
				if(up && info.keyDiff.get(expt) > 0.5) { 
					ids.add(id);
				} else if (!up && info.keyDiff.get(expt) < 0.5) { 
					ids.add(id);
				}
			}
		}
		return ids;
	}

	public void loadDataSegments() throws IOException { 
		WholeGenome g = WholeGenome.loadWholeGenome(props, key);
		g.loadIterators();

		handleDataSegments(new SerialIterator<DataSegment>(g.getWatsonSegments(), g.getCrickSegments()));
	}

	public void handleDataSegments(Iterator<DataSegment> segs) {
		int threePrimeSpacing = 100;
		int nsegs = 0;
		double pvalue = props.getDifferentialPValue();

		while(segs.hasNext()) { 
			DataSegment s = segs.next();
			Region segRegion = new Region(genome, s.chrom, s.firstLocation()-1, s.lastLocation()+1);

			Collection<Gene> overlappingGenes = geneFinder.findOverlapping(segRegion);

			for(Gene g : overlappingGenes) {

				int threep = g.getStrand() == '+' ? g.getEnd() : g.getStart();
				int fivep = g.getStrand() == '+' ? g.getStart() : g.getEnd();

				int other3p = g.getStrand() == '+' ? Math.max(threep - threePrimeSpacing, g.getStart()) : Math.min(threep + threePrimeSpacing, g.getEnd());

				int other5p = g.getStrand() == '+' ? Math.max(threep - threePrimeSpacing, g.getStart()) : Math.min(threep + threePrimeSpacing, g.getEnd());

				int start3, end3; 
				if(g.getStrand() == '+') { 
					start3 = other3p; end3 = threep;
				} else { 
					start3 = threep; end3 = other3p;
				}

				int start5, end5; 
				if(g.getStrand() == '-') { 
					start5 = other5p; end5 = fivep;
				} else { 
					start5 = fivep; end5 = other5p;
				}

				Region threePrimeRegion = new Region(genome, g.getChrom(), start3, end3); 
				Point threePrimePoint = new Point(genome, g.getChrom(), threep);

				Region fivePrimeRegion = new Region(genome, g.getChrom(), start5, end5); 
				Point fivePrimePoint = new Point(genome, g.getChrom(), fivep);

				GeneInformation info = geneInfo.get(g.getID());

				if(g.getStrand() == s.strand.charAt(0)) { 
					info.segments++;

					// We take the segment that overlaps the three-prime end (if there is one), 
					// or a "nearby" segment within the gene if we haven't recorded any expression
					// information for this gene yet....
					if(segRegion.contains(threePrimePoint) || 
							(segRegion.overlaps(threePrimeRegion) && info.keyExpr.isEmpty())) {

						//info.clear();

						for(String key : keyedChannels.keySet()) {

							Integer[] chs = keyedChannels.get(key);
							Integer[] oppChs = indexing.findChannels(oppStrain, key);

							double predicted = averagePredicted(s, threep, chs);
							double differentialScore = s.getExpectedDifferential(key);

							info.keyExpr.put(key, predicted);
							info.keyDiff.put(key, differentialScore);

							if(s.hasConsistentType(Segment.LINE, chs)) { 
								info.expressed.add(key);

								if(s.isDifferential(key, pvalue)) {
									info.differential.add(key);
								}
							}

							if(s.hasType(Segment.LINE, chs)) { 
								info.liberalExpressed.add(key);
							}

							if(s.hasConsistentType(Segment.LINE, oppChs)) { 
								info.oppExpressed.add(key);
							}

							//System.out.println(String.format("%s (%s:%d:%s) %s -> %.2f",
							//		g.getID(), s.chrom, threep, s.strand, key, predicted));
						}
					}

				} else {
					// antisense

					if(segRegion.contains(fivePrimePoint) || 
							(segRegion.overlaps(fivePrimeRegion) && info.keyAntiExpr.isEmpty())) {

						//info.clear();

						for(String key : keyedChannels.keySet()) {

							Integer[] chs = keyedChannels.get(key);
							Integer[] oppChs = indexing.findChannels(oppStrain, key);

							double predicted = averagePredicted(s, fivep, chs);
							double differentialScore = s.getExpectedDifferential(key);

							info.keyAntiExpr.put(key, predicted);
							info.keyAntiDiff.put(key, differentialScore);

							if(s.hasConsistentType(Segment.LINE, chs)) { 
								info.antiExpressed.add(key);
								
								if(s.isDifferential(key, pvalue)) {
									info.antiDifferential.add(key);
								}
							}

						}
					}
				}

				nsegs += 1;
				if(nsegs % 100 == 0) { System.out.print("."); System.out.flush(); }
				if(nsegs % 1000 == 0) { System.out.println("(" + nsegs/1000 + "k)"); System.out.flush(); }
			}
			System.out.println();
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

	private class GeneInformation {

		public Gene gene;
		public Map<String,Double> keyExpr, keyDiff, keyAntiExpr, keyAntiDiff;
		public Set<String> expressed, oppExpressed, liberalExpressed, differential, antiDifferential, antiExpressed;
		public boolean conserved;
		public int segments;

		public GeneInformation(Gene g) { 
			gene = g;
			keyExpr = new TreeMap<String,Double>();
			keyDiff = new TreeMap<String,Double>();
			keyAntiExpr = new TreeMap<String,Double>();
			keyAntiDiff = new TreeMap<String,Double>();
			expressed = new TreeSet<String>();
			differential = new TreeSet<String>();
			antiExpressed = new TreeSet<String>();
			antiDifferential = new TreeSet<String>();
			liberalExpressed = new TreeSet<String>();
			oppExpressed = new TreeSet<String>();
			conserved = false;
			segments = 0;
		}

		public void clear() {
			keyExpr.clear();
			keyDiff.clear();
			keyAntiExpr.clear();
			keyAntiDiff.clear();
			expressed.clear();
			differential.clear();
			antiDifferential.clear();
			antiExpressed.clear();
			liberalExpressed.clear();
			oppExpressed.clear();
		}
		
		public int diffCategory(String key) { 
			boolean sensePos = keyDiff.get(key) >= 0.0;
			boolean antiPos = keyAntiDiff.get(key) >= 0.0;
			
			int d = 0;
			d += sensePos ? 1 : 0;
			d += antiPos ? 2 : 0;
			
			return d;
		}

		public GeneExpr senseAntisenseValues(String key) { 
			GeneExpr e = new GeneExpr(gene.getID(), keyExpr.get(key), keyAntiExpr.get(key));
			
			if(antiDifferential.contains(key) || differential.contains(key)) { 
				if(antiDifferential.contains(key) && differential.contains(key)) { 
					e.color= Color.red;
				} else { 
					e.color = Color.orange;
				}
			/*
			if(expressed.contains(key) || antiExpressed.contains(key)) {
				if(expressed.contains(key) && antiExpressed.contains(key)) { 
					e.color = Color.red;
				} else { 
					e.color = Color.orange;
				}
				*/
			} else { 
				e.color = Coloring.clearer(Coloring.clearer(Color.blue));				
			}
			
			return e;
		}

		public GeneExpr senseAntisenseDiffs(String key) { 
			GeneExpr e = new GeneExpr(gene.getID(), keyDiff.get(key), keyAntiDiff.get(key));
			if(antiDifferential.contains(key) || differential.contains(key)) { 
				if(antiDifferential.contains(key) && differential.contains(key)) { 
					e.color= Color.red;
				} else { 
					e.color = Color.orange;
				}
			} else { 
				e.color = Coloring.clearer(Color.blue);
			}
			return e;
		}
	}

	public static class GeneExpr extends Model {

		public Double x, y; 
		public String id; 
		public Color color;
		public boolean expressed;

		public GeneExpr(String i, Double x, Double y) { 
			id = i; 
			this.x = x; 
			this.y = y;
			color = Coloring.clearer(Color.blue);
			expressed = false;

			if(diff(2.0)) { 
				color = Color.orange;
			}
		}

		public boolean diff(double foldChange) { 
			return Math.abs(x-y) >= Math.log(foldChange);
		}

		public GeneExpr() {}

		public String toString() { return String.format("%s: %.2f / %.2f", id, x, y); } 

		public int hashCode() { return id.hashCode(); }
		public boolean equals(Object o) { 
			if(!(o instanceof GeneExpr)) { return false; }
			return id.equals(((GeneExpr)o).id);
		}
	}
}




