/*
 * Author: tdanford
 * Date: Jul 27, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.*;

import javax.swing.AbstractAction;
import javax.swing.Action;

import java.io.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.ExpanderIterator;
import edu.mit.csail.cgs.sigma.GeneGenerator;
import edu.mit.csail.cgs.sigma.OverlappingRegionKeyExpander;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptionParameters;
import edu.mit.csail.cgs.sigma.expression.workflow.WholeGenome;
import edu.mit.csail.cgs.sigma.expression.workflow.Workflow;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.assessment.TranscriptCallSet;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.sigma.litdata.LitDataProperties;
import edu.mit.csail.cgs.sigma.litdata.snyder.SnyderCoverage;
import edu.mit.csail.cgs.sigma.litdata.steinmetz.SteinmetzProbeGenerator;
import edu.mit.csail.cgs.sigma.litdata.steinmetz.SteinmetzProperties;
import edu.mit.csail.cgs.utils.ArrayUtils;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.ModelListener;
import edu.mit.csail.cgs.viz.NonOverlappingLayout;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.eye.LocatedValueModel;
import edu.mit.csail.cgs.viz.eye.ModelLocatedValues;
import edu.mit.csail.cgs.viz.eye.ModelPrefs;
import edu.mit.csail.cgs.viz.paintable.AbstractPaintable;
import edu.mit.csail.cgs.viz.paintable.ArrowPainter;
import edu.mit.csail.cgs.viz.paintable.DirectedTagPainter;
import edu.mit.csail.cgs.viz.paintable.DoubleBufferedPaintable;
import edu.mit.csail.cgs.viz.paintable.HorizontalLadderScalePainter;
import edu.mit.csail.cgs.viz.paintable.HorizontalScalePainter;
import edu.mit.csail.cgs.viz.paintable.PaintablePanel;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;
import edu.mit.csail.cgs.viz.paintable.VerticalScalePainter;

public class NewViz extends AbstractRegionPaintable {
	
	public static String key = "s288c";
	public static String exptName = "mata";
	public static boolean steinmetz = false;
	public static boolean snyder = false;
	
	public static void main(String[] args) {
		WorkflowProperties wps = new WorkflowProperties();
		try {
			WholeGenome genome = WholeGenome.loadWholeGenome(wps, key);
			genome.loadIterators();
			
			Genome g = genome.getGenome();
			NewViz viz = new NewViz(genome);
			//viz.addChannels(0, 1);
			//viz.addChannels(2, 3);
			
			viz.addChannel(0, Color.red);
			//viz.addChannel(2, Color.red);
			//viz.addChannel(4, Color.red);
			//viz.addChannel(6, Color.red);
			//viz.addChannel(8, Color.red);
			
			viz.addChannel(1, Color.blue);
			//viz.addChannel(3, Color.blue);
			//viz.addChannel(5, Color.blue);
			//viz.addChannel(7, Color.blue);
			//viz.addChannel(9, Color.blue);
			
			if(steinmetz) { 
				viz.addSteinmetz();
			}
			
			if(snyder) { 
				viz.addSnyder();
			}
			
			viz.addTranscripts();
			
			Region region = new Region(g, "5", 322000, 326000);
			viz.setRegion(region);
			
			GenericRegionFrame frame = new GenericRegionFrame(viz);
			
			PaintablePanel pp = frame.getRegionPaintablePanel();

			frame.addActionMenuItem(viz.createUpdateVizParamsAction());
			frame.addActionMenuItem(viz.viewClustersAction());
			frame.addActionMenuSeparator();
			
			frame.addActionMenuItem(viz.createResegmentAction());
			frame.addActionMenuItem(viz.createUpdateSegmentationParamsAction());
			frame.addActionMenuItem(viz.createResetSegmentationParamsAction());
			frame.addActionMenuSeparator();

			frame.addActionMenuItem(viz.createResetTranscriptParamsAction());
			frame.addActionMenuItem(viz.createUpdateTranscriptParamsAction());
			frame.addActionMenuItem(viz.createTranscriptFindingAction(0));
			frame.addActionMenuSeparator();
			
			frame.addActionMenuItem(viz.createSteinmetzAveragingAction());
			
			frame.showMe();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private NewVizPaintableProperties vizProps;

	private SegmentationParameters segParams;
	private TranscriptionParameters transParams;
	
	private DataSegmentPaintable plusPainter, minusPainter;
	private GeneViz genePainter;
	private SteinmetzViz steinmetzPainter;
	private SnyderViz snyderPainter;
	
	private LinkedList<DataSegment> plusSegs, minusSegs;
	private Expander<Region,DataSegment> plusExpander, minusExpander;
	
	public NewViz(WholeGenome g) {
		this(new OverlappingRegionKeyExpander<DataSegment>(g.getWatsonSegments()),
			 new OverlappingRegionKeyExpander<DataSegment>(g.getCrickSegments()),
			 g.getProperties().getSigmaProperties().getGeneGenerator(g.getStrain()));
	}
	
	public NewViz(Expander<Region,DataSegment> p, Expander<Region,DataSegment> m, GeneGenerator gg) {
		WorkflowProperties pms = new WorkflowProperties();
		vizProps = new NewVizPaintableProperties();
		segParams = pms.getDefaultSegmentationParameters();
		transParams = pms.getDefaultTranscriptionParameters();
		
		plusExpander = p;
		minusExpander = m;
		plusSegs = new LinkedList<DataSegment>();
		minusSegs = new LinkedList<DataSegment>();
		plusPainter = new DataSegmentPaintable(vizProps, plusSegs);
		minusPainter = new DataSegmentPaintable(vizProps, minusSegs);
		genePainter = new GeneViz(gg);
		steinmetzPainter = null;
		snyderPainter = null;
	}
	
	public void addSteinmetz() { 
		steinmetzPainter = new SteinmetzViz();
		vizProps.vizSteinmetz = true;
		dispatchChangedEvent();
	}
	
	public void addSnyder() { 
		LitDataProperties props = new LitDataProperties();
		File dir = props.getDirectory();
		File input = new File(dir, "snyder_coverage.data");
		SnyderCoverage coverage;
		try {
			coverage = new SnyderCoverage(input);
			snyderPainter = new SnyderViz(coverage);
			vizProps.vizSnyder = true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addChannel(int ch, Color c) { 
		plusPainter.addChannel(ch, c);
		minusPainter.addChannel(ch, c);
	}
	
	public void addChannels(int fg, int bg) { 
		plusPainter.addFGChannel(fg);
		minusPainter.addFGChannel(fg);

		plusPainter.addBGChannel(bg);
		minusPainter.addBGChannel(bg);
	}
	
	public void addTranscripts() { 
		try {
			TranscriptCallSet calls = new TranscriptCallSet(new WorkflowProperties(), key);
			plusPainter.setTranscripts(calls.getWatsonTranscripts());
			minusPainter.setTranscripts(calls.getCrickTranscripts());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void update() {
		updateWithSegments(plusExpander.execute(region), minusExpander.execute(region));
	}
	
	private void updateWithSegments(Iterator<DataSegment> wsegs, Iterator<DataSegment> csegs) { 
		genePainter.update();

		plusSegs.clear();
		minusSegs.clear();
		
		while(wsegs.hasNext()) { 
			plusSegs.add(wsegs.next());
		}
		
		while(csegs.hasNext()) { 
			minusSegs.add(csegs.next());
		}
		
		plusPainter.setRegion(region);
		minusPainter.setRegion(region);
		
		plusPainter.update();
		minusPainter.update();
		
		if(steinmetzPainter != null) { 
			steinmetzPainter.update();
		}
		
		if(snyderPainter != null) { 
			snyderPainter.update();
		}
	}
	
	public Action createTranscriptFindingAction(int i) { 
		return new TranscriptFindingAction(key, i);
	}
	
	public Action viewClustersAction() { 
		return new AbstractAction("View Clusters") { 
			public void actionPerformed(ActionEvent e) { 
				Integer[] channels = new Integer[] { 0 };
				WorkflowProperties props = new WorkflowProperties(); 
				ArrayList<Cluster> clusters = new ArrayList<Cluster>();
				clusters.addAll(plusPainter.clusters(key, channels));
				clusters.addAll(minusPainter.clusters(key, channels));
				
				for(Cluster c : clusters) { 
					TranscriptCallingViewer calling = new TranscriptCallingViewer(key, props, c);
					calling.showFrame();
				}
			}
		};
	}
	
	private class TranscriptFindingAction extends AbstractAction {
		private String key;
		private Integer[] indices;
		
		public TranscriptFindingAction(String k, int i) {
			super(String.format("#%d Transcript-Calling", i));
			key = k;
			indices = new Integer[] { i };
		}
		public void actionPerformed(ActionEvent e) {
			plusPainter.findTranscripts(transParams, key, indices);
			minusPainter.findTranscripts(transParams, key, indices);
			NewViz.this.dispatchChangedEvent();
		}
	}
	
	public Action createResegmentAction() { 
		return new AbstractAction("Resegment...") { 
			public void actionPerformed(ActionEvent arg0) {
				resegment();
			}
		};
	}
	
	public Action createResetSegmentationParamsAction() { 
		return new AbstractAction("Reset Seg Params") { 
			public void actionPerformed(ActionEvent e) {
				WorkflowProperties props = new WorkflowProperties();
				segParams = props.getDefaultSegmentationParameters();
			}
		};
	}
	
	public Action createResetTranscriptParamsAction() { 
		return new AbstractAction("Reset Transcript Params") { 
			public void actionPerformed(ActionEvent e) {
				WorkflowProperties props = new WorkflowProperties();
				transParams = props.getDefaultTranscriptionParameters();
			}
		};
	}
	
	public Action createSteinmetzAveragingAction() { 
		return new AbstractAction("Average Steinmetz Data") { 
			public void actionPerformed(ActionEvent e) { 
				if(steinmetzPainter != null) {
					RegionKey wkey = new RegionKey(region.getChrom(), region.getStart(), region.getEnd(), "+");
					RegionKey ckey = new RegionKey(region.getChrom(), region.getStart(), region.getEnd(), "-");
					for(TranscriptCall call : plusPainter.getTranscripts(wkey)) {
						steinmetzPainter.addAveraging(call);
					}
					for(TranscriptCall call : minusPainter.getTranscripts(ckey)) {
						steinmetzPainter.addAveraging(call);
					}
					steinmetzPainter.update();
					NewViz.this.dispatchChangedEvent();
				}
			}
		};
	}
	
	public Action createUpdateVizParamsAction() { 
		return new AbstractAction("Visualization Params") { 
			public void actionPerformed(ActionEvent e) { 
				
				ModelPrefs<NewVizPaintableProperties> sprefs = 
					new ModelPrefs<NewVizPaintableProperties>(vizProps);
				
				sprefs.addModelListener(new ModelListener<NewVizPaintableProperties>() {
					public void modelChanged(NewVizPaintableProperties model) {
						vizProps.setFromModel(model);
						
						if(vizProps.vizSteinmetz && steinmetzPainter == null) { 
							addSteinmetz();
						} 
						if(!vizProps.vizSteinmetz && steinmetzPainter != null) { 
							steinmetzPainter = null;
						}

						if(vizProps.vizSnyder && snyderPainter == null) { 
							addSnyder();
						} 
						if(!vizProps.vizSnyder && snyderPainter != null) { 
							snyderPainter = null;
						}

						NewViz.this.dispatchChangedEvent();
					} 
				});
				sprefs.display();
			}
		};
	}
	
	public Action createUpdateSegmentationParamsAction() { 
		return new AbstractAction("Segmentation Params") { 
			public void actionPerformed(ActionEvent e) { 
				
				ModelPrefs<SegmentationParameters> sprefs = 
					new ModelPrefs<SegmentationParameters>(segParams);
				
				sprefs.addModelListener(new ModelListener<SegmentationParameters>() {
					public void modelChanged(SegmentationParameters model) {
						segParams.setFromModel(model);
					} 
				});
				sprefs.display();
			}
		};
	}
	
	public Action createUpdateTranscriptParamsAction() { 
		return new AbstractAction("Transcription Params") { 
			public void actionPerformed(ActionEvent e) { 
				
				ModelPrefs<TranscriptionParameters> sprefs = 
					new ModelPrefs<TranscriptionParameters>(transParams);
				
				sprefs.addModelListener(new ModelListener<TranscriptionParameters>() {
					public void modelChanged(TranscriptionParameters model) {
						transParams.setFromModel(model);
					} 
				});
				sprefs.display();
			}
		};
	}
	
	public void resegment() { 
		String[] args = new String[] {};
		WorkflowProperties props = new WorkflowProperties();
		Workflow workflow = new Workflow(props, args);
		
		Iterator<DataSegment> wsegs = 
			workflow.completeNoChunksSegmentation(watsonProbes(), key, exptName, segParams);
		Iterator<DataSegment> csegs = 
			workflow.completeNoChunksSegmentation(crickProbes(), key, exptName, segParams);
		updateWithSegments(wsegs, csegs);
		
		dispatchChangedEvent();
	}
	
	private Iterator<ProbeLine> watsonProbes() { 
		return new SortingIterator<ProbeLine>(
					new ExpanderIterator<DataSegment,ProbeLine>(
							new SegmentProbeLineExpander(), 
							plusExpander.execute(region)));
	}

	private Iterator<ProbeLine> crickProbes() { 
		return new SortingIterator<ProbeLine>(
				new ExpanderIterator<DataSegment,ProbeLine>(
						new SegmentProbeLineExpander(), 
						minusExpander.execute(region)));
	}
	
	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2-x1, h=y2-y1;
		int my = y1 + h/2;
		int spacing = 3;
		
		int geneHeight = Math.max(10+spacing*2, h/10);
		int scaleHeight = geneHeight;
		int scaleWidth = 0;
		
		int gy1 = my - geneHeight/2 - scaleHeight/2 - spacing*2;
		int gy2 = gy1 + geneHeight + scaleHeight + spacing*4;
		
		int sh = steinmetzPainter != null ? (h-geneHeight-scaleHeight)/5 : 0;
		int sny = snyderPainter != null ? (h-geneHeight-scaleHeight)/5 : 0;
		int ex = sh + sny;
		
		g.setColor(Color.white);
		g.fillRect(x1, y1, w, h);
		
		plusPainter.unifyScales(minusPainter);
		
		PaintableScale rscale = new PaintableScale((double)region.getStart(), (double)region.getEnd());
		PaintableScale scale = plusPainter.getScale();
		
		VerticalScalePainter vsp = new VerticalScalePainter(scale);
		HorizontalLadderScalePainter hsp = new HorizontalLadderScalePainter(rscale);

		vsp.paintItem(g, x1, y1, x1+scaleWidth, gy1-ex);		
		plusPainter.paintItem(g, x1+scaleWidth, y1, x2, gy1-ex);
		if(steinmetzPainter != null) { 
			steinmetzPainter.paintWatson(g, x1+scaleWidth, gy1-ex, x2, gy1);
		}
		if(snyderPainter != null) { 
			snyderPainter.paintWatson(g, x1+scaleWidth, gy1-ex+sh, x2, gy1);
		}
		
		genePainter.paintItem(g, x1+scaleWidth, gy1+spacing, x2, gy1+spacing+geneHeight);
		hsp.paintItem(g, x1+scaleWidth, gy1+geneHeight+spacing*2, x2, gy2-spacing);
		
		if(steinmetzPainter != null) { 
			steinmetzPainter.paintCrick(g, x1+scaleWidth, gy2, x2, gy2+sh);
		}
		vsp.paintItem(g, x1, gy2+sh, x1+scaleWidth, y2);		
		minusPainter.paintItem(g, x1+scaleWidth, gy2+sh, x2, y2);
		
	}
	
	private class GeneViz extends AbstractPaintable {

		private Expander<Region,Gene> geneExpander;

		public GeneViz(WorkflowProperties wps, String strain) {
			this(wps.getSigmaProperties().getGeneGenerator(strain));
		}

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
			
			DirectedTagPainter painter = new DirectedTagPainter();
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
	
	public static class AveragingRegion {
		
		private Set<Integer> breaks;
		private String chrom, strand;
		
		public AveragingRegion(String c, String s) { 
			chrom = c; 
			strand = s;
			breaks = new TreeSet<Integer>();
		}
		
		public void addBreak(Integer b) { breaks.add(b); }
		
		public void addRegion(RegionKey k) { 
			if(!k.chrom.equals(chrom) || !k.strand.equals(strand)) { 
				throw new IllegalArgumentException(k.toString());
			}
			addBreak(k.start);
			addBreak(k.end);
		}
		
		public RegionKey[] regions() { 
			RegionKey[] rks = new RegionKey[Math.max(0, breaks.size()-1)];
			int s = -1;
			int i = 0;
			for(Integer b : breaks) { 
				if(s != -1) { 
					RegionKey rk = new RegionKey(chrom, s, b, strand);
					rks[i++] = rk;
				}
				s = b+1;
			}
			return rks;
		}
	}
	
	private class SteinmetzViz extends AbstractPaintable {
		
		private SteinmetzProbeGenerator generator;
		private ModelLocatedValues watsonValues, crickValues;
		
		private Map<String,AveragingRegion> watsonAverage, crickAverage;
		private Map<RegionKey,Double> averages;
		private Map<RegionKey,Integer> counts;
		
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
		
			watsonAverage = new TreeMap<String,AveragingRegion>();
			crickAverage = new TreeMap<String,AveragingRegion>();
			averages = new TreeMap<RegionKey,Double>();
			counts = new TreeMap<RegionKey,Integer>();
			
			//watsonValues.synchronizeProperty(ModelLocatedValues.scaleKey, crickValues);
			watsonValues.setProperty(ModelLocatedValues.stemKey, false);
			crickValues.setProperty(ModelLocatedValues.stemKey, false);

			watsonValues.setProperty(ModelLocatedValues.radiusKey, 1);
			crickValues.setProperty(ModelLocatedValues.radiusKey, 1);

			watsonValues.setProperty(ModelLocatedValues.colorKey, Coloring.clearer(Color.red));
			crickValues.setProperty(ModelLocatedValues.colorKey, Coloring.clearer(Color.blue));
		}
		
		public void addAveraging(TranscriptCall c) { 
			if(c.strand.equals("+")) { 
				if(!watsonAverage.containsKey(c.chrom)) { 
					watsonAverage.put(c.chrom, new AveragingRegion(c.chrom, c.strand));
				}
				watsonAverage.get(c.chrom).addRegion(c);
			} else { 
				if(!crickAverage.containsKey(c.chrom)) { 
					crickAverage.put(c.chrom, new AveragingRegion(c.chrom, c.strand));
				}
				crickAverage.get(c.chrom).addRegion(c);				
			}
		}
		
		public void update() { 
			watsonValues.clearModels();
			crickValues.clearModels();
			
			averages.clear();
			counts.clear();

			if(watsonAverage.containsKey(region.getChrom())) { 
				RegionKey[] ks = watsonAverage.get(region.getChrom()).regions();
				for(int i = 0; i < ks.length; i++) { 
					averages.put(ks[i], 0.0);
					counts.put(ks[i], 0);
				}
			}
			if(crickAverage.containsKey(region.getChrom())) { 
				RegionKey[] ks = crickAverage.get(region.getChrom()).regions();
				for(int i = 0; i < ks.length; i++) { 
					averages.put(ks[i], 0.0);
					counts.put(ks[i], 0);
				}
			}
			
 			Iterator<ExpressionProbe> probes = generator.execute(region);
			Color watsonColor = Color.red;
			Color crickColor = Color.blue;
			double max = 1.0, min = 0.0;

			while(probes.hasNext()) { 
				ExpressionProbe probe = probes.next();
				int loc = probe.getLocation();
				double value = probe.mean();
				char strand = probe.getStrand();
				
				for(RegionKey k : averages.keySet()) { 
					if(strand == k.strand.charAt(0) && probe.getChrom().equals(k.chrom) && k.contains(loc)) {
						averages.put(k, averages.get(k) + value);
						counts.put(k, counts.get(k) + 1);
					}
				}
				
				if(strand == '+') { 
					watsonValues.addModel(new LocatedValueModel(loc, value, watsonColor));
				} else { 
					crickValues.addModel(new LocatedValueModel(loc, value, crickColor));					
				}
				max = Math.max(max, value);
				min = Math.min(min, value);
			}
			
			PaintableScale scale = new PaintableScale(min, max);
			watsonValues.setProperty(ModelLocatedValues.scaleKey, scale);
			crickValues.setProperty(ModelLocatedValues.scaleKey, scale);
			
			watsonValues.setBounds(region.getStart(), region.getEnd());
			crickValues.setBounds(region.getStart(), region.getEnd());
		}
		
		public void paintWatson(Graphics g, int x1, int y1, int x2, int y2) {
			int w = x2-x1, h = y2-y1;
			PaintableScale scale = watsonValues.getPropertyValue(ModelLocatedValues.scaleKey);
			VerticalScalePainter vsp = new VerticalScalePainter(scale);
			watsonValues.paintItem(g, x1, y1, x2, y2);			
			vsp.paintItem(g, x1, y1, x2, y2);
			
			paintAverages(g, x1, y1, x2, y2, "+", scale);
		}
		
		public void paintCrick(Graphics g, int x1, int y1, int x2, int y2) {
			int w = x2-x1, h = y2-y1;
			PaintableScale scale = crickValues.getPropertyValue(ModelLocatedValues.scaleKey);
			VerticalScalePainter vsp = new VerticalScalePainter(scale);
			crickValues.paintItem(g, x1, y1, x2, y2);
			vsp.paintItem(g, x1, y1, x2, y2);
			
			paintAverages(g, x1, y1, x2, y2, "-", scale);
		}

		public void paintAverages(Graphics g, int x1, int y1, int x2, int y2, String strand, PaintableScale scale) {
			Graphics2D g2 = (Graphics2D)g;
			Stroke oldStroke = g2.getStroke();
			g2.setStroke(new BasicStroke(2.0f));
			g.setColor(Color.black);
			int h = y2-y1, w = x2-x1;
			for(RegionKey key : averages.keySet()) { 
				if(key.strand.equals(strand) && counts.get(key) > 0) { 
					double mean = averages.get(key) / (double)counts.get(key);
					double meanf = scale.fractionalOffset(mean);
					double leftf = (double)(key.start - region.getStart()) / (double)region.getWidth();
					double rightf = (double)(key.end - region.getStart()) / (double)region.getWidth();
					
					int meany = y2-(int)Math.round(meanf * (double)h);
					int left = x1 + (int)Math.round(leftf * (double)w);
					int right = x1 + (int)Math.round(rightf * (double)w);
					
					g.drawLine(left, meany, right, meany);
					g.drawLine(left, meany, left, y2);
					g.drawLine(right, meany, right, y2);
					g.drawString(String.format("%.1f", mean), left+1, meany-1);
				}
			}
			g2.setStroke(oldStroke);
		}

		public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
			int w = x2-x1, h = y2-y1;
			int h2 = h/2;
			watsonValues.paintItem(g, x1, y1, x2, y1+h2);
			crickValues.paintItem(g, x1, y1+h2, x2, y2);
		}  
	}

	private class SnyderViz extends AbstractPaintable {
		
		private SnyderCoverage coverage;
		private ModelLocatedValues watsonValues;
		
		public void updateProps() {
		}

		public SnyderViz(SnyderCoverage c) {
			coverage = c;
			watsonValues = new ModelLocatedValues();
			
			watsonValues.setProperty(ModelLocatedValues.stemKey, false);
			watsonValues.setProperty(ModelLocatedValues.radiusKey, 1);
			watsonValues.setProperty(ModelLocatedValues.colorKey, Coloring.clearer(Color.red));
		}
		
		public void update() { 
			watsonValues.clearModels();
			
			RegionKey wkey = new RegionKey(region.getChrom(), region.getStart(), region.getEnd(), "+");
			int[] wcounts = coverage.coverage(wkey);
			
			Color watsonColor = Color.red;
			Color crickColor = Color.blue;
			
			int max = 1;
			
			for(int i = 0; i < wcounts.length; i++) { 
				int loc = region.getStart() + i;
				double value = (double)wcounts[i];
				watsonValues.addModel(new LocatedValueModel(loc, value, watsonColor));
				max = Math.max(max, wcounts[i]);
			}
			
			PaintableScale scale = new PaintableScale(0.0, (double)(max+1));
			watsonValues.setProperty(ModelLocatedValues.scaleKey, scale);
			watsonValues.setBounds(region.getStart(), region.getEnd());
		}
		
		public void paintWatson(Graphics g, int x1, int y1, int x2, int y2) {
			int w = x2-x1, h = y2-y1;
			PaintableScale scale = watsonValues.getPropertyValue(ModelLocatedValues.scaleKey);
			VerticalScalePainter vsp = new VerticalScalePainter(scale);
			watsonValues.paintItem(g, x1, y1, x2, y2);			
			vsp.paintItem(g, x1, y1, x2, y2);
		}

		public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
			int w = x2-x1, h = y2-y1;
			int h2 = h;
			watsonValues.paintItem(g, x1, y1, x2, y1+h2);
		}  
	}
}

class SortingIterator<T extends Comparable<T>> implements Iterator<T> { 
	private Iterator<T> output;
	
	public SortingIterator(Iterator<T> itr) { 
		TreeSet<T> vals =new TreeSet<T>();
		while(itr.hasNext()) { vals.add(itr.next()); }
		output = vals.iterator();
	}

	public boolean hasNext() {
		return output.hasNext();
	}

	public T next() {
		return output.next();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}

class SegmentProbeLineExpander implements Expander<DataSegment,ProbeLine> {
	
	public SegmentProbeLineExpander() { 
	}

	public Iterator<ProbeLine> execute(DataSegment a) {
		LinkedList<ProbeLine> lines = new LinkedList<ProbeLine>();
		for(int i = 0; i < a.dataLocations.length; i++) { 
			Double[] values =new Double[a.dataValues.length];
			for(int k = 0; k < a.dataValues.length; k++) { 
				values[k] = a.dataValues[k][i];
			}
			ProbeLine pl = new ProbeLine(a.chrom, a.strand, a.dataLocations[i], values);
			lines.add(pl);
		}
		return lines.iterator();
	}
}
