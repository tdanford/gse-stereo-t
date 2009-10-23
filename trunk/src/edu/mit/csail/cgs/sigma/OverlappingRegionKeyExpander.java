/*
 * Author: tdanford
 * Date: Jul 27, 2009
 */
package edu.mit.csail.cgs.sigma;

import java.util.Collection;
import java.util.Iterator;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.general.Stranded;
import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;

public class OverlappingRegionKeyExpander<X extends RegionKey> implements Expander<Region,X> {
	
	private OverlappingRegionKeyFinder<X> finder;
	
	public OverlappingRegionKeyExpander(Iterator<X> itr) { 
		finder = new OverlappingRegionKeyFinder<X>(itr);
	}

	public OverlappingRegionKeyExpander(Collection<X> c) { 
		finder = new OverlappingRegionKeyFinder<X>(c);
	}

	public Iterator<X> execute(Region a) {
		String strand = "+";
		RegionKey key = new RegionKey(a.getChrom(), a.getStart(), a.getEnd(), strand);
		
		if(a instanceof Stranded) { 
			Stranded s = (Stranded)a;
			strand = String.valueOf(s.getStrand());
			key = new RegionKey(a.getChrom(), a.getStart(), a.getEnd(), strand);
			return finder.findStrandedOverlapping(key).iterator();
		} else { 
			return finder.findOverlapping(key).iterator();
		}
	}

}
