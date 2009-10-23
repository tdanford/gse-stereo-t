/*
 * Author: tdanford
 * Date: Mar 10, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz.genome;

import java.util.*;
import java.awt.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.datasets.chippet.RunningOverlapSum;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.sigma.GeneGenerator;
import edu.mit.csail.cgs.sigma.IteratorCacher;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.expression.StrandFilter;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;
import edu.mit.csail.cgs.sigma.viz.chroms.*;
import edu.mit.csail.cgs.sigma.viz.chroms.information.*;
import edu.mit.csail.cgs.sigma.viz.chroms.renderers.*;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.UnitCoverage;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.paintable.AbstractPaintable;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.ewok.verbs.sequence.RegionCoverage;

public class WholeGenomeView extends AbstractPaintable {

	public static void main(String[] args) { 
		WorkflowProperties ps = new WorkflowProperties();
		String strain = args.length > 1 ? args[0] : "s288c";
		try {
			WholeGenomeView view = new WholeGenomeView(ps, strain);
			
			//view.addGeneRegions();
			view.addGenes();
			
			view.whole.jointCoverage("mata", "genes", "mataC");
			view.whole.subtractCoverage("mata", "genes", "mataNC");
			view.whole.subtractCoverage("mataNC", "genes", "mataNC", false);
			view.whole.jointCoverage("diff:mata", "mataC", "diff:mataC");
			view.whole.jointCoverage("diff:mata", "mataNC", "diff:mataNC");
			view.whole.jointCoverage("mata", "mata", "inter:mata", false);
			
			view.whole.jointCoverage("diploid", "genes", "dipC");
			view.whole.subtractCoverage("diploid", "genes", "dipNC");
			view.whole.subtractCoverage("dipNC", "genes", "dipNC", false);
			view.whole.jointCoverage("diff:diploid", "dipC", "diff:diploidC");
			view.whole.jointCoverage("diff:diploid", "dipNC", "diff:diploidNC");
			view.whole.jointCoverage("diploid", "diploid", "inter:diploid", false);

			view.whole.jointCoverage("matalpha", "genes", "matalphaC");
			view.whole.subtractCoverage("matalpha", "genes", "matalphaNC");
			view.whole.subtractCoverage("matalphaNC", "genes", "matalphaNC", false);
			view.whole.jointCoverage("diff:matalpha", "matalphaC", "diff:matalphaC");
			view.whole.jointCoverage("diff:matalpha", "matalphaNC", "diff:matalphaNC");
			view.whole.jointCoverage("matalpha", "matalpha", "inter:matalpha", false);

			view.whole.subtractCoverage("inter:matalpha", "genes", "toggle:matalpha", true); 
			view.whole.subtractCoverage("toggle:matalpha", "genes", "toggle:matalpha", false); 
			
			//view.addShadedTrack("diff:mataNC", Color.red);
			//view.addShadedTrack("diff:mataC", Coloring.clearer(Color.blue));

			//view.addShadedTrack("mataNC", Color.red);
			
			//view.addShadedTrack("matalpha", Color.blue);
			view.addShadedTrack("inter:matalpha", Color.red);
			view.addShadedTrack("toggle:matalpha", Color.orange);
			
			view.whole.printTrack(System.out, "toggle:matalpha");
			
			//view.addShadedTrack("mataNC", Color.red);
			//view.addShadedTrack("mataC", Coloring.clearer(Color.blue));
			
			//view.addDivergent("mata", "mata", 1000);
			//view.addConvergent("mata", "mata", 1000);

			view.restrictChromosomes("1");

			PaintableFrame pf = new PaintableFrame("Genome: " + strain, view);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private WorkflowProperties props;
	private String strain;
	private Genome genome;
	private TreeMap<String,ChromosomeView> chromViews;
	
	private WholeGenome whole;
	
	public WholeGenomeView(WorkflowProperties ps, String strain) throws IOException { 
		props = ps;
		this.strain = strain;
		genome = ps.getSigmaProperties().getGenome(strain);
		//whole = new WholeGenome(props, strain);
		whole = WholeGenome.loadWholeGenome(props, strain);

		chromViews = new TreeMap<String,ChromosomeView>();
		for(String chr : genome.getChromList()) {
			if(!chr.equals("mt")) { 
				chromViews.put(chr, new ChromosomeView(genome, chr));
				chromViews.get(chr).addPaintableChangedListener(this);
			}
		}

	}
	
	public void restrictChromosomes(String... chrs) { 
		Set<String> chrset = new TreeSet<String>();
		for(String c : chrs) { chrset.add(c); }
		TreeMap<String,ChromosomeView> views = new TreeMap<String,ChromosomeView>();
		for(String c : chromViews.keySet()) { 
			if(chrset.contains(c)) { 
				views.put(c, chromViews.get(c));
			} else { 
				chromViews.get(c).removePaintableChangedListener(this);
			}
		}
		
		chromViews = views;
		dispatchChangedEvent();
	}
	
	private class WGMultiPainter implements MultiChromPainter {

		private String chrom;
		private char strand;

		private boolean[][] covered;
		private String[] keys;
		private int length, size;

		public WGMultiPainter(String c, char str, String... ks) { 
			chrom = c;
			strand = str;
			length = genome.getChromLength(chrom);
			size = 1;
			keys = ks.clone();
			
			covered = null;
		}
		
		public int height() {
			return keys.length;
		}

		public int length() {
			return length;
		}

		public void recalculate(int units) {
			covered = new boolean[units][keys.length];
			int blockWidth = (int)Math.floor((double)length / (double)units);
			for(int i = 0; i < units; i++) { 
				int start = i * blockWidth;
				int end = start + blockWidth;
				StrandedRegion r = new StrandedRegion(genome, chrom, start, end, strand);
				
				for(int j = 0; j < keys.length; j++) { 
					covered[i][j] = whole.hasOverlapping(keys[j], r);
				}
			}
			
			size = units;
		}

		public int size() {
			return size;
		}

		public Character strand() {
			return strand;
		}

		public boolean[] value(int unit) {
			return covered[unit];
		} 
	}

	private class WGLocationSets implements ChromLocationSets {
		
		private int length, size; 
		private ArrayList<Integer[]> locs;
		
		public WGLocationSets(Collection<Pair<StrandedRegion,StrandedRegion>> matched,
				String chrom, 
				boolean firstSide, boolean secondSide) { 
			size = 1;
			length = genome.getChromLength(chrom);
			locs = new ArrayList<Integer[]>();
			System.out.println(String.format("Adding %d matched pairs...", matched.size()));
			
			for(Pair<StrandedRegion,StrandedRegion> m : matched) { 
				StrandedRegion s1 = m.getFirst(), s2 = m.getLast();
				if(s1.getChrom().equals(chrom) && s2.getChrom().equals(chrom)) { 
					int v1 = firstSide ? s1.getStart() : s1.getEnd();
					int v2 = secondSide ? s2.getStart() : s2.getEnd();
					locs.add(new Integer[] { v1, v2 });
					System.out.println(String.format("\t%d,%d", v1, v2));
				}
			}
		}

		public Iterator<Integer[]> locations() {
			return locs.iterator();
		}

		public int length() {
			return length;
		}

		public void recalculate(int units) {
		}

		public int size() {
			return size; 
		} 
		
	}

	private class WGPainter implements ChromPainter {

		private String chrom;
		private char strand;

		private boolean[] covered;
		private String key;
		private int length, size;

		public WGPainter(String c, char str, String k) { 
			chrom = c;
			strand = str;
			length = genome.getChromLength(chrom);
			size = 1;
			key = k;
			
			covered = null;
		}
		
		public int length() {
			return length;
		}

		public void recalculate(int units) {
			covered = new boolean[units];
			int blockWidth = (int)Math.floor((double)length / (double)units);
			for(int i = 0; i < units; i++) { 
				int start = i * blockWidth;
				int end = start + blockWidth;
				StrandedRegion r = new StrandedRegion(genome, chrom, start, end, strand);
				covered[i] = whole.hasOverlapping(key, r);
			}
			
			size = units;
		}

		public int size() {
			return size;
		}

		public Character strand() {
			return strand;
		}

		public boolean value(int unit) {
			return covered[unit];
		} 
	}

	private class WGScorer implements ChromScorer {

		private String chrom;
		private char strand;

		private Double[] covered;
		private String key;
		private int length, size;

		public WGScorer(String c, char str, String k) { 
			chrom = c;
			strand = str;
			length = genome.getChromLength(chrom);
			size = 1;
			key = k;
			
			covered = null;
		}
		
		public int length() {
			return length;
		}

		public void recalculate(int units) {
			covered = new Double[units];
			int blockWidth = (int)Math.floor((double)length / (double)units);
			for(int i = 0; i < units; i++) { 
				int start = i * blockWidth;
				int end = start + blockWidth;
				StrandedRegion r = new StrandedRegion(genome, chrom, start, end, strand);
				int coverage = whole.coverage(key, r);
				covered[i] = (double)coverage / (double)Math.max(1, r.getWidth());
				if(coverage > r.getWidth()) { 
					System.err.println(String.format("%d / %d = %.3f", coverage, r.getWidth(), covered[i]));
					//throw new IllegalArgumentException(); 
				}
			}
			
			size = units;
		}

		public int size() {
			return size;
		}

		public Character strand() {
			return strand;
		}

		public Double value(int unit) {
			return covered[unit];
		}

		public Double max() {
			return 1.0;
		}

		public Double zero() {
			return 0.0;
		} 
	}
	
	private class DivergentSiteInformation implements ChromLocations {
		
		private String chrom;
		private int length, size;
		private ArrayList<Integer> locations;
		private ArrayList<Integer> points;
		
		public DivergentSiteInformation(String c, int maxSize, ArrayList<Region> divregs) { 
			chrom = c;
			length = genome.getChromLength(chrom);
			size = 1;
			
			locations = new ArrayList<Integer>();
			points = new ArrayList<Integer>();
			
			for(Region r : divregs) { 
				if(r.getChrom().equals(c) && r.getWidth() <= maxSize) { 
					int m = (r.getStart()+r.getEnd())/2;
					locations.add(m);
				}
			}
		}

		public Iterator<Integer> locations() {
			//return points.iterator();
			return locations.iterator();
		}

		public int length() {
			return length;
		}

		public void recalculate(int units) {
			points.clear();
			for(Integer l : locations) { 
				double f = (double)l / (double)length;
				int pt = (int)Math.round(f * (double)units);
				points.add(pt);
			}
			size = units;
		}

		public int size() {
			return size;
		} 
	}
	
	public void addGeneRegions() {
		SigmaProperties sps = props.getSigmaProperties();
		GeneGenerator generator = sps.getGeneGenerator(strain);
		
		ChromInformationRenderer render = new DefaultPainterRenderer(Color.lightGray);
		
		for(String chr : chromViews.keySet()) { 
			ChromosomeView view = chromViews.get(chr);
			
			view.addMiddleInformation(new WGPainter(chr, '-', "genes"), render);
			view.addMiddleInformation(new WGPainter(chr, '+', "genes"), render);
		}
	}
	
	public void addGenes() {
		SigmaProperties sps = props.getSigmaProperties();
		GeneGenerator generator = sps.getGeneGenerator(strain);
		GenomeExpander<Gene> genomeGenerator = new GenomeExpander<Gene>(generator);
		
		ChromInformationRenderer render = new DefaultRegionSetRenderer(Color.lightGray);

		IteratorCacher<Gene> genes = new IteratorCacher<Gene>(genomeGenerator.execute(genome));

		StrandFilter<Gene> plus = new StrandFilter<Gene>('+');
		StrandFilter<Gene> minus = new StrandFilter<Gene>('-');

		for(String chr : chromViews.keySet()) { 
			ChromosomeView view = chromViews.get(chr);
			int len = genome.getChromLength(chr);
			
			//ChromRegions watsonGenes = new ChromRegionSet(chr, len, new FilterIterator<Gene,Gene>(plus, genes.iterator()));
			//ChromRegions crickGenes = new ChromRegionSet(chr, len, new FilterIterator<Gene,Gene>(minus, genes.iterator()));
			
			//view.addMiddleInformation(watsonGenes, render);
			//view.addMiddleInformation(crickGenes, render);
			
			ChromRegions allGenes = new ChromRegionSet(chr, len, genes.iterator());
			view.addMiddleInformation(allGenes, render);
		}
	}
	
	public void addShadedTrack(String key, Color c) { 
		//ChromInformationRenderer renderer = new ShadedScoreRenderer();
		ChromInformationRenderer wrenderer = new OneColorScoreRenderer(c, false);
		ChromInformationRenderer crenderer = new OneColorScoreRenderer(c, true);
		
		for(String chr : chromViews.keySet()) { 
			ChromosomeView view = chromViews.get(chr);
			WGScorer watsonInfo = new WGScorer(chr, '+', key);
			WGScorer crickInfo = new WGScorer(chr, '-', key);
			view.addUpperInformation(watsonInfo, wrenderer);
			view.addLowerInformation(crickInfo, crenderer);
		}		
	}
	
	public void addDivergent(String k1, String k2, int maxDist) { 
		Collection<Pair<StrandedRegion,StrandedRegion>> pairs = 
			whole.findPairs(k1, k2, maxDist, false, false);
		
		DefaultLocationSetRenderer render = new DefaultLocationSetRenderer(Color.red);
		
		for(String chr : chromViews.keySet()) { 
			ChromosomeView view = chromViews.get(chr);
			WGLocationSets locs = new WGLocationSets(pairs, chr, true, false);
			view.addMiddleInformation(locs, render);
		}
	}
	
	public void addConvergent(String k1, String k2, int maxDist) { 
		Collection<Pair<StrandedRegion,StrandedRegion>> pairs = 
			whole.findPairs(k1, k2, maxDist, true, false);
		
		DefaultLocationSetRenderer render = new DefaultLocationSetRenderer(Color.blue);
		
		for(String chr : chromViews.keySet()) { 
			ChromosomeView view = chromViews.get(chr);
			WGLocationSets locs = new WGLocationSets(pairs, chr, false, true);
			view.addMiddleInformation(locs, render);
		}
	}
	
	public void addKeyedTracks(String... keys) {
		for(int i = 0; i < keys.length; i++) { 
			try {
				whole.load(keys[i]);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		StackedPainterRenderer renderer = new StackedPainterRenderer();
		
		for(String chr : chromViews.keySet()) { 
			ChromosomeView view = chromViews.get(chr);
			WGMultiPainter watsonInfo = new WGMultiPainter(chr, '+', keys);
			WGMultiPainter crickInfo = new WGMultiPainter(chr, '-', keys);
			view.addUpperInformation(watsonInfo, renderer);
			view.addLowerInformation(crickInfo, renderer);
		}		
	}
	
	public void addTranscription(int channel, Color c) { 

		ChromInformationRenderer renderer = new OneColorScoreRenderer(c, false);
		
		for(String chr : chromViews.keySet()) { 
			ChromosomeView view = chromViews.get(chr);
			
			view.addUpperInformation(
					new SegmentationChromScorer(
							new StrandedRegion(view.getRegion(), '+'), 
							channel, 
							whole.getWatsonSegments()), 
					renderer);
			
			view.addLowerInformation(
					new SegmentationChromScorer(
							new StrandedRegion(view.getRegion(), '-'), 
							channel, 
							whole.getCrickSegments()), 
					renderer);
		}
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON);
		
		int w = x2-x1, h = y2-y1;
		
		g2.setColor(Color.white);
		g2.fillRect(x1, y1, w, h);
		
		int trackHeight = (int)Math.floor((double)h / (double)chromViews.size());

		Map<String,Rectangle> layout = layout(x1, y1, x2, y2-1);
		
		Font baseFont = g2.getFont();
		int fontSize = 8;
		Font font = new Font(baseFont.getFontName(),   
				baseFont.getStyle(), fontSize);
		g2.setFont(font);
		FontMetrics fm = g2.getFontMetrics();
		int fontHeight = fm.getAscent() + fm.getDescent();

		while(fontHeight < trackHeight / 10) { 
			fontSize = fontSize * 2; 
			font = new Font(font.getFontName(), font.getStyle(), fontSize);
			g2.setFont(font);
			fm = g2.getFontMetrics();
			fontHeight = fm.getAscent() + fm.getDescent();
		}
		System.out.println(String.format("font: %d -> %d", fontSize, fontHeight));
		
		for(String chr : layout.keySet()) { 
			Rectangle r = layout.get(chr);
			chromViews.get(chr).paintItem(g, r.x, r.y, r.x+r.width, r.y+r.height);
			
			g.setColor(Color.black);
			char[] chars = chr.toCharArray();
			int fontWidth = fm.charsWidth(chars, 0, chars.length);
			int ty = r.y + r.height/2 + fm.getDescent();
			int tx = Math.max(x1+2, r.x-fontWidth-2);
			g.drawString(chr, tx, ty);
		}
		
		g2.setFont(baseFont);
	}
	
	/**
	 * Lays out the chromosomes on the screen, so that they don't overlap, but
	 * so that we're not wasting too much space.
	 *  
	 * @return
	 */
	public Map<String,Rectangle> layout(int x1, int y1, int x2, int y2) { 
		LinkedHashMap<String,Rectangle> layoutMap = new LinkedHashMap<String,Rectangle>();
		TreeMap<Integer,LinkedList<String>> sortedKeys = new TreeMap<Integer,LinkedList<String>>();
		
		int maxLength = 1;
		int totalLength = 1;
		
		for(String k : chromViews.keySet()) { 
			int len = chromViews.get(k).getChromLength();
			
			maxLength = Math.max(maxLength, len);
			totalLength += len;
			
			if(!sortedKeys.containsKey(len)) { 
				sortedKeys.put(len, new LinkedList<String>());
			}
			sortedKeys.get(len).add(k);
		}
		
		int spacing = 20;
		int h = y2-y1, w = x2-x1;
		double aspect = (double)w / (double)h;
		
		int track = 1;
		int x = x1 + spacing, y = y1;

		for(Integer len : sortedKeys.keySet()) { 
			for(String chr : sortedKeys.get(len)) { 
				Rectangle r = null;
				int chromLength = chromViews.get(chr).getChromLength();
				double cf = (double)chromLength / (double)maxLength;
				int cwidth = (int)Math.floor(cf * (double)(w-spacing));

				if(x + cwidth > x2) { 
					track += 1;
					x = x1 + spacing;
				}
				
				cwidth = Math.min(cwidth, w - spacing*2);

				r = new Rectangle(x, track, cwidth, 1);
				x += cwidth;
				x += spacing;

				layoutMap.put(chr, r);
			}
		}

		int trackSpacing = 10;
		int trackHeight = (int)Math.floor((double)(h-(track-1)*trackSpacing) / (double)(track));
		
		for(String chr : layoutMap.keySet()) { 
			Rectangle r = layoutMap.get(chr);
			int ry = (r.y-1) * trackHeight + (r.y-1) * trackSpacing;
			r.y = ry;
			r.height = trackHeight;
		}
		
		return layoutMap;
	}
}

class FilteredExpander<X,Y,Z> implements Expander<X,Z> { 
	private Expander<X,Y> exp;
	private Filter <Y,Z> filter;
	
	public FilteredExpander(Filter<Y,Z> f, Expander<X,Y> e) { 
		exp = e;
		filter = f;
	}
	
	public Iterator<Z> execute(X value) { 
		return new FilterIterator<Y,Z>(filter, exp.execute(value));
	}
}
