/*
 * Author: tdanford
 * Date: Feb 19, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms.renderers;

import java.awt.*;
import java.util.*;
import edu.mit.csail.cgs.sigma.viz.chroms.*;

public class TriangleLocationRenderer 
	implements ChromInformationRenderer<ChromLocations> {
	
	private Color color;
	private boolean pointDown;
	
	public TriangleLocationRenderer(Color c) { 
		color = c;
		pointDown = true;
	}
	
	public void setColor(Color c) { color = c; }
	public void setPointDown(boolean p) { pointDown = p; }

	public void paintInformation(ChromLocations info, Graphics2D g2, int x1, int y1, int x2, int y2) {
		g2.setColor(color);
		int w = x2-x1, h = y2-y1;

		info.recalculate(w);

		Iterator<Integer> locations = info.locations();
		int length = info.length();
		
		int twidth = h;
		
		int[] xshape = new int[] { 0, twidth/2, -twidth/2 };
		int[] yshape = pointDown ? 
				new int[] { 0, -twidth, -twidth } : 
				new int[] { 0, twidth, twidth };
		
		while(locations.hasNext()) { 
			Integer location = locations.next();
			double locf = (double)location / (double)length;
			int x = (int)Math.round(locf * (double)w);
			int xtrans = x1 + x, ytrans = y1;
			
			g2.setColor(color);
			
			g2.translate(xtrans, ytrans);
			g2.fillPolygon(xshape, yshape, 3);
			g2.translate(-xtrans, -ytrans);
		}
	} 
}