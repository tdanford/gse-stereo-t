/*
 * Author: tdanford
 * Date: Oct 8, 2008
 */
package edu.mit.csail.cgs.sigma.viz.chroms;

import java.util.Iterator;

import edu.mit.csail.cgs.utils.Interval;

public interface ChromRegions extends ChromInformation {
	public Iterator<Interval<Boolean>> regions();
}
