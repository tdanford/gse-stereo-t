/*
 * Author: tdanford
 * Date: Feb 19, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms.renderers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Collection;

import edu.mit.csail.cgs.sigma.viz.chroms.*;

public class DefaultPainterRenderer implements ChromInformationRenderer<ChromPainter> {
	
	private Color color;
	
	public DefaultPainterRenderer(Color c) { 
		color = c;
	}

	public void paintInformation(ChromPainter cp, 
			Graphics2D g2, int x1, int y1, int x2, int y2) {

		g2.setColor(color);
		int w = x2-x1, h = y2-y1;

		cp.recalculate(w);

		for(int i = 0; i < w; i++) { 
			int x = x1+i;
			if(cp.value(i)) { 
				g2.drawLine(x, y1, x, y2);
			}
		}
	} 
}