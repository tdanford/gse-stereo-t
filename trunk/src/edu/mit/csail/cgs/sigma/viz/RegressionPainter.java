package edu.mit.csail.cgs.sigma.viz;

import java.awt.*;
import java.util.*;

import edu.mit.csail.cgs.viz.paintable.*;
import edu.mit.csail.cgs.utils.Pair;

public class RegressionPainter extends AbstractPaintable {
	
	private Vector<Pair<Double,Double>> pts;
	private double[] line;
	private double[] bounds;
	private double range;
	
	public RegressionPainter(Vector<Pair<Double,Double>> _pts, double[] _line) { 
		pts = new Vector<Pair<Double,Double>>(_pts);
		line = _line.clone();
		bounds = new double[2];
		bounds[0] = 0.0;
		bounds[1] = 1.0;
		
		for(Pair<Double,Double> pt : pts) { 
			bounds[0] = Math.min(pt.getFirst(), bounds[0]);
			bounds[0] = Math.min(pt.getLast(), bounds[0]);
			bounds[1] = Math.max(pt.getFirst(), bounds[1]);
			bounds[1] = Math.max(pt.getLast(), bounds[1]);
		}
		range = bounds[1] - bounds[0];
	}
	
	public int valueToPix(double val, int size) { 
		double frac = (val-bounds[0]) / range;
		return (int)Math.round(frac * (double)size);
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2 - x1, h = y2 - y1;
		int pointRadius = 2;
		int pointDiam = pointRadius*2;
		
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.setColor(Color.white);
		g.fillRect(x1, y1, w, h);
		
		g.setColor(Color.black);
		g.drawRect(x1, y1, w, h);
		
		Stroke oldStroke = g2.getStroke();
		g2.setStroke(new BasicStroke((float)2.0));
		
		g.setColor(Color.red);
		for(Pair<Double,Double> pt : pts) { 
			double x = pt.getFirst(), y = pt.getLast();
			int px = valueToPix(x, w), py = valueToPix(y, h);
			
			g.drawOval(x1+px-pointRadius, y2-py-pointRadius, pointDiam, pointDiam);
		}
		
		double b = line[0], m = line[1];
		double ly1 = m*bounds[0] + b, ly2 = m*bounds[1]+b;
		int lpy1 = valueToPix(ly1, h), lpy2 = valueToPix(ly2, h);
		
		g.setColor(Color.blue);
		g2.setStroke(oldStroke);
		g.drawLine(x1, lpy1, x2, lpy2);
		
		g2.setStroke(oldStroke);
	}

}
