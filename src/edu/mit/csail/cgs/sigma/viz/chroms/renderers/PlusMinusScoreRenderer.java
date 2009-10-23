/*
 * Author: tdanford
 * Date: Feb 19, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import edu.mit.csail.cgs.sigma.viz.chroms.*;

public class PlusMinusScoreRenderer implements ChromInformationRenderer<ChromScorer> {
	
	private Color minusColor, plusColor;
	private boolean flipped;
	
	public PlusMinusScoreRenderer(Color pc, Color mc, boolean flip) { 
		plusColor = pc;
		minusColor = mc;
		flipped = flip;
	}

	public void paintInformation(ChromScorer s,  
			Graphics2D g2, int x1, int y1, int x2, int y2) {
		
		int trackHeight = y2-y1;
		int chromPixWidth = x2-x1;

		g2.setStroke(new BasicStroke((float)1.0));
		
		s.recalculate(chromPixWidth);
		Double max = s.max();

		int baseline = y1 + (flipped ? 0 : trackHeight);

		Double zero = s.zero();
		int zy = baseline;
		
		if(zero != null) { 
			double f = (double)zero / (double)max;
			int dy = (int)Math.round(f * (double)trackHeight);

			zy += (flipped ? dy : -dy);

			g2.setColor(Color.gray);
			//g2.drawLine(x1, zy, x1+chromPixWidth, zy);
		}

		int ppx = -1, ppy = -1, pdy = 0;
		Double pvalue = null;
		
		for(int j = 0; j < chromPixWidth; j++) { 
			Double value = s.value(j);
			int px = -1, py = -1, dy = 0;

			if(value != null) { 
				px = x1 + j;
				py = baseline;

				if(value != null) {
					double f = (double)value / (double)max;
					dy = (int)Math.round(f * (double)trackHeight);
					py += (!flipped ? -dy : dy);

					if(pvalue != null) {
						boolean lastUp = pvalue >= zero;
						boolean thisUp = value >= zero;
						boolean sameSign = (lastUp == thisUp);

						if(py == zy) { 
							g2.setColor(Color.gray);
						} else { 
							if(pvalue >= zero) {  
								g2.setColor(plusColor);
							} else { 
								g2.setColor(minusColor);
							}
						}

						g2.drawLine(px, py, px, zy);
					}
				}
			}

			ppx = px; ppy = py; pdy = dy;
			pvalue = value;
		}
	} 
}
