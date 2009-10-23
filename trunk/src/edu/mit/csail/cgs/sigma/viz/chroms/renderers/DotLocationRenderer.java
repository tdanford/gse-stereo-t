/*
 * Author: tdanford
 * Date: Feb 19, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms.renderers;

import java.awt.*;
import java.util.*;
import edu.mit.csail.cgs.sigma.viz.chroms.*;

public class DotLocationRenderer implements ChromInformationRenderer<ChromLocations> {
	
	private Color color;
	
	public DotLocationRenderer(Color c) { 
		color = c;
	}

	public void paintInformation(ChromLocations info, Graphics2D g2, int x1, int y1, int x2, int y2) {
		g2.setColor(color);
		int w = x2-x1, h = y2-y1;

		info.recalculate(w);
		
		int diam = h;
		int rad = diam/2;

		Iterator<Integer> locations = info.locations();
		int length = info.length();
		
		int y = y1 + h/2;
		
		while(locations.hasNext()) { 
			Integer location = locations.next();
			double locf = (double)location / (double)length;
			int x = x1 + (int)Math.round(locf * (double)w);

			g2.setColor(color);

			g2.fillOval(x-rad, y-rad, diam, diam);
		}
	} 
}