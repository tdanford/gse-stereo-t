/*
 * Author: tdanford
 * Date: Dec 20 2008
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.awt.*;
import java.lang.reflect.*;
import java.util.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.utils.Interval;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.ModelFieldAnalysis;
import edu.mit.csail.cgs.viz.NonOverlappingIntervalLayout;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.eye.*;
import edu.mit.csail.cgs.viz.paintable.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.general.ScoredPoint;
import edu.mit.csail.cgs.datasets.general.ScoredRegion;

/**
 * 
 * @author tdanford
 *
 */
public class ModelTranscriptCallPainter extends AbstractModelPaintable {

	public static final String boundsKey = "bounds";
	public static final String colorKey = "color";
	public static final String sharedColorKey = "shared-color";
	public static final String strokeKey = "stroke";
	public static final String axisColorKey = "axis-color";
	public static final String drawWeightsKey = "draw-weights";
	public static final String directionKey = "arrow-direction";
	public static final String scaleColorsKey = "scale-colors?";
	
	public static Map<Integer,Color> typeColors,sharedTypeColors;
	
	static { 
		typeColors = new HashMap<Integer,Color>();
		typeColors.put(Segment.FLAT, Coloring.clearer(Color.orange));
		typeColors.put(Segment.LINE, Coloring.clearer(Color.red));

		sharedTypeColors = new HashMap<Integer,Color>();
		sharedTypeColors.put(Segment.FLAT, Coloring.clearer(Color.yellow));
		sharedTypeColors.put(Segment.LINE, Coloring.clearer(Color.pink));
	}

	private String xFieldName, yFieldName;
	private NonOverlappingIntervalLayout<Model> regions;
	private Vector<Model> models;
	
	public ModelTranscriptCallPainter() { 
		xFieldName = "start";
		yFieldName = "end";
		
		regions = new NonOverlappingIntervalLayout<Model>();
		models = new Vector<Model>();
		
		Color transblue = Color.blue;
		transblue = Coloring.clearer(Coloring.clearer(transblue));
		
		initProperty(new PropertyValueWrapper<Integer[]>(boundsKey, new Integer[] { 0, 1 }));
		initProperty(new PropertyValueWrapper<Color>(colorKey, transblue));
		initProperty(new PropertyValueWrapper<Float>(strokeKey, (float)3.0));
		initProperty(new PropertyValueWrapper<Boolean>(drawWeightsKey, Boolean.FALSE));
		initProperty(new PropertyValueWrapper<Boolean>(directionKey, Boolean.TRUE));
		initProperty(new PropertyValueWrapper<Boolean>(scaleColorsKey, Boolean.TRUE));
		
		startDrawingPoints();
	}
	
	public ModelTranscriptCallPainter(String xfield, String yfield) { 
		this();
		xFieldName = xfield;
		yFieldName = yfield;
	}
	
	public void addModel(Model m) {
		Class modelClass = m.getClass();
		ModelFieldAnalysis analysis = new ModelFieldAnalysis(modelClass);

		Field xfield = analysis.findField(xFieldName);
		Field yfield = analysis.findField(yFieldName);

		if(xfield != null && yfield != null) { 
			try {
				Object xvalue = xfield.get(m);
				Object yvalue = yfield.get(m);
				
				if(xvalue != null && yvalue != null) { 
					Class xclass = xvalue.getClass();
					Class yclass = yvalue.getClass();
					
					if(!Model.isSubclass(xclass, Integer.class)) { 
						throw new IllegalArgumentException("Start value must be an Integer");
					}
					
					if(!Model.isSubclass(yclass, Integer.class)) { 
						throw new IllegalArgumentException("End value must be an Integer");
					}
					
					if(!Model.isSubclass(yclass, Integer.class)) { 
						throw new IllegalArgumentException("Type value must be an integer");
					}
					
					Integer xnumber = (Integer)xvalue;
					Integer ynumber = (Integer)yvalue;
					
					int x = xnumber.intValue();
					int y = ynumber.intValue();
					
					addCallValue(x, y, m);
					
				} else { 
					throw new IllegalArgumentException("location or value was null");
				}
				
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException("location or value field was inaccessible", e);
			}
		} else { 
			String msg = "No Fields:";
			if(xfield == null) { 
				msg += String.format(" %s", xFieldName);
			}
			if(yfield == null) { 
				msg += String.format(" %s", yFieldName);
			}
			throw new IllegalArgumentException(msg);
		}
	}
	
