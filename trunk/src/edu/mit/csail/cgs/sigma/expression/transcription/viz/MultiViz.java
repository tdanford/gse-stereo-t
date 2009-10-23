/*
 * Author: tdanford
 * Date: Feb 16, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;

import javax.swing.AbstractAction;
import javax.swing.Action;

import edu.mit.csail.cgs.cgstools.slicer.StudentsTTest;
import edu.mit.csail.cgs.cgstools.tgraphs.TaggedGraph;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.GeneGenerator;
import edu.mit.csail.cgs.sigma.GenomeModel;
import edu.mit.csail.cgs.sigma.alignments.*;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.sigma.expression.models.Transcript;
import edu.mit.csail.cgs.sigma.expression.segmentation.*;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.InputGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.viz.ModelSegmentValues;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptionParameters;
import edu.mit.csail.cgs.sigma.expression.transcription.identifiers.GreedyTranscriptIdentifier;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.sigma.litdata.miura.MiuraHit;
import edu.mit.csail.cgs.sigma.litdata.miura.MiuraHitExpander;
import edu.mit.csail.cgs.sigma.litdata.steinmetz.SteinmetzProbeGenerator;
import edu.mit.csail.cgs.sigma.litdata.steinmetz.SteinmetzProperties;
import edu.mit.csail.cgs.sigma.tgraphs.NewSigmaGraph;
import edu.mit.csail.cgs.sigma.tgraphs.SegmentGraph;
import edu.mit.csail.cgs.sigma.tgraphs.SegmentGraphViz;
import edu.mit.csail.cgs.sigma.tgraphs.SigmaGraph;
import edu.mit.csail.cgs.sigma.tgraphs.SigmaGraphViz;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.viz.NonOverlappingLayout;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.eye.*;
import edu.mit.csail.cgs.viz.paintable.*;

public class MultiViz extends AbstractRegionPaintable {

	public static class MultiVizProps extends Model { 
		
		public Boolean showFG, showBG;
		public Boolean showFGTrapezoids, showBGTrapezoids;
		public Boolean grayBackground;
		public Boolean showSegments, showBoundaries, showDips;
		public Boolean stemmed;
		public Boolean showTranscripts;
		
		public Boolean showSteinmetz, showMiura, showSequenceVariation;
		
		public MultiVizProps() { 
			showBG = true;
			showFG = true;
			
			showFGTrapezoids = false;
			showBGTrapezoids = false;
			
			grayBackground = true;
			showSegments = true;
			showBoundaries = false;
			showTranscripts = true;
			showDips = true;
			stemmed = false;
			
			showSequenceVariation = false;
			showSteinmetz = true;
			showMiura = false;
		}
	}
	
	public static void main(String[] args) { 
		regular_viz(args);
		//test_viz(args);
	}
	
	public static void regular_viz(String[] args) { 
		try {
			WorkflowProperties props = new WorkflowProperties();
			
			//String key = "s288c";
			String key = args.length > 0 ? args[0] : "txns288c";
			String expt = args.length > 1 ? args[1] : "matalpha";
			
			String strain = props.parseStrainFromKey(key);
			
			// mat-a, mat-alpha, diploid
			
			MultiViz viz = new MultiViz(props, key);

			Genome genome = props.getSigmaProperties().getGenome(strain);

			Region region = new Region(genome, "11", 0, 10000);
			viz.setRegion(region);

			//SigmaGraph segGraph = NewSigmaGraph.loadGraph(viz.whole, expt);
			//SigmaGraphViz Viz = new SigmaGraphViz(segGraph, region);
			
			int idx = -1;

			//GenericRegionFrame frame = new GenericRegionFrame(new StackedRegionPaintable(region, segGraphViz, viz));
			GenericRegionFrame frame = new GenericRegionFrame(new StackedRegionPaintable(region, viz));
			//GenericRegionFrame frame = new GenericRegionFrame(new StackedRegionPaintable(region, segGraphViz));

			idx = viz.addDataSegmentChannel(expt);

			File testtransw = new File(props.getDirectory(), "txns288c_plus.transcripts");
			if(testtransw.exists()) { 
				viz.dsChannels.get(idx).loadTranscripts(testtransw);
			}
			
			if(viz.mvProps.showSteinmetz) { 
				viz.addSteinmetzData();
			}
			
			if(viz.mvProps.showMiura) { 
				viz.addMiuraData();
			}
			
			//viz.dsChannels.get(idx).loadTranscripts(testtrans);
			
			frame.addActions(viz.collectCallTranscriptsActions());
			frame.showMe();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private WorkflowProperties props;
	private WorkflowIndexing indexing;
	private Map<String,WorkflowInputGenerator> generators;
	
	private ArrayList<DataSegmentViz> dsChannels;
	private int plusChannels, minusChannels;
	private String key, strain, oppStrain;
	private Genome genome;
	private WholeGenome whole;
	private boolean test;
	public MultiVizProps mvProps;
	
	private GeneViz genes;
	private IndelSNPViz indelSnps;
	private ArrayList<RegionKeyViz> regionsViz;
	private RegionKeyViz<MiuraHit> miura;
	private SteinmetzViz steinmetz;
	
	public MultiViz(WorkflowProperties ps, String key) throws IOException {
		props = ps;
		this.key = key;
		mvProps = new MultiVizProps();
		indexing = props.getIndexing(key);
		strain = props.parseStrainFromKey(key);
		oppStrain = indexing.getOppositeStrain(strain);
		genome = props.getSigmaProperties().getGenome(strain);
		whole = WholeGenome.loadWholeGenome(props, key);
		
		generators = new HashMap<String,WorkflowInputGenerator>();
		dsChannels = new ArrayList<DataSegmentViz>();
		plusChannels = minusChannels = 0;
		
		GeneGenerator gen = props.getSigmaProperties().getGeneGenerator(strain);
		genes = new GeneViz(gen);
		indelSnps = mvProps.showSequenceVariation ? new IndelSNPViz() : null;
		regionsViz = new ArrayList<RegionKeyViz>();
		steinmetz = null;
		miura = null;
		
		whole.loadIterators();
		//genes = null;
		test = false;
	}
	
	public MultiViz(WorkflowProperties ps, String key, File w, File c) throws IOException {
		props = ps;
		this.key = key; 
		mvProps = new MultiVizProps();
		strain = props.parseStrainFromKey(key);
		indexing = props.getIndexing(key);
		oppStrain = indexing.getOppositeStrain(strain);
		genome = props.getSigmaProperties().getGenome(strain);
		whole = WholeGenome.loadWholeGenome(props, key);
		
		generators = new HashMap<String,WorkflowInputGenerator>();
		dsChannels = new ArrayList<DataSegmentViz>();
		plusChannels = minusChannels = 0;
		
		GeneGenerator gen = props.getSigmaProperties().getGeneGenerator(strain);
		genes = new GeneViz(gen);
		indelSnps = null;
		steinmetz = null;
		miura = null;
		regionsViz = new ArrayList<RegionKeyViz>();
		
		whole.loadIterators(w, c);
		//genes = null;
		test = true;
	}

	public MultiVizProps getProps() { return mvProps; }

	public void setProps(MultiVizProps mvps) { 
		mvProps = mvps;
		
		if(mvProps.showSequenceVariation) { 
			try {
				indelSnps = new IndelSNPViz();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for(DataSegmentViz v : dsChannels) { 
			v.updateProps();
		}
		
		if(genes != null) { 
			genes.updateProps();
		}
		
		if(indelSnps != null) { 
			indelSnps.updateProps();
		}
		
		if(steinmetz != null) { 
			steinmetz.updateProps();
		}
		
		if(miura != null) { 
			miura.updateProps();
		}
		
		for(RegionKeyViz v : regionsViz) { 
			v.updateProps();
		}
	}

	private DataSegmentViz findFirstChannel(String str) { 
		for(DataSegmentViz v : dsChannels) { 
			if(v.strand.equals(str)) { 
				return v;
			}
		}
		return null;
	}
	
	public void update() { 
		String chrom = region.getChrom();
		int start = region.getStart(), end = region.getEnd();
		
		for(String key : generators.keySet()) { 
			String strand = strandFromKey(key);
			WorkflowInputGenerator generator = generators.get(key);
			generator.generate(chrom, start, end, strand);
		}
		
		for(DataSegmentViz v : dsChannels) { 
			v.update();
		}
		
		if(indelSnps != null) {
			indelSnps.update();
		}
		
		if(genes != null) { 
			genes.update();		
		}
		
		if(steinmetz != null) { 
			steinmetz.update();
		}
		
		if(miura != null) { 
			miura.update();
		}
		
		for(RegionKeyViz v : regionsViz) { 
			v.update();
		}
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		if(region == null) { return; }
		
		Graphics2D g2 = (Graphics2D)g;
		
		int h = y2 - y1, w = x2 - x1;
		int spacing = 5;
		int spacing2 = spacing*2;
		
		int gh = calculateGeneHeight(h);
		int geneHeight = genes != null ? gh : 0;
		int indelHeight = indelSnps != null ? gh : 0;
		int miuraHeight = miura != null ? gh*2 : 0;
		int regionsHeight = gh * regionsViz.size();
		
		int tracksHeight = Math.max(20, h - geneHeight - indelHeight - miuraHeight - regionsHeight);
		int numChannels = Math.max(1, dsChannels.size());
		if(steinmetz != null) { numChannels += 2; }
		
		int trackHeight = (int)Math.round((double)tracksHeight / (double)numChannels);
		
		int ty = y1;
		Rectangle geneRect = null;

		for(DataSegmentViz viz : dsChannels) { 
			if(viz.plusStrand()) { 
				viz.paintItem(g, x1, ty+spacing, x2, ty+trackHeight-spacing2);
				ty += trackHeight;
			}			
		}

		if(steinmetz != null) { 
			steinmetz.paintWatson(g, x1, ty+spacing, x2, ty+trackHeight-spacing2);
			ty += trackHeight;
		}

		if(miura != null) { 
			miura.paintWatson(g, x1, ty, x2, ty+gh);
			ty += gh;			
		}
		
		geneRect = new Rectangle(x1, ty, x2-x1, geneHeight);
		
		if(genes != null) { 
			genes.paintItem(g, x1, ty, x2, ty+geneHeight);
			ty += geneHeight;
		}
		
		if(indelSnps != null) { 
			indelSnps.paintItem(g, x1, ty, x2, ty+indelHeight);
			ty += indelHeight;			
		}
		
		if(miura != null) { 
			miura.paintCrick(g, x1, ty, x2, ty+gh);
			ty += gh;			
		}
		
		if(steinmetz != null) { 
			steinmetz.paintCrick(g, x1, ty+spacing, x2, ty+trackHeight-spacing2);
			ty += trackHeight;
		}
		
		for(RegionKeyViz v : regionsViz) { 
			v.paintItem(g, x1, ty, x2, ty+indelHeight);
			ty += geneHeight;						
		}

		for(DataSegmentViz viz : dsChannels) { 
			if(!viz.plusStrand()) { 
				viz.paintItem(g, x1, ty+spacing, x2, ty+trackHeight-spacing2);
				ty += trackHeight;
			}			
		}	
		
		if(geneRect != null) { 
			DataSegmentViz viz = findFirstChannel("+");
			if(viz != null) { 
				for(Paintable p : viz.badges.keySet()) { 
					Rectangle r = viz.badges.get(p);
					r.y = geneRect.y;
					r.height = geneRect.height/2;
					p.paintItem(g, r.x, r.y, r.x + r.width, r.y + r.height);
				}
			}
			
			viz = findFirstChannel("-");
			if(viz != null) { 
				for(Paintable p : viz.badges.keySet()) { 
					Rectangle r = viz.badges.get(p);
					r.height = geneRect.height/2;
					r.y = geneRect.y + r.height;
					p.paintItem(g, r.x, r.y, r.x + r.width, r.y + r.height);
				}
			}
		}
	}
	
	public int addDataSegmentChannel(String exptName, boolean strand, Integer[] fg, Integer[] bg) { 
		return addDataSegmentChannel(exptName, strand, new DataSegmentExpander(genome, 
				strand ? whole.getWatsonSegments() : whole.getCrickSegments()), fg, bg);
	}

	public int addDataSegmentChannel(String expt) {
		return addDataSegmentChannel(strain, expt, oppStrain, expt);
	}
	
	public int addDataSegmentChannel(String exptName, Integer[] fg, Integer[] bg) { 
		int i1 = addDataSegmentChannel(exptName, true, fg, bg);
		
		int i2 = addDataSegmentChannel(exptName, false, fg, bg);
		
		dsChannels.get(i1).synchronizeScales(dsChannels.get(i2));
		
		return i1;		
	}
	
	public int addDataSegmentChannel(String strain1, String expt1, String strain2, String expt2) { 
		int i1 = addDataSegmentChannel(expt1, true, 
				indexing.findChannels(strain1, expt1),
				indexing.findChannels(strain2, expt2));
		
		try {
			if(!test) { 
				dsChannels.get(i1).loadTranscripts(expt1, true);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		int i2 = addDataSegmentChannel(expt2, false, 
				indexing.findChannels(strain1, expt1),
				indexing.findChannels(strain2, expt2));
		
		dsChannels.get(i1).synchronizeScales(dsChannels.get(i2));
		
		try {
			if(!test) { 
				dsChannels.get(i2).loadTranscripts(expt1, false);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return i1;
	}

	public void addSteinmetzData() { 
		steinmetz = new SteinmetzViz();
		dispatchChangedEvent();
	}
	
	public void addMiuraData() { 
		try {
			miura = new RegionKeyViz<MiuraHit>(new MiuraHitExpander(props));
			dispatchChangedEvent();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int addDataSegmentChannel(String exptName, 
			boolean strand, Expander<Region,DataSegment> exp, Integer[] fg, Integer[] bg) { 
		DataSegmentViz viz = new DataSegmentViz(exptName, exp, strand ? "+" : "-", fg, bg);
		dsChannels.add(viz);
		if(strand) { 
			plusChannels += 1;
		} else { 
			minusChannels += 1;
		}
		update();
		dispatchChangedEvent();
		return dsChannels.size()-1;
	}
	
	public void addRegionViz(Expander<Region,? extends RegionKey> exp) { 
		regionsViz.add(new RegionKeyViz(exp));
		dispatchChangedEvent();
	}
	
	public Collection<Action> collectCallTranscriptsActions() { 
		ArrayList<Action> acts = new ArrayList<Action>();
		for(int i = 0; i < dsChannels.size(); i++) { 
			acts.add(new TranscriptFindingAction(i));
		}
		return acts;
	}
	
	private class TranscriptFindingAction extends AbstractAction {
		private int index;
		public TranscriptFindingAction(int i) {
			super(String.format("#%d Transcript-Calling", i));
			index = i;
		}
		public void actionPerformed(ActionEvent e) {
			dsChannels.get(index).findTranscripts();
			MultiViz.this.dispatchChangedEvent();
		}
	}
	
	public static String strandFromKey(String k) {  
		return k.endsWith("negative") ? "-" : "+";
	}
	
	public void addGenerator(WorkflowInputGenerator gen) { 
		addGenerator(gen.getKey(), gen);
	}
	
	public void addGenerator(String k, WorkflowInputGenerator gen) { 
		if(!generators.containsKey(k)) { 
			generators.put(k, gen);
		} else { 
			throw new IllegalArgumentException(k);
		}
	}
	
	private int calculateGeneHeight(int h) { 
		return Math.max(40, h/10);
	}
	
	private void paintDiffs(Graphics2D g2, int x1, int y1, int x2, int y2, Collection<Integer[]> diffs, int track) { 
		int w = x2-x1, h = y2-y1;
		Stroke oldStroke = g2.getStroke();
		g2.setStroke(new BasicStroke((float)2.0));
		
		// Paints diffs. 
		g2.setColor(Color.red);

		int y = y2-(track+1)*4;
		
		for(Integer[] b : diffs) {
			double f1 = (double)(b[0] - region.getStart()) / (double)region.getWidth();
			double f2 = (double)(b[1] - region.getStart()) / (double)region.getWidth();
			int leftx = x1 + (int)Math.round(f1 * (double)w);
			int rightx = x1 + (int)Math.round(f2 * (double)w);
			g2.drawLine(leftx, y, rightx, y);
		}
		
		g2.setStroke(oldStroke);
	}

	private class GeneViz extends AbstractPaintable {

		private Expander<Region,Gene> geneExpander;
		
		public GeneViz(Expander<Region,Gene> ge) { 
			geneExpander = ge;
		}
		
		public void updateProps() {
		}

		public void update() { 
			// don't need to do anything here. 
		}
		
		public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
			Iterator<Gene> genes = geneExpander.execute(region);
			NonOverlappingLayout<Gene> layout = new NonOverlappingLayout<Gene>();
			layout.setRegions(genes);
			Collection<Gene> laidOutGenes = layout.getRegions();
			
			ArrowPainter painter = new ArrowPainter();
			painter.setFillColor(Color.white);
			
			int h = y2 - y1, w = x2 - x1;
			int tracks = layout.getNumTracks();
			int trackHeight = (int)Math.round((double)h / (double)Math.max(1, tracks));
			
			for(Gene gene : laidOutGenes) { 
				int geneStart = gene.getStart(), geneEnd = gene.getEnd();
				double startFrac = (double)(geneStart - region.getStart()) / (double)region.getWidth();
				double endFrac = (double)(geneEnd - region.getStart()) / (double)region.getWidth();
				int geneX1 = x1 + (int)Math.round(startFrac * (double)w);
				int geneX2 = x1 + (int)Math.round(endFrac * (double)w);
				int track = layout.getTrack(gene);
				int geneY1 = y1 + track * trackHeight;
				int geneY2 = geneY1 + trackHeight;
				
				if(gene.getStrand() == '-') { 
					painter.setDirection(false);
				} else { 
					painter.setDirection(true);
				}
				painter.setLabel(gene.getName());
				painter.paintItem(g, geneX1, geneY1, geneX2, geneY2);
			}
		} 
	}

	private class IndelSNPViz<K extends RegionKey> extends AbstractPaintable {
		
		private Expander<Region,Insertion> insertionExpander;
		private Expander<Region,Deletion> deletionExpander;
		private Expander<Region,SNP> snpExpander;
		private String strainKey;

		public IndelSNPViz() throws IOException {
			strainKey = strain.equals("s288c") ? "S288C" : "S1278b";
			IndelParser indels = new IndelParser(props);
			SNPParser snps = new SNPParser(props);
			insertionExpander = indels.insertionOverlapExpander(strainKey);
			deletionExpander = indels.deletionOverlapExpander(strainKey);
			snpExpander = snps.snpExpander(strainKey);
		}
		
		public void updateProps() {
		}

		public void update() { 
			// don't need to do anything here. 
		}
		
		public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
			Iterator<SNP> snps = snpExpander.execute(region);
			Iterator<Insertion> inserts = insertionExpander.execute(region);
			Iterator<Deletion> deletions = deletionExpander.execute(region);
			
			int h = y2 - y1, w = x2 - x1;
			int my = y1 + h/2;
			
			g.setColor(Color.black);
			g.drawLine(x1, my, x2, my);

			while(snps.hasNext()) { 
				SNP snp = snps.next();
				int loc = snp.location;
				double locf = (double)(loc - region.getStart()) / (double)region.getWidth();
				int snpx = x1 + (int)Math.round(locf * (double)w);
				g.setColor(Color.black); 
				g.drawLine(snpx, my-5, snpx, my+5);
			}

			while(deletions.hasNext()) { 
				Deletion del = deletions.next();
				int loc = del.location;
				double locf = (double)(loc - region.getStart()) / (double)region.getWidth();
				int delx = x1 + (int)Math.round(locf * (double)w);
				
				g.setColor(Color.black); 
				g.drawLine(delx, my, delx-5, my-5);
				g.drawLine(delx, my, delx+5, my-5);
				g.drawLine(delx-5, my-5, delx+5, my-5);
				
				g.drawString(String.format("%d", del.size), delx, my-8);
			}

			while(inserts.hasNext()) { 
				Insertion insert = inserts.next();
				int istart = insert.start, iend = insert.end;
				double fstart = (double)(istart - region.getStart()) / (double)region.getWidth();
				double fend = (double)(iend - region.getStart()) / (double)region.getWidth();
				int xstart = x1 + (int)Math.round(fstart * (double)w);
				int xend = x1 + (int)Math.round(fend * (double)w);
				
				g.setColor(Color.gray); 
				g.fillRect(xstart, my-5, xend-xstart, 10);
				g.setColor(Color.black); 
				g.drawRect(xstart, my-5, xend-xstart, 10);
			}

		} 
	}
	
	private class RegionKeyViz<K extends RegionKey> extends AbstractPaintable {

		private Expander<Region,K> expander;
		
		private ArrayList<K> watson, crick;
		
		public RegionKeyViz(Expander<Region,K> ge) { 
			expander = ge;
			watson = new ArrayList<K>();
			crick = new ArrayList<K>();
		}
		
		public void update() { 
			watson.clear();
			crick.clear();

			Iterator<K> itr = expander.execute(region);
			while(itr.hasNext()) { 
				K reg = itr.next();
				if(reg.strand.equals("+")) { 
					watson.add(reg);
				} else { 
					crick.add(reg);
				}
			}
		}

		public void updateProps() {
		}
		
		public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
			paintRegions(g, x1, y1, x2, y2, new SerialIterator<K>(watson.iterator(), crick.iterator()));
		}

		public void paintWatson(Graphics g, int x1, int y1, int x2, int y2) {
			paintRegions(g, x1, y1, x2, y2, watson.iterator());
		}

		public void paintCrick(Graphics g, int x1, int y1, int x2, int y2) {
			paintRegions(g, x1, y1, x2, y2, crick.iterator());
		}

		public void paintRegions(Graphics g, int x1, int y1, int x2, int y2, Iterator<K> regs) {
			Iterator<StrandedRegion> regions = 
				new MapperIterator<K,StrandedRegion>(new RegionKeyToRegionMapper<K>(), 
						regs);
			
			NonOverlappingLayout<StrandedRegion> layout = new NonOverlappingLayout<StrandedRegion>();
			layout.setRegions(regions);
			Collection<StrandedRegion> laidOutRegions = layout.getRegions();
			
			ArrowPainter painter = new ArrowPainter();
			painter.setFillColor(Color.white);
			
			int h = y2 - y1, w = x2 - x1;
			int tracks = layout.getNumTracks();
			int trackHeight = Math.max(1, (int)Math.round((double)h / (double)Math.max(1, tracks)));
			
			for(StrandedRegion reg : laidOutRegions) { 
				int rStart = reg.getStart(), rEnd = reg.getEnd();
				double startFrac = (double)(rStart - region.getStart()) / (double)region.getWidth();
				double endFrac = (double)(rEnd - region.getStart()) / (double)region.getWidth();
				int rX1 = x1 + (int)Math.round(startFrac * (double)w);
				int rX2 = x1 + (int)Math.round(endFrac * (double)w);
				int track = layout.getTrack(reg);
				int rY1 = y1 + track * trackHeight;
				int rY2 = rY1 + trackHeight;
				
				g.drawRect(rX1, rY1, rX2-rX1, rY2-rY1);
				
				int rh = rY2-rY1;
				int mx = (rX1 + rX2) / 2;
				int my = (rY1 + rY2) / 2;
				if(reg.getStrand() == '+') { 
					int px = Math.min(mx+rh/2, rX2);
					g.drawLine(mx, rY1, px, my);
					g.drawLine(px, my, mx, rY2);
				} else { 
					int px = Math.max(mx-rh/2, rX1);
					g.drawLine(mx, rY1, px, my);
					g.drawLine(px, my, mx, rY2);					
				}
			}
		} 
	}
	
	private class RegionKeyToRegionMapper<K extends RegionKey> implements Mapper<K,StrandedRegion> {
		public StrandedRegion execute(K a) {
			return new StrandedRegion(genome, a.chrom, a.start, a.end, a.strand.charAt(0));
		} 
	}

	public static Color[] trackColors = new Color[] { 
		Color.red, Coloring.darken(Color.green), Color.blue };
	
	private class SteinmetzViz extends AbstractPaintable {
		
		private SteinmetzProbeGenerator generator;
		private ModelLocatedValues watsonValues, crickValues;
		
		public SteinmetzViz() { 
			this(new SteinmetzProperties());
		}
		
		public void updateProps() {
		}

		public SteinmetzViz(SteinmetzProperties props) { 
			this(new SteinmetzProbeGenerator(props));
		}
		
		public SteinmetzViz(SteinmetzProbeGenerator gen) {
			generator = gen;
			watsonValues = new ModelLocatedValues();
			crickValues = new ModelLocatedValues();
			
			watsonValues.synchronizeProperty(ModelLocatedValues.scaleKey, crickValues);
			watsonValues.setProperty(ModelLocatedValues.stemKey, false);
			crickValues.setProperty(ModelLocatedValues.stemKey, false);

			watsonValues.setProperty(ModelLocatedValues.radiusKey, 1);
			crickValues.setProperty(ModelLocatedValues.radiusKey, 1);

			watsonValues.setProperty(ModelLocatedValues.colorKey, Coloring.clearer(Color.red));
			crickValues.setProperty(ModelLocatedValues.colorKey, Coloring.clearer(Color.blue));
		}
		
		public void update() { 
			watsonValues.clearModels();
			crickValues.clearModels();
			
 			Iterator<ExpressionProbe> probes = generator.execute(region);
			Color watsonColor = Color.red;
			Color crickColor = Color.blue;

			while(probes.hasNext()) { 
				ExpressionProbe probe = probes.next();
				int loc = probe.getLocation();
				double value = probe.mean();
				char strand = probe.getStrand();
				
				if(strand == '+') { 
					watsonValues.addModel(new LocatedValueModel(loc, value, watsonColor));
				} else { 
					crickValues.addModel(new LocatedValueModel(loc, value, crickColor));					
				}
			}
			
			watsonValues.setBounds(region.getStart(), region.getEnd());
			crickValues.setBounds(region.getStart(), region.getEnd());
		}
		
		public void paintWatson(Graphics g, int x1, int y1, int x2, int y2) {
			int w = x2-x1, h = y2-y1;
			PaintableScale scale = watsonValues.getPropertyValue(ModelLocatedValues.scaleKey);
			VerticalScalePainter vsp = new VerticalScalePainter(scale);
			watsonValues.paintItem(g, x1, y1, x2, y2);			
			vsp.paintItem(g, x1, y1, x2, y2);
		}

		public void paintCrick(Graphics g, int x1, int y1, int x2, int y2) {
			int w = x2-x1, h = y2-y1;
			PaintableScale scale = crickValues.getPropertyValue(ModelLocatedValues.scaleKey);
			VerticalScalePainter vsp = new VerticalScalePainter(scale);
			crickValues.paintItem(g, x1, y1, x2, y2);
			vsp.paintItem(g, x1, y1, x2, y2);
		}

		public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
			int w = x2-x1, h = y2-y1;
			int h2 = h/2;
			watsonValues.paintItem(g, x1, y1, x2, y1+h2);
			crickValues.paintItem(g, x1, y1+h2, x2, y2);
		}  
	}
	
	public class DataSegmentViz extends AbstractPaintable {

		private String expt;
		private Integer[] fg, bg;
		private ModelLocatedValues fgValues, bgValues;
		private ModelLocatedDifferenceValues diffValues; 
		
		private ArrayList<Integer> boundaries;
		private ArrayList<Transcript> transcripts;
		
		private ModelSegmentValues[] fgSegmentValues, bgSegmentValues;
		private ModelSegmentValues[] fgTranscriptValues, bgTranscriptValues;
		private String strand;
		
		private Expander<Region,DataSegment> data;
		private DataSegment[] segs;
		private Map<Paintable,Rectangle> badges;
		
		private PaintableScale scale;
		
		public DataSegmentViz(String e, Expander<Region,DataSegment> exp, String str, Integer[] fg, Integer[] bg) {
			expt = e;
			data = exp;
			this.fg = fg.clone();
			this.bg = bg.clone();
			
			System.out.println(String.format("MultiViz$DataSegmentViz: %s", str));
			System.out.print("\tfg:");
			for(int i = 0; i < fg.length; i++) { 
				System.out.print(" " + fg[i]);
			}
			System.out.println();
			System.out.print("\tbg:");
			for(int i = 0; i < bg.length; i++) { 
				System.out.print(" " + bg[i]);
			}
			System.out.println();
			
			fgValues = new ModelLocatedValues();
			bgValues = new ModelLocatedValues();
			scale = new PaintableScale(0.0, 1.0);

			fgValues.setProperty(ModelLocatedValues.stemKey, mvProps.stemmed);
			fgValues.setProperty(ModelLocatedValues.colorKey, trackColors[0]);
			bgValues.setProperty(ModelLocatedValues.stemKey, false);
			bgValues.setProperty(ModelLocatedValues.colorKey, trackColors[1]);
			fgValues.synchronizeProperty(ModelSegmentValues.scaleKey, bgValues);

			diffValues = new ModelLocatedDifferenceValues();
			diffValues.setProperty(ModelLocatedDifferenceValues.strokeKey, (float)1.0);
			diffValues.setProperty(ModelLocatedDifferenceValues.radiusKey, 3);
			fgValues.synchronizeProperty(ModelLocatedDifferenceValues.scaleKey, diffValues);
			
			fgSegmentValues = new ModelSegmentValues[fg.length];
			bgSegmentValues = new ModelSegmentValues[bg.length];
			fgTranscriptValues = new ModelSegmentValues[fg.length];
			bgTranscriptValues = new ModelSegmentValues[bg.length];
			
			for(int i = 0; i < fg.length; i++) { 
				fgSegmentValues[i] = new ModelSegmentValues();
				fgSegmentValues[i].setSegmentColor(Color.pink);
				fgSegmentValues[i].setTypeFieldName("segmentType");
				fgSegmentValues[i].setProperty(ModelSegmentValues.drawTrapezoids, mvProps.showFGTrapezoids);
				fgValues.synchronizeProperty(ModelSegmentValues.scaleKey, fgSegmentValues[i]);

				fgTranscriptValues[i] = new ModelSegmentValues();
				fgTranscriptValues[i].setSegmentColor(Color.pink);
				fgTranscriptValues[i].setTypeFieldName("segmentType");
				fgTranscriptValues[i].setProperty(ModelSegmentValues.drawTrapezoids, mvProps.showFGTrapezoids);
				//fgValues.synchronizeProperty(ModelSegmentValues.scaleKey, fgTranscriptValues[i]);
			}
			for(int i = 0; i < bg.length; i++) { 
				bgSegmentValues[i] = new ModelSegmentValues();
				bgSegmentValues[i].setSegmentColor(Color.green);
				bgSegmentValues[i].setTypeFieldName("segmentType");
				bgSegmentValues[i].setProperty(ModelSegmentValues.drawTrapezoids, mvProps.showBGTrapezoids);
				fgValues.synchronizeProperty(ModelSegmentValues.scaleKey, bgSegmentValues[i]);

				bgTranscriptValues[i] = new ModelSegmentValues();
				bgTranscriptValues[i].setSegmentColor(Color.green);
				bgTranscriptValues[i].setTypeFieldName("segmentType");
				bgTranscriptValues[i].setProperty(ModelSegmentValues.drawTrapezoids, mvProps.showBGTrapezoids);
				//fgValues.synchronizeProperty(ModelSegmentValues.scaleKey, bgTranscriptValues[i]);
			}
			
			strand = str;
			boundaries = new ArrayList<Integer>();
			segs = null;
			badges = new HashMap<Paintable,Rectangle>();
			transcripts = new ArrayList<Transcript>();
		}
		
		public void updateProps() {
			fgValues.setProperty(ModelLocatedValues.stemKey, mvProps.stemmed);
			for(int i = 0; i < fg.length; i++) { 
				fgSegmentValues[i].setProperty(ModelSegmentValues.drawTrapezoids, mvProps.showFGTrapezoids);				
				fgTranscriptValues[i].setProperty(ModelSegmentValues.drawTrapezoids, mvProps.showFGTrapezoids);				
			}
			for(int i = 0; i < bg.length; i++) { 
				bgSegmentValues[i].setProperty(ModelSegmentValues.drawTrapezoids, mvProps.showBGTrapezoids);				
				bgTranscriptValues[i].setProperty(ModelSegmentValues.drawTrapezoids, mvProps.showBGTrapezoids);				
			}
			dispatchChangedEvent();
		}

		public void synchronizeScales(DataSegmentViz v) { 
			fgValues.synchronizeProperty(ModelSegmentValues.scaleKey, v.diffValues);
			fgValues.synchronizeProperty(ModelSegmentValues.scaleKey, v.fgValues);
			fgValues.synchronizeProperty(ModelSegmentValues.scaleKey, v.bgValues);
			for(int i = 0; i < v.fg.length; i++) { 
				fgValues.synchronizeProperty(ModelSegmentValues.scaleKey, v.fgSegmentValues[i]);
				fgValues.synchronizeProperty(ModelSegmentValues.scaleKey, v.fgTranscriptValues[i]);
			}
			for(int i = 0; i < v.bg.length; i++) { 
				fgValues.synchronizeProperty(ModelSegmentValues.scaleKey, v.bgSegmentValues[i]);
				fgValues.synchronizeProperty(ModelSegmentValues.scaleKey, v.bgTranscriptValues[i]);
			}
		}
		
		public void loadTranscripts(String expt, boolean strand) throws IOException {
			String fplusname = String.format("%s_plus_%s.transcripts", key, expt);
			String fminusname = String.format("%s_negative_%s.transcripts", key, expt);
			String fname = strand ? fplusname : fminusname;
			File f = new File(props.getDirectory(), fname);
			if(f.exists()) { loadTranscripts(f); } 
		}
		
		public void loadTranscripts(File... fs) throws IOException {
			for(File f : fs) { 
				WorkflowTranscriptReader reader = new WorkflowTranscriptReader(f);
				addTranscriptCalls(reader);
				reader.close();
			}
		}
		
		public Iterator<DataSegment> getDataSegments() { 
			LinkedList<DataSegment> seglist = new LinkedList<DataSegment>();
			for(int i = 0; i < segs.length; i++) { 
				seglist.add(segs[i]);
			}
			return seglist.iterator();
		}
		
		public void addTranscriptCalls(Iterator<TranscriptCall> calls) { 
			while(calls.hasNext()) {
				TranscriptCall call = calls.next();
				Transcript t = new Transcript(genome, 
						call.chrom, call.start, call.end, call.strand.charAt(0), 
						call.intensities );
				transcripts.add(t);
				
				Double[] pms = new Double[] { call.intensities[0], call.falloff };
				int tsStart = call.start;
				int tsEnd = call.end;
				int chIdx = 0;
				int ch = fg[chIdx];
				Segment ts = new Segment(ch, false, Segment.LINE, tsStart, tsEnd, pms);
				fgTranscriptValues[chIdx].addModel(ts);
			}			
			dispatchChangedEvent();
		}
		
		public void findTranscripts() {
			WorkflowProperties props = new WorkflowProperties();
			String[] args = new String[] {};
			Workflow worker = new Workflow(props, args);
			TranscriptionParameters tparams = new TranscriptionParameters();

			Iterator<DataSegment> segitr = getDataSegments();
			Iterator<TranscriptCall> calls = 
				worker.completeCalling(segitr, key, tparams, expt);

			transcripts.clear();
			addTranscriptCalls(calls);
		}
		
		public boolean plusStrand() { 
			return strand.equals("+");
		}
		
		public void clearModels() { 
			fgValues.clearModels();
			bgValues.clearModels();
			for(int i = 0; i < bg.length; i++) { 
				bgTranscriptValues[i].clearModels();
				bgSegmentValues[i].clearModels();
			}
			for(int i = 0; i < fg.length; i++) { 
				fgSegmentValues[i].clearModels();
				fgTranscriptValues[i].clearModels();
			}
			boundaries.clear();
			segs = null;
			badges.clear();
			//transcripts.clear();
		}
		
		public void update() { 
			clearModels();
			
			if(region != null) {
				Iterator<DataSegment> dsegs = data.execute(region);
				ArrayList<DataSegment> seglist = new ArrayList<DataSegment>();
				while(dsegs.hasNext()) { seglist.add(dsegs.next()); }

				segs = seglist.toArray(new DataSegment[0]);
				Arrays.sort(segs);
				
				for(int k = 0; k < segs.length; k++) { 
					DataSegment dseg = segs[k];

					if(k > 0) { 
						int s= dseg.start, e = segs[k-1].end;
						int m = (s+e)/2;
						boundaries.add(m);
						
					}

					Integer[] locs = dseg.dataLocations;
					Double[][] values = dseg.dataValues;
			
					for(int j = 0; j < fg.length; j++) { 
						int channel = fg[j];

						Segment channelSegment = new Segment(dseg.getSegment(channel));
						channelSegment.shared = dseg.isDifferential(expt);

						for(int i = 0; i < locs.length; i++) { 
							double value = values[channel][i];
							int loc = locs[i];
							double predicted = dseg.predicted(channel, loc);

							Color c = Color.red;
							if(mvProps.grayBackground && channelSegment.segmentType.equals(Segment.FLAT)) { 
								c = Coloring.clearer(Coloring.clearer(c));
							}
							
							fgValues.addModel(new LocatedValueModel(loc, value, c));
							//rawValues.get(idx).addModel(new LocatedValueModel(loc, predicted));

							/*
							if(j == 0) { 
								double v1 = values[fg[0]][i];
								double v2 = values[bg[0]][i];

								diffValues.addModel(new DiffValueModel(loc, v1, v2));
							}
							*/
						}

						/*
						if(fg[0].equals(channel)) { 
							fgSegmentValues[0].addModel(channelSegment);
						}
						*/

						if(dseg.hasConsistentType(Segment.LINE, fg)) { 
							fgSegmentValues[j].addModel(channelSegment);
						}
					}
					
					for(int j = 0; j < bg.length; j++) { 
						int channel = bg[j];
						
						Segment channelSegment = new Segment(dseg.getSegment(channel));
						channelSegment.shared = false;

						for(int i = 0; i < locs.length; i++) { 
							double value = values[channel][i];
							int loc = locs[i];
							double predicted = dseg.predicted(channel, loc);
							Color c = Coloring.darken(Color.green);
							if(mvProps.grayBackground && channelSegment.segmentType.equals(Segment.FLAT)) { 
								c = Coloring.clearer(Coloring.clearer(c));
							}
							bgValues.addModel(new LocatedValueModel(loc, value, c));
						}
						
						if(dseg.hasConsistentType(Segment.LINE, bg)) { 
							bgSegmentValues[j].addModel(channelSegment);
						}
					}
					
				}

				for(int j = 0; j < fg.length; j++) { 
					fgSegmentValues[j].setBounds(region.getStart(), region.getEnd());
					fgTranscriptValues[j].setBounds(region.getStart(), region.getEnd());
				}
				for(int j = 0; j < bg.length; j++) { 
					bgSegmentValues[j].setBounds(region.getStart(), region.getEnd());
					bgTranscriptValues[j].setBounds(region.getStart(), region.getEnd());
				}
				diffValues.setBounds(region.getStart(), region.getEnd());
				fgValues.setBounds(region.getStart(), region.getEnd());
				bgValues.setBounds(region.getStart(), region.getEnd());
			}
		}

		public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
			
			Graphics2D g2 = (Graphics2D)g;
			
			PaintableScale scale = fgValues.getPropertyValue(ModelLocatedValues.scaleKey);
			scale.setScale(0.0, scale.getMax());
			
			if(mvProps.showBG) { 
				bgValues.paintItem(g, x1, y1, x2, y2);
			}
			fgValues.paintItem(g, x1, y1, x2, y2);
			
			VerticalScalePainter vsp = new VerticalScalePainter(scale);
			vsp.paintItem(g, x1, y1, x2, y2);

			int w = x2-x1, h= y2-y1;
			if(mvProps.showSegments) { 
				for(int i = 0; i < fg.length; i++) { 
					fgSegmentValues[i].paintItem(g, x1, y1, x2, y2);
				}
				for(int i = 0; i < bg.length; i++) { 
					bgSegmentValues[i].paintItem(g, x1, y1, x2, y2);
				}
			}
			
			if(mvProps.showTranscripts) { 
				for(int i = 0; i < fg.length; i++) { 
					//fgTranscriptValues[i].paintItem(g, x1, y1, x2, y2);
				}
				for(int i = 0; i < bg.length; i++) { 
					//bgTranscriptValues[i].paintItem(g, x1, y1, x2, y2);
				}				
			}
			//diffValues.paintItem(g, x1, y1, x2, y2);
			
			badges.clear();
			for(int i = 0; i < segs.length; i++) { 
				int s = segs[i].dataLocations[0], e = segs[i].dataLocations[segs[i].dataLocations.length-1];
				double sf = (double)(s - region.getStart()) / (double)region.getWidth();
				double ef = (double)(e - region.getStart()) / (double)region.getWidth();
				
				int rx1 = x1 + (int)Math.round(sf * (double)w);
				int rx2 = x1 + (int)Math.round(ef * (double)w);
				int ry1 = y2 - 10;
				int ry2 = y2;
				
				Rectangle r = new Rectangle(rx1, ry1, rx2-rx1, ry2-ry1);
				//badges.put(new DifferentialKeyPaintable(segs[i].differential), r);

				/*
				if(i < segs.length-1) { 
					double ssf = (double)(segs[i+1].start - region.getStart()) / (double)region.getWidth();
					int srx1 = x1 + (int)Math.round(sf * (double)w);
					Rectangle cr = new Rectangle(rx1, ry1, srx1-rx1, ry2-ry1);

					if(segs[i].isContinuous(segs[i+1], fg[0])) { 
						badges.put(new ConnectorPaintable(Color.blue), cr);
					} else { 
						badges.put(new ConnectorPaintable(Color.red), cr);
					}
				}
				*/
			}
			
			NonOverlappingLayout layout = new NonOverlappingLayout();
			Iterator<Transcript> internal = 
				new FilterIterator<Transcript,Transcript>(new Filter<Transcript,Transcript>() { 
					public Transcript execute(Transcript t) { 
						return region.overlaps(t) ? t : null; 
					}
				}, transcripts.iterator());
			ArrayList<Transcript> overlappingTranscripts = new ArrayList<Transcript>();
			while(internal.hasNext()) { overlappingTranscripts.add(internal.next()); }
			layout.setRegions(overlappingTranscripts);
			
			int numTracks= layout.getNumTracks();
			ArrowPainter arrow = new ArrowPainter();
			
			int transcriptStrip = h/3;
			int transTrackHeight = Math.max(10, (int)Math.round((double)transcriptStrip / (double)Math.max(1, numTracks)));
			
			for(Transcript t : overlappingTranscripts) {
				int track = layout.getTrack(t);
				int s = t.getStart(), e = t.getEnd();
				double sf = (double)(s - region.getStart()) / (double)region.getWidth();
				double ef = (double)(e - region.getStart()) / (double)region.getWidth();

				int rx1 = x1 + (int)Math.floor(sf * (double)w);
				int rx2 = x1 + (int)Math.floor(ef * (double)w);
				int ry1 = y2 - transTrackHeight*(track+1);
				int ry2 = ry1 + transTrackHeight;

				/*
				System.out.println(String.format(
						"-> T: %d,%d (%d) => %.2f:%.2f %d,%d %d,%d",
						s, e, track, sf, ef, rx1, rx2, ry1, ry2));
				*/

				int twidth = rx2-rx1, theight = ry2-ry1;
				arrow.setDirection(t.getStrand() == '+');
				if(mvProps.showTranscripts) { 
					arrow.paintItem(g, rx1, ry1, rx1+twidth, ry1+theight);
				}
			}
			
			//System.out.println();
			
			g.setColor(Color.orange);
			if(mvProps.showDips && fg[0] != null && fg[0] >= 0) { 
				for(int i = 0; i < segs.length; i++) { 
					if(segs[i].hasFivePrimeDip(fg[0])) {
						int loc = segs[i].strand.equals("+") ? segs[i].start : segs[i].end;
						double locf = (double)(loc - region.getStart()) / (double)region.getWidth();
						int x = x1 + (int)Math.round(locf * (double)w);
						int[] xs = new int[] { x, x-5, x+5 };
						int[] ys = new int[] { y2-5, y2, y2 };
						g2.fillPolygon(xs, ys, 3);
						g2.drawLine(x, y1, x, y2);
					}
				}
				System.out.println();
			} else { 
				System.out.println(String.format("No DIPS shown: %s", String.valueOf(fg[0])));
			}
		}
	}
	
	public static class ConnectorPaintable extends AbstractPaintable {
		
		private Color color;
		
		public ConnectorPaintable(Color c) { 
			color = c;
		}

		public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
			g.setColor(color);
			int w = x2 - x1, h = y2-y1;
			g.drawOval(x1, y1, w, h);
		} 
	}

	public static class DiffValueModel extends Model {
		
		public Integer location;
		public Double value1, value2; 
		
		public DiffValueModel() {}
		public DiffValueModel(int loc, double v1, double v2) { 
			location = loc; 
			value1 = v1; 
			value2 = v2;
		}
	}

	public DataSegmentViz getDataChannel(int idx) {
		return dsChannels.get(idx);
	}
}