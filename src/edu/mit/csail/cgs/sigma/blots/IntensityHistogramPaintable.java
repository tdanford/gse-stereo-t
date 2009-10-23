/*
 * Author: tdanford
 * Date: Jun 14, 2008
 */
/**
 * 
 */
package edu.mit.csail.cgs.sigma.blots;

import java.awt.*;
import edu.mit.csail.cgs.viz.paintable.*;

/**
 * @author tdanford
 *
 */
public class IntensityHistogramPaintable extends AbstractPaintable {
	
	private IntensityHistogram hist;
	
	public IntensityHistogramPaintable(IntensityHistogram h) { 
		hist = h;
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.viz.paintable.AbstractPaintable#paintItem(java.awt.Graphics, int, int, int, int)
	 */
	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2 - x1, h = y2 - y1;
		g.setColor(Color.blue);
		g.drawRect(x1, y1, w, h);
		double dmax = (double)Math.max(1, hist.max()); 

		if(w >= hist.getLength()) { 
			int pixelsPerCount = Math.max(1, w/(hist.getLength() + 1));

			g.setColor(Color.black);

			for(int i = 0; i < hist.getLength(); i++) { 
				int x = x1 + pixelsPerCount * (i + 1);
				double f = (double)hist.getCount(i) / dmax;
				int y = y2 - (int)Math.round((double)h * f);

				g.drawLine(x, y2, x, y);
				g.fillOval(x-2,y-2,4,4);
			}
		} else { 
			int countsPerPixel = Math.max(1, hist.getLength()/w);
			g.setColor(Color.black);
			
			int[] pix = new int[w];
			int maxSum = 0;
			
			int idx = 0;
			for(int i = 0; i < w; i++) { 
				int sum = 0;
				for(int j = 0; j < countsPerPixel; j++) { 
					sum += hist.getCount(idx+i);
				}
				idx += countsPerPixel;
				pix[i] = sum;
				maxSum = Math.max(sum, maxSum);
			}
			
			for(int i = 0; i < w; i++) { 
				double f = (double)pix[i] / (double)maxSum;
				int x = x1 + i;
				int y = y2 - (int)Math.round(f * (double)h);
				g.drawLine(x, y2, x, y);
			}
		}
	}

}
