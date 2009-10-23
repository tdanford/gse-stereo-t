/*
 * Author: tdanford
 * Date: Feb 17, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.viz.paintable.AbstractPaintable;

public abstract class AbstractRegionPaintable extends AbstractPaintable {

	protected Region region;
	
	public AbstractRegionPaintable() { 
		region = null;
	}
	
	public AbstractRegionPaintable(Region r) { 
		region = r; 
	}
	
	public Region getRegion() { return region; }
	
	public void setRegion(Region r) { 
		region = r;
		update();
		dispatchChangedEvent();
	}
	
	protected abstract void update();
}
