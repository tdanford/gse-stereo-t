/*
 * Author: tdanford
 * Date: Jul 17, 2008
 */
package edu.mit.csail.cgs.sigma.blots;

import java.util.*;
import java.awt.*;

import edu.mit.csail.cgs.viz.paintable.*;

public class ArtificialBlot extends AbstractPaintable {
	
	private int lanes;
	private double mean, spread;
	
	private Vector<Double>[] contents;
	
	public ArtificialBlot(int numlanes, double m, double s) { 
		lanes = numlanes; 
		mean = m; 
		spread = s;
		contents = new Vector[lanes];
	}
	
	public void setLaneContents(int lane, double[] vs) { 
		contents[lane].clear();
		for(int i = 0; i < vs.length; i++) { 
			contents[lane].add(vs[i]);
		}
		dispatchChangedEvent();
	}
	
	public void addLaneContents(int lane, double value) { 
		contents[lane].add(value);
		dispatchChangedEvent();
	}
	
	public void clearLane(int lane) { 
		contents[lane].clear();
		dispatchChangedEvent();
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2-x1, h = y2-y1;
		int laneWidth = w / ((lanes*2)+1);
		
		g.setColor(Color.white);
		g.fillRect(x1, y1, w, h);
		
		for(int y = 0; y < h; y++) { 
			double f = (double)y / (double)h;
			double length = offsetToLength(f);
			int lx = laneWidth;
			int rx = laneWidth+lx;
			
			for(int i = 0; i < lanes; i++) {
				double intensity = Math.min(1.0, laneIntensity(i, length));
				
				int colorValue = (int)Math.floor(intensity*254.0);
				Color c = new Color(colorValue, colorValue, colorValue);
				g.setColor(c);
				g.drawLine(lx, y, rx, y);
				
				lx += (laneWidth*2);
				rx = lx + laneWidth;
			}
		}
	}
	
	public double laneIntensity(int lane, double length) { 
		double sum = 0.0;
		for(Double base : contents[lane]) { 
			sum += lengthFalloff(lane, length, base);
		}
		return sum;
	}
	
	public double lengthFalloff(int lane, double len, double base) {
		double diff = len - base;
		double diffsq = diff*diff;
		double falloff = Math.exp(-diffsq);
		
		return falloff / (double)Math.max(1, contents[lane].size());
	}
	
	public double offsetToLength(double offset) { 
		double value = 1.0 / offset;
		value = value - 1.0;
		value = Math.log(value);
		value = -value;
		value *= spread;
		value += mean;
		return value;
	}

	public double lengthToOffset(double length) { 
		double coeff = (length-mean)/spread;
		return 1.0 / (1.0 + Math.exp(-coeff));
	}
}
