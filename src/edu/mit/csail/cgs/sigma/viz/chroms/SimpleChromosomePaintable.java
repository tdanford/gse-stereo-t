/*
 * Author: tdanford
 * Date: Feb 19, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms;

import java.util.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.viz.paintable.AbstractPaintable;

public class SimpleChromosomePaintable extends AbstractPaintable {
	
	private int length;
	private boolean flipLower;

	private TrackedChromosomePaintable upper; 
	//private OverlayChromosomePaintable upper;

	private TrackedChromosomePaintable lower;
	//private OverlayChromosomePaintable lower;
	
	//private TrackedChromosomePaintable middle;
	private OverlayChromosomePaintable middle;
	
	public SimpleChromosomePaintable(int len) {
		length = len;
		flipLower = true;
		
		upper = new TrackedChromosomePaintable();
		lower = new TrackedChromosomePaintable();
		//middle = new TrackedChromosomePaintable();

		//upper = new OverlayChromosomePaintable();
		//lower = new OverlayChromosomePaintable();
		middle = new OverlayChromosomePaintable();
	}
	
	public void recalculate(int units) { 
		upper.recalculate(units);
		lower.recalculate(units);
		middle.recalculate(units);
	}
	
	public void setFlipLower(boolean f) { flipLower = f; }
	
	public int length() { return length; }
	
	public void addUpperInformation(ChromInformation info, ChromInformationRenderer render) {
		if(info.length() != length) { 
			throw new IllegalArgumentException(String.format("%d != %d", 
					info.length(), length));
		}
		upper.addInformation(info, render);
	}
	
	public void addMiddleInformation(ChromInformation info, ChromInformationRenderer render) { 
		if(info.length() != length) { 
			throw new IllegalArgumentException(String.format("%d != %d", 
					info.length(), length));
		}
		middle.addInformation(info, render);
	}
	
	public void addLowerInformation(ChromInformation info, ChromInformationRenderer render) { 
		if(info.length() != length) { 
			throw new IllegalArgumentException(String.format("%d != %d", 
					info.length(), length));
		}
		lower.addInformation(info, render);
	}
	
	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		Graphics2D g2 = (Graphics2D)g;
		
		int w = x2-x1, h = y2-y1;
		int size = upper.size() + lower.size() + 1;
		int trackHeight = (int)Math.round((double)h / (double)size);
		
		int upperHeight = trackHeight * upper.size();
		int lowerHeight = trackHeight * lower.size();
		
		if(middle.size() == 0) {
			int newTrackHeight = 3;
			int diff = trackHeight-newTrackHeight;
			trackHeight = newTrackHeight;
			upperHeight += diff/2;
			lowerHeight += diff/2;
		}
		
		int uy1 = y1, uy2 = y1 + upperHeight, uy3 = y1 + upperHeight + trackHeight;
		
		int my = uy2 + (trackHeight/2);
		
		g2.setColor(Color.black);
		g2.drawLine(x1, my, x2, my);
		
		upper.paintItem(g2, x1, uy1, x2, uy1+upperHeight);
		middle.paintItem(g2, x1, uy2, x2, uy2+trackHeight);

		if(flipLower) { 
			//int ytrans = uy3 + lowerHeight;
			int ytrans = y2;
			int xtrans = x2;
			double rotation = -Math.PI;

			g2.translate(xtrans, ytrans);
			g2.rotate(rotation);

			lower.paintItem(g2, 0, 0, w, lowerHeight);
			
			g2.rotate(-rotation);
			g2.translate(-xtrans, -ytrans);
		} else { 
			lower.paintItem(g2, x1, uy3, x2, uy3 + lowerHeight);
		}
	}
}
