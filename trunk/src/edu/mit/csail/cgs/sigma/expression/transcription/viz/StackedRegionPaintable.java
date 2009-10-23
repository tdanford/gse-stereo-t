package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.awt.Color;
import java.awt.Graphics;

import edu.mit.csail.cgs.datasets.general.Region;

public class StackedRegionPaintable extends AbstractRegionPaintable {

	private AbstractRegionPaintable[] regions;
	
	public StackedRegionPaintable(Region r, AbstractRegionPaintable... rs) {
		super(r);
		regions = rs.clone();
		
		for(AbstractRegionPaintable rp : regions) { 
			rp.addPaintableChangedListener(this);
		}
	}

	protected void update() {
		for(AbstractRegionPaintable rp : regions) { 
			rp.update();
		}
	}
	
	public void setRegion(Region r) { 
		super.setRegion(r);
		for(AbstractRegionPaintable rp : regions) {
			rp.setRegion(r);
		}
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int h = y2-y1, w = x2-x1;
		
		g.setColor(Color.white);
		g.fillRect(x1, y1, w, h);
		
		if(regions.length == 2) { 
			int h3 = h/3;
			regions[0].paintItem(g, x1, y1, x2, y1+h3);
			regions[1].paintItem(g, x1, y1+h3, x2, y2);
		} else { 
			int th = (int)Math.floor(h / regions.length);
			for(int i = 0; i < regions.length; i++) { 
				int ry = y1 + th * i;
				regions[i].paintItem(g, x1, ry, x2, ry+th);
			}
		}
	}
}
