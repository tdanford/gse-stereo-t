/*
 * Author: tdanford
 * Date: Sep 16, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.viz;

import java.awt.*;
import java.lang.reflect.*;
import java.util.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.ModelFieldAnalysis;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.eye.*;
import edu.mit.csail.cgs.viz.paintable.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.general.ScoredPoint;
import edu.mit.csail.cgs.datasets.general.ScoredRegion;

/**
 * This is a visualization component.  
 * 
 * It's the piece of code which draws colored blocks and lines that correspond to 
 * fitted segments.  
 * 
 * @author tdanford
 *
 */
public class ModelSegmentValues extends AbstractModelPaintable {

	public static final String boundsKey = "bounds";
	public static final String scaleKey = "scale";
	public static final String colorKey = "color";
	public static final String sharedColorKey = "shared-color";
	public static final String strokeKey = "stroke";
	public static final String axisColorKey = "axis-color";
	public static final String drawWeightsKey = "draw-weights";
	public static final String drawTrapezoids = "draw-trapezoids";
	
	public static Map<Integer,Color> typeColors,sharedTypeColors;
	
	static { 
		typeColors = new HashMap<Integer,Color>();
		//typeColors.put(Segment.FLAT, Coloring.clearer(Color.orange));
		//typeColors.put(Segment.LINE, Coloring.clearer(Color.green));
		//typeColors.put(Segment.FLAT, Coloring.clearer(Color.orange));
		typeColors.put(Segment.FLAT, new Color(255, 255, 255, 0));
		typeColors.put(Segment.LINE, Coloring.clearer(Color.blue.darker()));

		sharedTypeColors = new HashMap<Integer,Color>();
		//sharedTypeColors.put(Segment.FLAT, Coloring.clearer(Color.yellow));
		sharedTypeColors.put(Segment.FLAT, new Color(255, 255, 255, 0));
		//sharedTypeColors.put(Segment.FLAT, Coloring.clearer(Color.orange));
		sharedTypeColors.put(Segment.LINE, Coloring.clearer(Color.green.brighter()));
	}
	

	private String xFieldName, yFieldName, scoreFieldName, typeFieldName, sharedFieldName;
	private Vector<Pair<Integer,Integer>> ranges;
	private Vector<Double[]> scores;
	private Vector<Integer> types;
	private Vector<Boolean> shared;
	private Vector<Model> models;
	private Color segmentColor;
	
	public ModelSegmentValues() { 
		xFieldName = "start";
		yFieldName = "end";
		typeFieldName = "type";
		scoreFieldName = "params";
		sharedFieldName = "shared";
		segmentColor = null;
		
		ranges = new Vector<Pair<Integer,Integer>>();
		scores = new Vector<Double[]>();
		types = new Vector<Integer>();
		shared = new Vector<Boolean>();
		models = new Vector<Model>();
		
		Color transblue = Color.blue;
		transblue = Coloring.clearer(Coloring.clearer(transblue));
		
		initProperty(new PropertyValueWrapper<Integer[]>(boundsKey, new Integer[] { 0, 1 }));
		initProperty(new PropertyValueWrapper<PaintableScale>(scaleKey, new PaintableScale(0.0, 1.0)));
		initProperty(new PropertyValueWrapper<Color>(colorKey, transblue));
		initProperty(new PropertyValueWrapper<Float>(strokeKey, (float)3.0));
		initProperty(new PropertyValueWrapper<Boolean>(drawWeightsKey, Boolean.FALSE));
		initProperty(new PropertyValueWrapper<Boolean>(drawTrapezoids, Boolean.TRUE));
		
		startDrawingPoints();
	}
	
	public void setXFieldName(String n) { xFieldName = n; }
	public void setYFieldName(String n) { xFieldName = n; }
	public void setTypeFieldName(String n) { typeFieldName = n; }
	public void setScoreFieldName(String n) { scoreFieldName = n; }
	public void setSharedFieldName(String n) { sharedFieldName = n; }
	
	public ModelSegmentValues(String xfield, String yfield, String typeField, String scoreField) { 
		this();
		xFieldName = xfield;
		yFieldName = yfield;
		typeFieldName = typeField;
		scoreFieldName = scoreField;
	}
	
