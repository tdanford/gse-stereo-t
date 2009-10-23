/*
 * Author: tdanford
 * Date: May 5, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.figures;

import java.awt.*;
import java.util.*;

import edu.mit.csail.cgs.viz.paintable.*;

public class TwoArrowSchematic extends AbstractPaintable {
	
	public static void main(String[] args) { 
		Paintable p = new TranscriptSchematic(
				new ConvergentSchematic(0.5, 0.5, 0.25), null);
		PaintableFrame pf = new PaintableFrame("Schematic", p);
	}
	
	private boolean d1, d2;
	private double frac1, frac2, overfrac;
	private Color color;
	
	public Integer[] p1x, p2x;
	
	public TwoArrowSchematic(double f1, double f2, double over, boolean dl, boolean dr) { 
		frac1 = f1; 
		frac2 = f2; 
		overfrac = over;
		color = Color.blue;
		d1 = dl; 
		d2 = dr;
	}
	
	public void setColor(Color c) { 
		color = c;
		dispatchChangedEvent();
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2-x1, h = y2-y1;
		
		ArrowPainter p1 = new ArrowPainter(color, d1);
		ArrowPainter p2 = new ArrowPainter(color, d2);
		
		int p1w = (int)Math.round(frac1 * (double)w);
		int p2w = (int)Math.round(frac2 * (double)w);
		
		double totalfrac = Math.min(1.0, frac1+frac2-overfrac);
		int offset = (w - (int)Math.round(totalfrac * (double)w)) / 2;
		
		int p1x1 = offset, p1x2 = p1x1+p1w;
		int p2x1 = p1x2 - (int)Math.round(overfrac * (double)w);
		int p2x2 = p2x1 + p2w;
		
		int my = y1 + (h/2);
		p1x = new Integer[] { p1x1, p1x2, };
		p2x = new Integer[] { p2x1, p2x2, };
		
		p1.paintItem(g, p1x1, y1, p1x2, my);
		p2.paintItem(g, p2x1, my, p2x2, y2);
	}

}
