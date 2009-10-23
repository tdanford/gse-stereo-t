/*
 * Author: tdanford
 * Date: January 22, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.sql.SQLException;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;

import edu.mit.csail.cgs.datasets.chipchip.ChipChipData;
import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.SudeepExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.segmentation.*;
import edu.mit.csail.cgs.sigma.expression.segmentation.viz.ModelSegmentValues;
import edu.mit.csail.cgs.sigma.expression.transcription.*;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.TAFit;
import edu.mit.csail.cgs.sigma.expression.transcription.identifiers.ExhaustiveIdentifier;
import edu.mit.csail.cgs.sigma.expression.transcription.viz.ModelTranscriptCallPainter;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.FileInputData;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.viz.eye.*;
import edu.mit.csail.cgs.viz.paintable.DoubleBufferedPaintable;
import edu.mit.csail.cgs.viz.paintable.PaintablePanel;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;
import edu.mit.csail.cgs.viz.paintable.VerticalScalePainter;
import edu.mit.csail.cgs.viz.utils.FileChooser;

/**
 * Paints a single track of stranded data, plus segments and transcript calls. 
 * 
 * @author tdanford
 */
public class TranscriptionViz 
	extends PaintablePanel {
	
	private VerticalScalePainter scalePaintable;       // left-hand scale.
	private ModelLocatedValues valuesPaintable, predictedPaintable;  // probes
	private ModelSegmentValues fittedPaintable;	       // segments
	private ModelTranscriptCallPainter callPaintable;  // arrows
	
	private TranscriptionParameters callParams;
	private ExhaustiveIdentifier identifier;
	
	private String chrom, strand;
	private Integer start, end;
	private Integer channel;
	
	private InputData data;
	private ArrayList<Segment> segments;
	private ArrayList<Call> calls; 

	//////////////////////////////////////////////////////////////////
	// For dynamic painting of segment and probe values
	private Set<Model> nearModel; 
	private Collection<Pair<Rectangle,Model>> selectedSegmentModels;
	private Point modelPoint, mousePoint;
	private boolean probeMarking, transcriptMarking;
	//////////////////////////////////////////////////////////////////
	
	public TranscriptionViz(String strain, Integer ch, boolean dir) { 
		super();
		
		callParams = new TranscriptionParameters();
		identifier = new ExhaustiveIdentifier(new WorkflowProperties(), callParams);
		
		valuesPaintable = new ModelLocatedValues();
		predictedPaintable = new ModelLocatedValues();
		fittedPaintable = new ModelSegmentValues();
		callPaintable = new ModelTranscriptCallPainter();
		
		probeMarking = false;
		transcriptMarking = false;
		
		data = null;
		segments = new ArrayList<Segment>();
		calls = new ArrayList<Call>();
		
		chrom = null; 
		strand = "+";
		start = 0; end = 1;
		channel = ch;
		
		BaseExpressionProperties bps = new SudeepExpressionProperties();
		GeneAnnotationProperties gaps = new GeneAnnotationProperties();
		
		DoubleBufferedPaintable vbuffered = new DoubleBufferedPaintable(valuesPaintable);
		vbuffered.paintBackground(true);
		setPaintable(vbuffered);
		
		String bk = ModelRangeValues.boundsKey;
		String sk = ModelLocatedValues.scaleKey;
		
		callPaintable.setProperty(ModelTranscriptCallPainter.directionKey, dir);
		
		valuesPaintable.setProperty(ModelLocatedValues.stemKey, false);
		predictedPaintable.setProperty(ModelLocatedValues.stemKey, false);
		
		valuesPaintable.synchronizeProperty(bk, predictedPaintable);
		valuesPaintable.synchronizeProperty(bk, fittedPaintable);
		valuesPaintable.synchronizeProperty(bk, callPaintable);
		
		String scaleKey = ModelLocatedValues.scaleKey;
		valuesPaintable.synchronizeProperty(scaleKey, fittedPaintable);
		valuesPaintable.synchronizeProperty(scaleKey, predictedPaintable);
		fittedPaintable.setProperty(ModelSegmentValues.drawWeightsKey, Boolean.TRUE);

		PaintableScale scale = valuesPaintable.getPropertyValue(scaleKey);
		scalePaintable = new VerticalScalePainter(scale);
		
		predictedPaintable.setProperty(ModelLocatedValues.colorKey, Color.magenta);
		
		addMouseMotionListener(new MouseMotionAdapter() { 
			public void mouseMoved(MouseEvent e) { 
				Pair<Point,Set<Model>> pp = 
					valuesPaintable.findNearestDrawnPoint(e.getPoint());
				if(transcriptMarking) { 
					selectedSegmentModels = fittedPaintable.findDrawnRects(e.getPoint());
					selectedSegmentModels.addAll(
							callPaintable.findDrawnRects(e.getPoint()));
					repaint();
				}
				if(probeMarking && pp != null) { 
					nearModel = pp.getLast();
					modelPoint = pp.getFirst();
					mousePoint = e.getPoint();
					repaint();
				}
			}
		});
		
		addComponentListener(new ComponentListener() {

			public void componentHidden(ComponentEvent arg0) {
				nearModel = null;
				modelPoint = null;
				repaint();
			}

			public void componentMoved(ComponentEvent arg0) {
				nearModel = null;
				modelPoint = null;
				repaint();
			}

			public void componentResized(ComponentEvent arg0) {
				nearModel = null;
				modelPoint = null;
				repaint();
			}

			public void componentShown(ComponentEvent arg0) {
				nearModel = null;
				modelPoint = null;
				repaint();
			} 
			
		});
	}
	
	public void setCallingParameters(TranscriptionParameters cps) {
		callParams = cps;
		identifier = new ExhaustiveIdentifier(new WorkflowProperties(), callParams);
	}
	
	public void setData(InputData d, Integer dataStart, Integer dataEnd) {
		data = d;
		Integer[] locations = data.locations();
		Double[] values = data.values()[channel];
		
		valuesPaintable.clearModels();
		fittedPaintable.clearModels();
		callPaintable.clearModels();
		predictedPaintable.clearModels();
		
		for(int i = 0; i < locations.length; i++) {
			if(dataStart <= locations[i] && dataEnd >= locations[i]) { 
				valuesPaintable.addModel(new Datapoint(locations[i], Math.max(0.0, values[i])));
			}
		}
		
		segments.clear();
		calls.clear();

		chrom = data.chrom();
		strand = data.strand();
		start = dataStart;
		end = dataEnd;

		PropertyValueWrapper<Integer[]> boundsWrapper = 
			(PropertyValueWrapper<Integer[]>)valuesPaintable.getProperty(
					ModelRangeValues.boundsKey);
		boundsWrapper.setValue(new Integer[] { start, end });
		
		valuesPaintable.setProperty(ModelRangeValues.boundsKey, boundsWrapper);
		predictedPaintable.setProperty(ModelRangeValues.boundsKey, boundsWrapper);
		callPaintable.setProperty(ModelRangeValues.boundsKey, boundsWrapper);
		fittedPaintable.setProperty(ModelRangeValues.boundsKey, boundsWrapper);
		
		repaint();
	}
	
	public void synchronizeScales(TranscriptionViz sv) { 
		String sk = ModelLocatedValues.scaleKey;
		PaintableScale oldScale = sv.valuesPaintable.getPropertyValue(sk);
		double max = oldScale.getMax(), min = oldScale.getMin();
		
		valuesPaintable.synchronizeProperty(sk, sv.fittedPaintable);
		valuesPaintable.synchronizeProperty(sk, sv.valuesPaintable);
		valuesPaintable.synchronizeProperty(sk, sv.callPaintable);
		valuesPaintable.synchronizeProperty(sk, sv.predictedPaintable);
		
		PaintableScale scale = valuesPaintable.getPropertyValue(sk);
		scale.updateScale(min);
		scale.updateScale(max);
		
		sv.scalePaintable = new VerticalScalePainter(scale);
		sv.repaint();
	}
	
	public void addPredictedData(Integer offset, Double[] values) {
		Integer[] locs = data.locations();
		
		for(int i = 0; i < values.length; i++) { 
			//int idx = indices[i];
			//predictedPaintable.addModel(new Datapoint(locs[idx], values[i]));
			//System.out.println(String.format("(%d):%.2f", indices[i], values[i]));
			predictedPaintable.addModel(new Datapoint(locs[offset+i], values[i]));
		}		
		repaint();
	}
	
	/**
	 * Important -- this method assumes that the 
	 * coordinates of the transcript calls (the 'start' and 'end' fields) have *already been 
	 * converted* into real (base) coordinates.
	 * 
	 * @param arrangement
	 */
	public void setCalls(Collection<Call> cs) {
		calls.clear();
		callPaintable.clearModels();

		for(Call c : cs) {
			calls.add(c);
			callPaintable.addModel(new ModelTranscriptCallPainter.CallModel(c.start, c.end));
		}
		
		repaint();
	}
	
	public void setFits(Collection<TAFit> fits) { 
		ArrayList<Call> fitCalls = new ArrayList<Call>();

		predictedPaintable.clearModels();
		for(TAFit fit : fits) {
			Cluster c = fit.arrangement.cluster;
			Integer[] locations = c.locations;
			Integer offset = c.segments[0].start;
			
			for(int i = 0; i < fit.arrangement.calls.length; i++) { 
				Call tc = fit.arrangement.calls[i];
				Double param = fit.params[i];
				int si = tc.start, ei = tc.end-1;
				int idx1 = c.segments[si].start, idx2 = c.segments[ei].end;
				int loc1 = locations[idx1]; 
				int loc2 = locations[idx2];
				fitCalls.add(new Call(loc1, loc2, param));
			}
			
			addPredictedData(offset, fit.predicted);
		}
		
		setCalls(fitCalls);
	}
	
	public void setSegments(Iterator<Segment> segs) {
		Integer[] locs = data.locations();
		
		fittedPaintable.clearModels();
		callPaintable.clearModels();
		
		calls.clear();
		segments.clear();

		while(segs.hasNext()) { 
			Segment s = segs.next();
			if(s.start >= 0 && s.end <= locs.length) { 
				FittedRange r = new FittedRange(s, locs, s.params, s.segmentType, s.shared);

				fittedPaintable.addModel(r);
				segments.add(s);
			}
		}

		repaint();
	}
	
	protected void paintComponent(Graphics g) { 
		int w = getWidth(), h = getHeight();
		g.setColor(Color.white);
		g.fillRect(0, 0, w, h);
		
		int calls = callPaintable.size();
		int h4 = (int)Math.floor((double)h / (calls > 0 ? 6.0 : 4.0));
		int bottom = calls > 0 ? h4 * 2 : h4;
		
		int yarr = h-bottom;
		
		Graphics2D g2 = (Graphics2D)g;
		FontMetrics fm = g2.getFontMetrics();
		
		super.paintItem(g, 0, 0, w, yarr);

		//segPaintable.paintItem(g, 0, 0, w, yarr);
		//predictedPaintable.paintItem(g, 0, 0, w, yarr);
		fittedPaintable.paintItem(g, 0, 0, w, yarr);
		scalePaintable.paintItem(g, 0, 0, w, yarr);
		
		if(calls > 0) { 
			if(chrom != null) { 
				callPaintable.paintItem(g, 0, yarr+h4, w, h);
			} else { 
				callPaintable.paintItem(g, 0, yarr, w, h);				
			}
		} else { 
			//System.out.println("No calls to paint.");
		}
		
		String str;
		int strw = 0, strh = 0, strx = 0, stry = 0;
		
		if(probeMarking && nearModel != null && modelPoint != null) { 
			str = nearModel.toString();
			strw = fm.charsWidth(str.toCharArray(), 0, str.length());
			strh = fm.getHeight();
			
			strx = mousePoint.x;
			stry = mousePoint.y-fm.getDescent();
			
			strx -= Math.max(0, (strx+strw-w));
			stry = Math.max(stry, fm.getHeight());

			g.setColor(Color.white);
			g.fillRect(strx, stry-fm.getAscent(), strw, strh);

			g.setColor(Color.black);
			g.drawLine(mousePoint.x, mousePoint.y, modelPoint.x, modelPoint.y);
			g.drawString(str, strx, stry);
		}

		if(selectedSegmentModels != null && transcriptMarking) { 
			for(Pair<Rectangle,Model> rmp : selectedSegmentModels) { 
				Rectangle r = rmp.getFirst();
				Model m = rmp.getLast();

				str = m.toString();
				strw = fm.charsWidth(str.toCharArray(), 0, str.length());
				strh = fm.getHeight();

				strx = r.x;
				stry = r.y-fm.getDescent();

				strx -= Math.max(0, (strx+strw-w));
				stry = Math.max(stry, fm.getHeight());

				g.setColor(Color.white);
				g.fillRect(strx, stry-fm.getAscent(), strw, strh);

				g.setColor(Color.black);
				g.drawRect(r.x, r.y, r.width, r.height);
				g.drawLine(r.x, r.y, r.x+r.width, r.y+r.height);
				g.drawLine(r.x, r.y+r.height, r.x+r.width, r.y);
				g.drawString(str, strx, stry);
			}
		}
	}
	
	public static class Datapoint extends Model { 
		public Integer location;
		public Double value;
		
		public Datapoint(Integer loc, Double val) { 
			location = loc; value = val;
		}
	}
	
	public static class SegmentRange extends Model { 
		public Integer start, end;
		
		public SegmentRange(Segment s, Integer[] locs) { 
			start = locs[s.start]; 
			end = locs[s.end];
			
			//if(s.start > 0) { start -= (locs[s.start] - locs[s.start-1])/2; }
			//if(s.end < locs.length-1) { end += (locs[s.end+1] - locs[s.end])/2; }
			
			System.out.println(String.format("+Segment(%d,%d)", start, end));
		}
	}

	public static class FittedRange extends Model { 
		public Integer start, end;
		public Double[] params;
		public Integer type;
		public boolean shared;
		
		public FittedRange(Segment s, Integer[] locs, Double[] p, Integer t, boolean sh) { 
			if(s.start < 0 || s.end > locs.length) { 
				throw new IllegalArgumentException(
						String.format("%d,%d outside of %d array", s.start, s.end, locs.length));
			}
			
			start = locs[s.start]; 
			end = locs[s.end-1];
			params = p.clone();
			type = t;
			shared = sh;
		}
	}

	public Action createSnapshotAction() {
		return new AbstractAction("Snapshot...") {
			public void actionPerformed(ActionEvent e) {
				int w = getWidth(), h = getHeight();
				FileChooser chooser = new FileChooser(null);
				File f = chooser.chooseSave();
				if(f != null) { 
					nearModel = null; modelPoint = null;
		            BufferedImage im = 
		                new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		            Graphics g = im.getGraphics();
		            Graphics2D g2 = (Graphics2D)g;
		            g2.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
		            paintComponent(g);
		            try {
						ImageIO.write(im, "png", f);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			} 
		};
	}

	public void setTranscriptMarking(boolean value) {
		transcriptMarking = value;
		if(!transcriptMarking) { 
			selectedSegmentModels.clear();
		}
		repaint();
	}
	
	public void setProbeMarking(boolean value) {
		probeMarking = value;
		if(!probeMarking) { 
			modelPoint = mousePoint = null;
		}
		repaint();
	}
	
	public void findBestFit() {
		ArrayList<Call> callList = new ArrayList<Call>();
		
		FileInputData fileData = new FileInputData(data);
		InputSegmentation segmentation = new InputSegmentation(fileData, segments);

		if(segmentation != null) {
			
			// TODO: fix me.
			Collection<Cluster> clusters = null; // segmentation.clusters(channel);
			
			System.out.println(String.format("#Clusters: %d", clusters.size()));
			
			ArrayList<TAFit> fits = new ArrayList<TAFit>();

			for(Cluster c : clusters) { 
				TAFit fit = identifier.identify(c);
				fits.add(fit);
				
				int numCalls = fit.arrangement.calls.length;
				System.out.println(String.format("Cluster %d-%d -> %d segments -> %d calls",
						fileData.locations[c.segments[0].start], 
						fileData.locations[c.segments[c.segments.length-1].end],
						c.segments.length, 
						numCalls));

				/*
				for(int i = 0; i < fit.calls.calls.length; i++) { 
					TranscriptCall tc = fit.calls.calls[i];
					Double param = fit.params[i];
					int si = tc.start, ei = tc.end-1;
					int idx1 = c.segments[si].start, idx2 = c.segments[ei].end;
					int loc1 = fileData.locations[idx1]; 
					int loc2 = fileData.locations[idx2];
					System.out.println(String.format("\t%d-%d, %.3f", 
							loc1, loc2, param));
					callList.add(new TranscriptCall(loc1, loc2, param));
				}
				*/
			}
			
			//setCalls(callList);
			setFits(fits);
		}
	}
}
