package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.*;

import edu.mit.csail.cgs.viz.paintable.AbstractPaintable;

public class DifferentialKeyPaintable extends AbstractPaintable {
	
	public static String[] defaultPossible = new String[] { "mata", "matalpha", "diploid" };
	public static Color[] colors = new Color[] { Color.red, Color.cyan, Color.green, Color.orange };

	private String[] possible;
	private Set<String> actual;
	
	public DifferentialKeyPaintable(String[] k) { 
		possible = defaultPossible;
		actual = new TreeSet<String>();
		for(String kk : k) { actual.add(kk); }
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		if(actual.isEmpty()) { return; }
		
		int count = possible.length;
		int w = x2-x1, h = y2-y1;
		if(w > h*count) { w = h*count; }
		h = w/count;
		
		int xx1 = x1, xx2 = x2;
		
		x1 = x1 + (x2-x1-w)/2;
		y1 = y1 + (y2-y1-h)/2;
		x2 = x1 + w;
		y2 = y1 + h;
		
		Graphics2D g2 = (Graphics2D)g;

		int boxWidth = h; 
		
		for(int i = 0; i < possible.length; i++) { 
			if(actual.contains(possible[i])) { 
				g2.setColor(colors[i]);
			} else { 
				g2.setColor(Color.white);
			}
			
			int x = x1 + i * boxWidth;
			g2.fillRect(x, y1, boxWidth, h);
		}

		int y = y1 + h/2;
		g2.setColor(Color.black);
		
		g2.drawRect(x1, y1, w, h);
		
		// draw the line bounds.
		g2.drawLine(xx1, y, x1, y);
		g2.drawLine(x2, y, xx2, y);
		g2.drawLine(xx1, y1, xx1, y2);
		g2.drawLine(xx2, y1, xx2, y2);
		
		for(int i = 1; i < possible.length; i++) { 
			int x = x1 + i * boxWidth;
			g2.drawLine(x, y1, x, y2);
		}
	}

}
