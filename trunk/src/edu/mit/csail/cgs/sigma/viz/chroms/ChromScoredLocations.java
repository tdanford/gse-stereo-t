/*
 * Author: tdanford
 * Date: Oct 8, 2008
 */
package edu.mit.csail.cgs.sigma.viz.chroms;

import java.util.Iterator;

public interface ChromScoredLocations extends ChromInformation {
	public Iterator<Integer> locations();
}
