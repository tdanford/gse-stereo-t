/*
 * Author: tdanford
 * Date: Jul 27, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.util.*;
import java.awt.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.sigma.expression.StrandFilter;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptionParameters;
import edu.mit.csail.cgs.sigma.expression.workflow.Workflow;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowIndexing;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.sigma.expression.workflow.models.TranscriptCall;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;
import edu.mit.csail.cgs.viz.paintable.VerticalScalePainter;

public class DataSegmentPaintable extends AbstractRegionPaintable {
	
	private Collection<DataSegment> segSource;
	private ArrayList<DataSegment> segs;
	private TreeSet<Integer> channels;
	private Map<Integer,Color> dataColors;
	private Map<Integer,Color> segmentColors;
	private TranscriptCallPaintable callPainter;
	private PaintableScale scale;
	private NewVizPaintableProperties vizProps;
	
	public DataSegmentPaintable(NewVizPaintableProperties ps, Collection<DataSegment> ss, Integer... chs) {
		vizProps = ps;
		segSource = ss;
		segs = new ArrayList<DataSegment>();
		channels = new TreeSet<Integer>();
		dataColors = new TreeMap<Integer,Color>();
		segmentColors = new TreeMap<Integer,Color>();
		scale = new PaintableScale(0.0, 1.0);
		
		callPainter = new TranscriptCallPaintable();
		callPainter.addPaintableChangedListener(this);

		for(int i = 0; i < chs.length; i++) { 
			 if(i % 2 == 0) { 
				 addChannel(chs[i], Color.red, Coloring.clearer(Color.pink));
			 } else { 
				 addChannel(chs[i], Color.blue, Coloring.clearer(Color.cyan));
			 }
		}
	}
	
	public void setRegion(Region r) { 
		callPainter.setRegion(r);
		super.setRegion(r);
	}
	
	public Collection<TranscriptCall> getTranscripts(RegionKey key) { 
		return callPainter.getTranscripts(key);
	}
	
	public void findTranscripts(TranscriptionParameters tparams, String key, Integer[] channels) {
		WorkflowProperties props = new WorkflowProperties();
		WorkflowIndexing indexing = props.getIndexing(key);
		
		String expt = indexing.exptName(channels[0]);
		
		String[] args = new String[] {};
		Workflow worker = new Workflow(props, args);
		
		TreeSet<DataSegment> sorted = new TreeSet<DataSegment>(segs);
		
		Iterator<TranscriptCall> cs = 
			worker.completeCalling(
					sorted.iterator(),
					key, tparams, expt);
		
		callPainter.clearTranscriptCalls(region);
		callPainter.addTranscriptCalls(cs);
		dispatchChangedEvent();
	}
	
	public Collection<Cluster> clusters(String key, Integer[] channels) { 
		ArrayList<Cluster> clusters = new ArrayList<Cluster>();
		WorkflowProperties props = new WorkflowProperties();
		WorkflowIndexing indexing = props.getIndexing(key);
		
		String expt = indexing.exptName(channels[0]);
		
		String[] args = new String[] {};
		Workflow worker = new Workflow(props, args);
		
		TreeSet<DataSegment> sorted = new TreeSet<DataSegment>(segs);
		Iterator<Cluster> clusts = worker.processDatasegments(sorted.iterator(), channels);
		while(clusts.hasNext()) { clusters.add(clusts.next()); }
		
		return clusters;
	}
	
	public void addChannel(int ch, Color d) { 
		addChannel(ch, d, Coloring.clearer(d));
	}
	
	public void addChannel(int ch, Color data, Color seg) { 
		channels.add(ch);
		dataColors.put(ch, data);
		segmentColors.put(ch, seg);
	}

	protected void update() {
		callPainter.update();
		segs.clear();
		
		segs.addAll(segSource);
		
		scale.setScale(0.0, 1.0);
		for(DataSegment seg : segs) {
			 for(Integer ch : channels) { 
				 for(int i = 0; i < seg.dataLocations.length; i++) { 
					 scale.updateScale(seg.dataValues[ch][i]);
				 }
			 }
		}
	}
	
	private int pixX(int bp, int x1, int x2) {
		int w = x2-x1;
		int r1 = region.getStart(), r2 = region.getEnd();
		double f = (double)(bp - r1) / (r2 - r1);
		return x1 + (int)Math.round((double)w * f);
	}
	
	private int pixY(double value, int y1, int y2) { 
		int h = y2-y1;
		double f = scale.fractionalOffset(value);
		return y2 - (int)Math.round(f * (double)h);
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		System.out.println(String.format("<%f, %f>", scale.getMin(), scale.getMax()));
		scale.setScale(0.0, scale.getMax());
		callPainter.setScale(scale);

		paintSegments(g, x1, y1, x2, y2);
		paintData(g, x1, y1, x2, y2);
		paintTranscripts(g, x1, y1, x2, y2);
		
		System.out.println();
	}
	
	public PaintableScale getScale() { return scale; }
	
	public void unifyScales(DataSegmentPaintable dsp) { 
		double m = Math.max(scale.getMax(), dsp.scale.getMax());
		scale.setScale(0.0, m);
		dsp.scale.setScale(0.0, m);
	}
	
	private void paintData(Graphics g, int x1, int y1, int x2, int y2) { 
		for(Integer ch : channels) { 
			paintData(g, x1, y1, x2, y2, ch);
		}		
	}
	
	private void paintTranscripts(Graphics g, int x1, int y1, int x2, int y2) {
		if(vizProps.drawTranscripts) { 
			callPainter.paintItem(g, x1, y1, x2, y2);
		}
	}

	private void paintData(Graphics g, int x1, int y1, int x2, int y2, Integer ch) { 
		for(DataSegment seg : segs) { 
			paintData(g, x1, y1, x2, y2, ch, seg);
		}
	}

	private void paintSegments(Graphics g, int x1, int y1, int x2, int y2) { 
		for(Integer ch : channels) { 
			paintSegments(g, x1, y1, x2, y2, ch);
		}
	}

	private void paintSegments(Graphics g, int x1, int y1, int x2, int y2, Integer ch) {
		for(int segIndex = 0; segIndex < segs.size(); segIndex++) { 
			paintSegments(g, x1, y1, x2, y2, ch, segIndex);
		}
	}

	private void paintData(Graphics g, int x1, int y1, int x2, int y2, Integer ch, DataSegment seg) { 
		Graphics2D g2 = (Graphics2D)g;
		Stroke oldStroke = g2.getStroke();
		g2.setStroke(new BasicStroke(2.0f));
		
		int radius = 3;
		int diam = radius * 2;
		
		Color c = dataColors.get(ch);
		if(!seg.segmentTypes[ch].equals(Segment.LINE) && vizProps.grayBackgroundSegments) { 
			//c = Coloring.clearer(c);
			c = Coloring.clearer(Color.gray);
		}

		g2.setColor(c);
		
		for(int i = 0; i < seg.dataLocations.length; i++) { 
			int x = pixX(seg.dataLocations[i], x1, x2);
			double value = Math.max(seg.dataValues[ch][i], 0.0);
			int y = pixY(value, y1, y2);
			g2.drawOval(x-radius, y-radius, diam, diam);
		}
		
		g2.setStroke(oldStroke);
	}

	private void paintSegments(Graphics g, int x1, int y1, int x2, int y2, Integer ch, Integer segIndex) {
		
		DataSegment seg = segs.get(segIndex);
		boolean transcribed = seg.segmentTypes[ch].equals(Segment.LINE);
		
		/*
		System.out.println(String.format("%s:%d-%d:%s %d (%s)", 
				seg.chrom, seg.start, seg.end, seg.strand, 
				ch, (transcribed ? "*" : " ")));
		*/
		
		if(!transcribed) { 
			return; 
		}

		Graphics2D g2 = (Graphics2D)g;
		Stroke oldStroke = g2.getStroke();
		
		Double[] params = seg.segmentParameters[ch];
		Double intercept = params[0], slope = params[1];
		
		int segWidth = seg.end-seg.start;
		
		double rightValue = intercept + slope * (double)segWidth;
		double leftValue = intercept;
		
		int start = pixX(seg.start, x1, x2);
		int end = pixX(seg.end, x1, x2);
		int left = pixY(leftValue, y1, y2);
		int right = pixY(rightValue, y1, y2);
		
		int gammaheight = pixY(intercept, y1, y2);
		
		int[] x = new int[] { start, start, end, end };
		int[] y = new int[] { y2, left, right, y2 };
		
		if(seg.isAnyDifferential() && vizProps.showDifferentialSegments) { 
			//g2.setColor(segmentColors.get(ch));
			//g2.fillPolygon(x, y, 4);
			if(seg.getExpectedDifferential("mata") >= 0.0) { 
				g2.setColor(Color.red);
			} else { 
				g2.setColor(Color.blue);
			}
			g2.fillRect(start, y2-10, end-start, 10);
		}
		
		g2.setColor(Color.black);
		g2.setStroke(new BasicStroke(2.0f));
		
		//g2.drawLine(start-5, gammaheight, start+5, gammaheight);
		//System.out.println(String.format("(ch: %d, itcpt: %f, slp: %f)", ch, intercept, slope));
	
		if(vizProps.paintSegmentParams) { 
			g2.setColor(Coloring.opaque(segmentColors.get(ch)));
			g2.drawLine(start, left, end, right);
		}
		
		if(vizProps.drawSegmentBoundaries) { 
			g2.setColor(Color.gray);
			g2.drawLine(start, y1, start, y2);
			g2.drawLine(end, y1, end, y2);
		}
		
		g2.setStroke(oldStroke);
	}

	public void addFGChannel(int i) {
		addChannel(i, Color.red, Coloring.clearer(Color.pink));
	}
	public void addBGChannel(int i) {
		addChannel(i, Color.blue, Coloring.clearer(Color.cyan));
	}

	public void setTranscripts(Collection<TranscriptCall> ts) {
		callPainter.setTranscripts(ts);
	}
}

