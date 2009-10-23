/*
 * Author: tdanford
 * Date: Oct 8, 2008
 */
package edu.mit.csail.cgs.sigma.viz.chroms;

public interface ChromPainter extends ChromInformation {
	public boolean value(int unit);
	public Character strand();
}
