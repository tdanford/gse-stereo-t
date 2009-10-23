/**
 * 
 */
package edu.mit.csail.cgs.sigma.viz.chroms.information;

import java.util.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.sigma.viz.chroms.ChromRegions;
import edu.mit.csail.cgs.utils.Interval;

/**
 * @author tdanford
 *
 */
public class ChromRegionSet implements ChromRegions {
	
	private int length;
	private int size;
	private Vector<Region> regions;
	private ArrayList<Interval<Boolean>> intervals;
	
	public ChromRegionSet(String chr, int len, Iterator<? extends Region> regs) { 
		length = len;
		size = 1;
		regions = new Vector<Region>();
		intervals = new ArrayList<Interval<Boolean>>();
		
		while(regs.hasNext()) { 
			Region r = regs.next();
			if(r.getChrom().equals(chr)) { 
				regions.add(r);
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.viz.chroms.ChromRegions#regions()
	 */
	public Iterator<Interval<Boolean>> regions() {
		return intervals.iterator();
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.viz.chroms.ChromInformation#length()
	 */
	public int length() {
		return length;
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.viz.chroms.ChromInformation#recalculate(int)
	 */
	public void recalculate(int units) {
		size = units;
		intervals.clear();
		
		for(Region r : regions) { 
			int s = r.getStart(), e = r.getEnd();
			double sf = (double)s / (double)length;
			double ef = (double)e / (double)length;
			int ps = (int)Math.round(sf * (double)size);
			int pe = (int)Math.round(ef * (double)size);
			
			Boolean strand = null;
			if(r instanceof StrandedRegion) { 
				StrandedRegion str = (StrandedRegion)r;
				strand = str.getStrand() == '+';
			}
			intervals.add(new Interval<Boolean>(ps, pe, strand));
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.viz.chroms.ChromInformation#size()
	 */
	public int size() {
		return size;
	}

}