	public void setBounds(int start, int end) { 
		PropertyValueWrapper<Integer[]> wrapper = getProperty(boundsKey);
		wrapper.setValue(new Integer[] { start, end });
	}
	
	public void setSegmentColor(Color c) { 
		segmentColor = c;
	}
	
	public void addModel(Model m) {
		Class modelClass = m.getClass();
		ModelFieldAnalysis analysis = new ModelFieldAnalysis(modelClass);

		Field xfield = analysis.findField(xFieldName);
		Field yfield = analysis.findField(yFieldName);
		Field typefield = analysis.findField(typeFieldName);
		Field scorefield = analysis.findField(scoreFieldName);
		Field sharedfield = analysis.findField(sharedFieldName);

		if(xfield != null && yfield != null && scorefield != null && typefield != null && sharedfield != null) { 
			try {
				Object xvalue = xfield.get(m);
				Object yvalue = yfield.get(m);
				Object svalue = scorefield.get(m);
				Object tvalue = typefield.get(m);
				Object shvalue = sharedfield.get(m);
				
				if(xvalue != null && yvalue != null && svalue != null && tvalue != null) { 
					Class xclass = xvalue.getClass();
					Class yclass = yvalue.getClass();
					Class sclass = svalue.getClass();
					Class tclass = tvalue.getClass();
					Class shclass = shvalue.getClass();
					
					if(!Model.isSubclass(xclass, Integer.class)) { 
						throw new IllegalArgumentException("Start value must be an Integer");
					}
					
					if(!Model.isSubclass(yclass, Integer.class)) { 
						throw new IllegalArgumentException("End value must be an Integer");
					}
					
					if(!Model.isSubclass(yclass, Integer.class)) { 
						throw new IllegalArgumentException("Type value must be an integer");
					}
					
					if(!Model.isSubclass(sclass, Double[].class)) { 
						throw new IllegalArgumentException("Score value must be a Double array.");
					}

					if(!Model.isSubclass(tclass, Integer.class)) { 
						throw new IllegalArgumentException("Type value must be an Integer.");
					}

					if(!Model.isSubclass(shclass, Boolean.class)) { 
						throw new IllegalArgumentException("Shared value must be a Boolean.");
					}

					Integer xnumber = (Integer)xvalue;
					Integer ynumber = (Integer)yvalue;
					Integer type = (Integer)tvalue;
					Double[] snumber = (Double[])svalue;
					Boolean share = (Boolean)shvalue;
					
					int x = xnumber.intValue();
					int y = ynumber.intValue();
					Double[] sc = snumber;
					
					addScoredRangeValue(x, y, sc, type, share, m);
					
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
			if(scorefield == null) { 
				msg += String.format(" %s", scoreFieldName);
			}
			if(typefield == null) { 
				msg += String.format(" %s", typeFieldName);
			}
			if(sharedfield == null) { 
				msg += String.format(" %s", sharedFieldName);
			}
			throw new IllegalArgumentException(msg);
		}
	}
	
	private void addScoredRangeValue(int x, int y, Double[] s, Integer type, boolean sh, Model m) {
		if(x > y) { throw new IllegalArgumentException(); }
		
		PaintableScale scale = getPropertyValue(scaleKey);
		
		ModelPaintableProperty boundsProp = getProperty(boundsKey);
		Integer[] bounds = (Integer[])boundsProp.getValue();
		
		ranges.add(new Pair<Integer,Integer>(x, y));
		scores.add(s);
		types.add(type);
		models.add(m);
		shared.add(sh);

		if(x < bounds[0] || y > bounds[1]) {
			bounds[0] = Math.min(x, bounds[0]);
			bounds[1] = Math.max(y, bounds[1]);
			setProperty(new PropertyValueWrapper<Integer[]>(boundsKey, bounds));
		}

		if(type == Segment.LINE) { 
			Double maxScore = s[0] + s[1];
			Double minScore = s[0];

			scale.updateScale(maxScore);
			scale.updateScale(minScore);
			
		} else if(type == Segment.FLAT) { 
			scale.updateScale(s[0]);
		}
		
		dispatchChangedEvent();
	}

	public void addModels(Iterator<? extends Model> itr) {
		while(itr.hasNext()) { 
			addModel(itr.next());
		}
	}

	public void clearModels() {
		ranges.clear();
		models.clear();
		shared.clear();
		scores.clear();
		types.clear();
		
		dispatchChangedEvent();
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		Integer[] bounds = getPropertyValue(boundsKey);
		Color color = getPropertyValue(colorKey, Color.red);
		float stroke = getPropertyValue(strokeKey, (float)1.0);
		int strokeWidth = Math.max(1, (int)Math.floor(stroke));
		Color axisColor = getPropertyValue(axisColorKey, Color.black);
		PaintableScale yScale = getPropertyValue(scaleKey);
		Boolean drawWeights = getPropertyValue(drawWeightsKey);
		Boolean isDrawingTrapezoids = getPropertyValue(drawTrapezoids);
		
		int length = Math.max(1, bounds[1] - bounds[0] + 1);
		
		int w = x2-x1, h = y2-y1;
		
		Graphics2D g2 = (Graphics2D)g;
		Stroke oldStroke = g2.getStroke();
		g2.setStroke(new BasicStroke(stroke));
		
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		/** Painting Code **/
		
		// Axes
		//g2.setColor(axisColor);
		//g2.drawRect(x1, y1, w-1, h-1);
		
		int wArea = w - strokeWidth*2;
		
		clearDrawnPoints();
		
		// Points
		for(int i = 0; i < ranges.size(); i++) {
			Model m = models.get(i);
			Pair<Integer,Integer> r = ranges.get(i);
			Double[] params = scores.get(i);
			int type = types.get(i);
			boolean share = shared.get(i);
			
			int start = r.getFirst();
			int end = r.getLast();
			//System.out.println(String.format("=> %d, %d : %.3f", start, end, score));
			
			double xf = (double)(start-bounds[0]) / (double)length; 
			double yf = (double)(end-bounds[0]+1) / (double)length;
			
			double zf1 = yScale.fractionalOffset(params[0]);
			
			int px = x1 + (int)Math.round(xf * (double)wArea);
			int py = x1 + (int)Math.round(yf * (double)wArea);
			int pz1 = y2 - (int)Math.round(zf1 * (double)h);
			int pz2 = pz1;

			if(type == Segment.LINE) { 
				double intercept = params[0];
				double slope = params[1];
				double zf2 = yScale.fractionalOffset(intercept + (end-start+1)*slope);
				pz2 = y2 - (int)Math.round(zf2 * (double)h);
			}
			
			int[] sx = new int[] { px, px, py, py };
			int[] sy = new int[] { y2, pz1, pz2, y2 };

			if(segmentColor == null) { 
				if(share){ 
					g2.setColor(sharedTypeColors.containsKey(type) ? 
							sharedTypeColors.get(type) : Coloring.clearer(Color.gray));				
				} else { 
					g2.setColor(typeColors.containsKey(type) ? 
							typeColors.get(type) : Coloring.clearer(Color.gray));
				}
			} else { 
				g2.setColor(segmentColor);
			}

			if(isDrawingTrapezoids) {
				
				g2.fillPolygon(sx, sy, 4);
				
			} else { 

				/*
				if(share) { 
					g2.setColor(Color.gray);
				} else { 
					g2.setColor(Color.black);				
				}
				*/
				g2.drawLine(px, pz1, py, pz2);
			}
			
			if(share) { 
				g2.setColor(Coloring.clearer(Color.blue));		
				g2.fillRect(px, y2-10, (py-px), 10);
			}

			if(m != null) { 
				int minpz = Math.min(pz1, pz2);
				Rectangle rect = 
					new Rectangle(px, minpz, py-px, y2-minpz);
				drawRect(rect, m);
			} 

			/*
			if(drawWeights) { 
				String weightString = "";
				for(int k = 0; k < params.length; k++) { 
					if(k > 0) { weightString+=","; }
					weightString += String.format("%.2f", params[k]);
				}
				//g2.drawString(weightString, px+(py-px)/3, y2-1);
				g2.drawString(weightString, py+1, pz2);
			}
			*/
		}
		
		g2.setStroke(oldStroke);
	}
	
	public static class ScoredRangeModel extends Model { 
		public Integer start, end;
		public Double score;
		public Integer type;
		
		public ScoredRangeModel(int s, int e, double ss, int t) { 
			start = s; end = e;
			score = ss;
			type = t;
		}
	}
}


