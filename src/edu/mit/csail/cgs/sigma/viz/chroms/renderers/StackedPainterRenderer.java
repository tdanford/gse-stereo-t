/*
 * Author: tdanford
 * Date: Feb 19, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms.renderers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Collection;

import edu.mit.csail.cgs.sigma.viz.chroms.*;

public class StackedPainterRenderer 
	implements ChromInformationRenderer<MultiChromPainter> {
	
	private Color[] color;
	
	public StackedPainterRenderer() { 
		color = new Color[] { 
				Color.red, Color.blue, Color.orange, 
				Color.green, Color.magenta, Color.cyan };
	}

	public void paintInformation(MultiChromPainter cp, 
			Graphics2D g2, int x1, int y1, int x2, int y2) {

		int w = x2-x1, h = y2-y1;

		cp.recalculate(w);
		int tracks = cp.height();
		int trackHeight = (int)Math.round((double)h / (double)Math.max(1, tracks));

		for(int i = 0; i < w; i++) { 
			int x = x1+i;
			boolean[] v = cp.value(i);
			int y = y2-trackHeight;
			for(int j = 0; j < tracks; j++) { 
				if(v[j]) { 
					Color c = color[j];
					g2.setColor(c);
					g2.drawLine(x, y, x, y+trackHeight);
					y -= trackHeight;
				}
			}
		}
	} 
}