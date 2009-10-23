package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.awt.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.viz.paintable.*;

public abstract class RegionPainter extends AbstractPaintable {
	
	protected Region region;
	protected int width, height;

	public RegionPainter() { 
		region = null;
		width = height = 1;
	}
	
	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) { 
		Graphics2D g2 = (Graphics2D)g;
		int w = x2 - x1, h = y2 - y1;
		
		if(!isReadyToPaint() || width != w || height != h) {
            width = w;
			height = h;
            System.out.println(
            		String.format("******* doLayout() ********, %d x %d pix", width, height));
			doLayout();
		}
		
		paintRegion(g2, x1, y1, w, h);
	}
	
	public int getXOffset(int bp) { 
		if(region == null) { return 0; }
        int offset = bp-region.getStart();
        int rwidth = region.getWidth();
        double frac = (double)offset / (double)rwidth;
		int pix = (int)Math.round(frac * (double)width);
        
        /*
        System.out.println(String.format("bp %d (region: %d bp, window: %d pix) --> %.3f, %d pix",
                bp, rwidth, width, frac, pix));
        */
        
        return pix;
	}
	
	public abstract void paintRegion(Graphics2D g, int x1, int y1, int w, int h);
	public abstract boolean isReadyToPaint();
	public abstract void doLayout();
	
	public void setRegion(Region r) { 
		region = r;
	}
}
