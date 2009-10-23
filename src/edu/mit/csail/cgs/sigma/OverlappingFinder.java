/*
 * Created on Jan 10, 2008
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma;

import java.util.Collection;

import edu.mit.csail.cgs.datasets.general.Point;
import edu.mit.csail.cgs.datasets.general.Region;

public interface OverlappingFinder<X> {
    public Collection<X> findOverlapping(Region r);
    public Collection<X> findOverlapping(Point p);
}
