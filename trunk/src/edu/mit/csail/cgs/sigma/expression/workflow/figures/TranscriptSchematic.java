/*
 * Author: tdanford
 * Date: May 5, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.figures;

import java.awt.*;
import java.util.*;

import edu.mit.csail.cgs.viz.eye.AbstractModelPaintable;
import edu.mit.csail.cgs.viz.paintable.AbstractPaintable;

public class TranscriptSchematic extends AbstractPaintable {
	
	public TwoArrowSchematic arrows;
	public AbstractModelPaintable data;
	
	public boolean drawBoundaries;
	
	public TranscriptSchematic(TwoArrowSchematic s, AbstractModelPaintable p) { 
		arrows = s;
		data = p;
		
		drawBoundaries = true;
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2-x1, h = y2-y1;
		int y3 = y2 - h/3;
		Graphics2D g2 = (Graphics2D)g;
		
		g.setColor(Color.white);
		g.fillRect(x1, y1, x2, y2);
		
		if(data != null) { data.paintItem(g, x1, y1, x2, y3); } 
		if(arrows != null) { arrows.paintItem(g, x1, y3, x2, y2); } 
		
		if(arrows != null && drawBoundaries) { 
			Stroke oldStroke = g2.getStroke();
			g2.setStroke(new BasicStroke(2.0f,
					BasicStroke.CAP_ROUND, 
					BasicStroke.JOIN_ROUND, 
					1.0f, 
					new float[] { 5.0f, 5.0f }, 1.0f));
			
			g2.setColor(Color.black);
			for(Integer x : arrows.p1x) { 
				g2.drawLine(x, y1, x, y2);
			}
			for(Integer x : arrows.p2x) { 
				g2.drawLine(x, y1, x, y2);
			}
			
			g2.setStroke(oldStroke);
		}

		g.setColor(Color.black);
		g.drawRect(x1, y1, x2, y2);
	}
}
