/*
 * Author: tdanford
 * Date: Feb 19, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms.renderers;

import java.awt.Color;
import java.awt.Graphics2D;

import edu.mit.csail.cgs.sigma.viz.chroms.ChromInformationRenderer;
import edu.mit.csail.cgs.sigma.viz.chroms.ChromScorer;

public class OneColorScoreRenderer implements ChromInformationRenderer<ChromScorer> {
	
	private Color scoreColor;
	private boolean flipped;
	
	public OneColorScoreRenderer(Color c, boolean flip) { 
		scoreColor = c;
		flipped = flip;
	}

	public void paintInformation(ChromScorer s,  
			Graphics2D g2, int x1, int y1, int x2, int y2) {
		
		int trackHeight = y2-y1;
		int chromPixWidth = x2-x1;

		s.recalculate(chromPixWidth);
		Double max = s.max();

		int ppx = -1, ppy = -1;
		int baseline = y2;

		Double zero = s.zero(); 
		double f = 1.0;
		
		//System.out.println(String.format("zero/max: %.2f / %.2f", zero, max));
		
		if(zero != null) { 
			f = (double)zero / (double)max;
			int zy = baseline;
			int dy = (int)Math.round(f * (double)trackHeight);

			zy -= dy;

			g2.setColor(Color.gray);
			//g2.drawLine(x1, zy, x1+chromPixWidth, zy);
		}

		for(int j = 0; j < chromPixWidth; j++) { 
			Double value = s.value(j);
			int px = -1, py = -1;

			if(value != null) { 
				px = flipped ? x2 - j : x1 + j;
				py = baseline;

				f = (double)(value - zero) / (double)(max - zero);
				int dy = (int)Math.floor(f * (double)trackHeight);
				if(f > 0.0) { dy = Math.max(dy, 1); }

				//System.out.println(String.format("\t%.2f -> %d / %d", f, dy, trackHeight));

				py -= dy;

				if(ppx != -1 && py != -1) {
					//g2.setColor(scale(scoreColor, f));
					g2.setColor(scoreColor);
					if(py != baseline || ppy != baseline) { 
						g2.drawLine(ppx, ppy, px, py);
					}
				}
			}

			ppx = px; ppy = py;
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