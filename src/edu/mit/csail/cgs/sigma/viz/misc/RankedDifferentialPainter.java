/*
 * Author: tdanford
 * Date: Nov 4, 2008
 */
package edu.mit.csail.cgs.sigma.viz.misc;

import java.awt.*;
import java.util.*;
import java.lang.reflect.*;

import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.viz.eye.*;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;

public class RankedDifferentialPainter extends AbstractModelPaintable {
	
	public static final String scaleKey = "scale";
	public static final String quantileKey = "quantile";
	
	private String fieldName;
	private TreeSet<ModelWrapper> sorted;
	
	public RankedDifferentialPainter() { 
		this("differential");
	}
	
	public RankedDifferentialPainter(String fieldName) { 
		this.fieldName = fieldName;
		sorted = new TreeSet<ModelWrapper>();
		initProperty(new PropertyValueWrapper<PaintableScale>(scaleKey, new PaintableScale(-1.0, 1.0)));
		initProperty(new PropertyValueWrapper<Double>(quantileKey, 0.1));
	}

	public void addModel(Model m) {
		try {
			PaintableScale scale = getPropertyValue(scaleKey);
			ModelWrapper w = new ModelWrapper(m);
			sorted.add(w);
			scale.updateScale(w.value);
			dispatchChangedEvent();
		} catch (NoSuchFieldException e) {
			throw new IllegalArgumentException(fieldName, e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(fieldName, e);
		}
	}

	public void addModels(Iterator<? extends Model> itr) {
		setEventPassthrough(false);
		while(itr.hasNext()) { 
			addModel(itr.next());
		}
		dispatchChangedEvent();
		setEventPassthrough(true);
	}

	public void clearModels() {
		sorted.clear();
		setProperty(new PropertyValueWrapper<PaintableScale>(scaleKey, new PaintableScale(-1.0, 1.0)));
		dispatchChangedEvent();
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		PaintableScale scale = getPropertyValue(scaleKey);
		Double quantile = getPropertyValue(quantileKey);
		
		int w = x2-x1, h = y2-y1;
		
		int zeroy = y2-(int)Math.round(scale.fractionalOffset(0.0) * (double)h);
		
		g.setColor(Color.white);
		g.fillRect(x1, y1, w, h);
		
		g.setColor(Color.black);
		g.drawLine(x1, zeroy, x2, zeroy);
		
		int radius = 2;
		int diam = radius*2;
		
		int size= sorted.size()+1;
		int i = 0;
		
		double sum = 0.0;
		
		int upperQuantile = (int)Math.floor(quantile * (double)sorted.size());
		int lowerQuantile = (int)Math.floor((1.0-quantile) * (double)sorted.size());
		Double upperQuantileValue = null, lowerQuantileValue = null;
		
		for(ModelWrapper wrapper : sorted) {
			i += 1;
			
			if(i == upperQuantile) { upperQuantileValue = wrapper.value; }
			if(i == lowerQuantile) { lowerQuantileValue = wrapper.value; }
			
			int y = y2-(int)Math.round(scale.fractionalOffset(wrapper.value) * (double)h);
			int x = x1 + (int)Math.round(((double)i / (double)size) * (double)w);
			sum += wrapper.value;
			
			g.setColor(Color.red);
			g.drawOval(x-radius, y-radius, diam, diam);
		}
		
		sum /= Math.max(1, (double)sorted.size());
		g.setColor(Color.blue);
		int meany = y2- (int)Math.round(scale.fractionalOffset(sum) * (double)h);
		g.drawLine(x1, meany, x2, meany);
		g.drawString(String.format("%.2f", sum), x1+2, meany-1);
		
		if(upperQuantileValue != null) { 
			int qy = y2 - (int)Math.round(scale.fractionalOffset(upperQuantileValue) * (double)h);
			int qx = x1 + (int)Math.round(((double)upperQuantile / (double)sorted.size()) * (double)w);
			g.setColor(Color.red);
			g.drawLine(qx, zeroy, qx, qy);
			g.drawString(String.format("%.2f", upperQuantileValue), qx+2, zeroy-1);
		}
		if(lowerQuantileValue != null) { 
			int qy = y2 - (int)Math.round(scale.fractionalOffset(lowerQuantileValue) * (double)h);
			int qx = x1 + (int)Math.round(((double)lowerQuantile / (double)sorted.size()) * (double)w);
			g.setColor(Color.red);
			g.drawLine(qx, zeroy, qx, qy);
			g.drawString(String.format("%.2f", lowerQuantileValue), qx+2, zeroy-1);
		}
	}

	private class ModelWrapper implements Comparable<ModelWrapper> {
		
		public Model model;
		public Double value;
		
		public ModelWrapper(Model m) throws NoSuchFieldException, IllegalAccessException { 
			model = m;
			Field f = m.getClass().getField(fieldName);
			Object val = f.get(m);
			if(!(val instanceof Number)) { throw new IllegalArgumentException(val.toString()); }
			value = ((Number)val).doubleValue();
		}
		
		public int compareTo(ModelWrapper w) { 
			if(value > w.value) { return -1; }
			if(value < w.value) { return 1; }
			return 0;
		}
		
		public int hashCode() { 
			return model.hashCode();
		}
		
		public boolean equals(Object o) { 
			if(!(o instanceof ModelWrapper)) { return false; }
			ModelWrapper w = (ModelWrapper)o;
			return model.equals(w.model);
		}
	}
}
