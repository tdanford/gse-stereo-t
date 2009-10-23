/*
 * Author: tdanford
 * Date: Feb 19, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms.renderers;

import java.awt.*;
import java.util.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.sigma.viz.chroms.*;
import edu.mit.csail.cgs.utils.Interval;
import edu.mit.csail.cgs.viz.NonOverlappingIntervalLayout;
import edu.mit.csail.cgs.viz.NonOverlappingLayout;
import edu.mit.csail.cgs.viz.paintable.ArrowPainter;

public class DefaultRegionSetRenderer implements ChromInformationRenderer<ChromRegions> {
	
	private Color color;
	
	public DefaultRegionSetRenderer() { this(Color.black); }
	
	public DefaultRegionSetRenderer(Color c) { 
		color = c;
	}

	public void paintInformation(ChromRegions info, Graphics2D g2, int x1, int y1, int x2, int y2) {
		g2.setColor(color);
		int w = x2-x1, h = y2-y1;
		int h2 = h/2;
		int h4 = h/4;

		info.recalculate(w);

		Iterator<Interval<Boolean>> regions = info.regions();
		int length = info.length();

		NonOverlappingIntervalLayout layout = new NonOverlappingIntervalLayout(info.regions());
		int numTracks = layout.getNumTracks();
		int trackHeight = Math.max(1, (int)Math.floor((double) h / (double)Math.max(1, numTracks)));
		
		ArrowPainter arrow = new ArrowPainter(Color.lightGray);
		
		while(regions.hasNext()) { 
			Interval<Boolean> intv = regions.next();
			Boolean strand = intv.data;

			int px1 = x1 + intv.start;
			int px2 = x1 + intv.end;

			g2.setColor(color);

			int track = layout.getTrack(intv);
			int py = y1 + track * trackHeight;
			
			int pwidth = px2-px1, pheight = trackHeight;
			
			if(strand == null) { 
				g2.drawRect(px1, py, px2-px1, trackHeight);
			} else { 
				arrow.setDirection(strand);
				arrow.paintItem(g2, px1, py, px2, py+trackHeight);
			}
		}
	} 
}