/*
 * Author: tdanford
 * Date: Oct 8, 2008
 */
package edu.mit.csail.cgs.sigma.viz.chroms;

public interface MultiChromPainter extends ChromInformation {
	public int height();  // the length of the returned array from 'value()'
	public boolean[] value(int unit);  
	public Character strand();
}
