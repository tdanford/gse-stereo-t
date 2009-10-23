/*
 * Author: tdanford
 * Date: Feb 19, 2009
 */
package edu.mit.csail.cgs.sigma.viz.chroms;

import java.awt.Graphics2D;

public interface ChromInformationRenderer<ChromInfo extends ChromInformation> {

	public void paintInformation(ChromInfo info, Graphics2D g2,
			int x1, int y1, int x2, int y2);
}