	private void addCallValue(int x, int y, Model m) {
		if(x > y) { throw new IllegalArgumentException(); }
		
		ModelPaintableProperty boundsProp = getProperty(boundsKey);
		Integer[] bounds = (Integer[])boundsProp.getValue();
		
		regions.addInterval(new Interval<Model>(x, y-1, m));
		models.add(m);

		if(x < bounds[0] || y > bounds[1]) {
			bounds[0] = Math.min(x, bounds[0]);
			bounds[1] = Math.max(y, bounds[1]);
			setProperty(new PropertyValueWrapper<Integer[]>(boundsKey, bounds));
		}

		dispatchChangedEvent();
	}
	
	public int size() { 
		return models.size();
	}

	public void addModels(Iterator<? extends Model> itr) {
		while(itr.hasNext()) { 
			addModel(itr.next());
		}
	}

	public void clearModels() {
		regions.clear();
		models.clear();
		
		dispatchChangedEvent();
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		Integer[] bounds = getPropertyValue(boundsKey);
		Color color = getPropertyValue(colorKey, Color.red);
		float stroke = getPropertyValue(strokeKey, (float)1.0);
		int strokeWidth = Math.max(1, (int)Math.floor(stroke));
		Color axisColor = getPropertyValue(axisColorKey, Color.black);
		Boolean drawWeights = getPropertyValue(drawWeightsKey); 
		Boolean direction = getPropertyValue(directionKey); 
		Boolean scaleColors = getPropertyValue(scaleColorsKey); 
		
		ArrowPainter arrower = new ArrowPainter(color, direction);
		int length = Math.max(1, bounds[1] - bounds[0] + 1);
		
		int w = x2-x1, h = y2-y1;
		
		Graphics2D g2 = (Graphics2D)g;
		Stroke oldStroke = g2.getStroke();
		g2.setStroke(new BasicStroke(stroke));

		Iterator<Interval> intervals = regions.iterator();
		int tracks = Math.max(1, regions.getNumTracks());
		
		int trackHeight = (int)Math.floor((double)h / (double)tracks);
		
		clearDrawnPoints();
		
		while(intervals.hasNext()) { 
			Interval<Model> intv = intervals.next();
			int track = regions.getTrack(intv);
			
			int iy1 = y1 + trackHeight*track;
			int iy2 = iy1 + trackHeight;
			
			double xf1 = (double)(intv.start - bounds[0]) / (double)length;
			double xf2 = (double)(intv.end - bounds[0]) / (double)length;
			
			int ix1 = x1 + (int)Math.round(xf1 * (double)w); 
			int ix2 = x1 + (int)Math.round(xf2 * (double)w) + 1;
			
			arrower.paintArrow((Graphics2D)g, ix1, iy1, ix2, iy2);

			Rectangle rect = new Rectangle(ix1, iy1, ix2-ix1, iy2-iy1);
			drawRect(rect, intv.data);
		}
		
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(oldStroke);
	}
	
	public static class CallModel extends Model {
		
		public Integer start, end, length;
		public Boolean inRibBand;
		
		public CallModel(int s, int e) { 
			start = s; end = e;
			length = end-start+1;
			
			inRibBand = (length >= 1400 && length <= 1800) ||
				(length >= 2500 && length <= 3500);
		}
	}
}


