/*
 * Author: tdanford
 * Date: Jul 27, 2009
 */
package edu.mit.csail.cgs.sigma;

import java.util.Collection;
import java.util.Iterator;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.ewok.verbs.Expander;

public class OverlappingRegionExpander<X extends Region> implements Expander<Region,X> {
	
	private OverlappingRegionFinder<X> finder;
	
	public OverlappingRegionExpander(Iterator<X> itr) { 
		finder = new OverlappingRegionFinder<X>(itr);
	}

	public OverlappingRegionExpander(Collection<X> c) { 
		finder = new OverlappingRegionFinder<X>(c);
	}

	public Iterator<X> execute(Region a) {
		return finder.findOverlapping(a).iterator();
	}

}
