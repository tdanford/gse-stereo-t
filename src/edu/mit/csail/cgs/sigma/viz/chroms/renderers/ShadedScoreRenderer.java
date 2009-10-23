/*
 * Author: tdanford
 * Date: Feb 19, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms.renderers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Collection;

import edu.mit.csail.cgs.sigma.viz.chroms.*;

public class ShadedScoreRenderer 
	implements ChromInformationRenderer<ChromScorer> {

	private Color color;
	
	public ShadedScoreRenderer(Color c) {
		color = c;
	}
	
	public ShadedScoreRenderer() { this(Color.red); }

	public void paintInformation(ChromScorer cp, 
			Graphics2D g2, int x1, int y1, int x2, int y2) {

		int w = x2-x1, h = y2-y1;

		cp.recalculate(w);
		
		double zero = cp.zero(), max = cp.max();

		for(int i = 0; i < w; i++) { 
			int x = x1+i;
			double score = cp.value(i);
			int y = y2-h;
			
			double f = (score - zero) / (max-zero);
			
			Color c = scale(color, f);
			g2.setColor(c);
			g2.drawLine(x, y, x, y+h);
		}
	}
	
	private Color scale(Color c, double f) { 
		int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();
		r = (int)Math.floor(f * (double)r + (1.0-f) * 255.0);
		g = (int)Math.floor(f * (double)g + (1.0-f) * 255.0);
		b = (int)Math.floor(f * (double)b + (1.0-f) * 255.0);
		return new Color(r, g, b, a);
	}

}