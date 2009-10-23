/*
 * Author: tdanford
 * Date: Feb 19, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms.renderers;

import java.awt.*;
import java.util.*;
import edu.mit.csail.cgs.sigma.viz.chroms.*;

public class DefaultLocationSetRenderer implements ChromInformationRenderer<ChromLocationSets> {
	
	private Color color;
	
	public DefaultLocationSetRenderer() { this(Color.black); }
	
	public DefaultLocationSetRenderer(Color c) { 
		color = c;
	}

	public void paintInformation(ChromLocationSets info, Graphics2D g2, int x1, int y1, int x2, int y2) {
		g2.setColor(color);
		int w = x2-x1, h = y2-y1;
		int h2 = h/2;
		int h4 = h/4;

		info.recalculate(w);

		Iterator<Integer[]> locations = info.locations();
		int length = info.length();
		
		while(locations.hasNext()) { 
			Integer[] locs = locations.next();

			double locf1 = (double)locs[0] / (double)length;
			double locf2 = (double)locs[1] / (double)length;
			
			int px1 = x1 + (int)Math.round(locf1 * (double)w);
			int px2 = x1 + (int)Math.round(locf2 * (double)w);

			int mx = (px1 + px2) / 2;
			
			g2.setColor(color);
			
			g2.drawLine(px1, y1, px1, y1+h2);
			g2.drawLine(px2, y2, px2, y1+h2);
			g2.drawLine(px1, y1+h4, mx, y1+h4);
			g2.drawLine(px2, y2-h4, mx, y2-h4);
			g2.drawLine(mx, y1+h4, mx, y2-h4);
		}
	} 
}