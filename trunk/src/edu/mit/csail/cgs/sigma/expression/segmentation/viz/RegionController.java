/*
 * Author: tdanford
 * Date: Jan 20, 2009
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.viz;

import edu.mit.csail.cgs.datasets.general.Region;

public interface RegionController {

	public void moveLeft();
	public void moveRight();
	public void zoomIn();
	public void zoomOut();
	public void jumpTo(String loc);
	public Region region();
}
