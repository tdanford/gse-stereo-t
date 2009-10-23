/*
 * Author: tdanford
 * Date: Sep 11, 2008
 */
package edu.mit.csail.cgs.sigma;

import java.util.*;

import edu.mit.csail.cgs.datasets.general.Point;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;

public class OverlappingPointFinder<X extends Point> implements OverlappingFinder<X> {
	
	private Map<String,OverlappingChromPointFinder<X>> chromFinders;
	
	public OverlappingPointFinder(Iterator<X> pts) { 
		chromFinders = new HashMap<String,OverlappingChromPointFinder<X>>();
		Map<String,LinkedList<X>> chrompts = new HashMap<String,LinkedList<X>>();
		while(pts.hasNext()) { 
			X pt = pts.next();
			if(!chrompts.containsKey(pt.getChrom())) { 
				chrompts.put(pt.getChrom(), new LinkedList<X>());
			}
			chrompts.get(pt.getChrom()).add(pt);
		}
		
		for(String chrom : chrompts.keySet()) { 
			chromFinders.put(chrom, new OverlappingChromPointFinder<X>(chrompts.get(chrom)));
		}
	}

	public Collection<X> findOverlapping(Region r) {
		if(chromFinders.containsKey(r.getChrom())) { 
			return chromFinders.get(r.getChrom()).findOverlapping(r);
		} else { 
			return new LinkedList<X>();
		}
	}

	public Collection<X> findOverlapping(Point p) {
		int loc = p.getLocation();
		return findOverlapping(new Region(p.getGenome(), p.getChrom(), loc, loc));
	}

	public X findOneOverlapping(Point p) {
		if(chromFinders.containsKey(p.getChrom())) { 
			return chromFinders.get(p.getChrom()).findOneOverlapping(p);
		} else { 
			return null;
		}		
	}
	
	private static class OverlappingChromPointFinder<X extends Point> {
		
		private Vector<X> points;
		
		public OverlappingChromPointFinder(Collection<X> pts) { 
			points = new Vector<X>(new TreeSet<X>(pts));
		}
		
		public int findNextIndex(int loc) { 
			if (loc <= points.get(0).getLocation()) { return 0; }
			if(loc >= points.get(points.size()-1).getLocation()) { return points.size()-1; }
			
			int lower = 0, upper = points.size()-1;
			
			while(upper-lower > 1) { 
				int middle = (upper+lower)/2;
				int middleLoc = points.get(middle).getLocation();
				
				if(middleLoc < loc) { 
					lower = middle;
				} else if (middleLoc > loc) { 
					upper = middle;
				} else { 
					return middle;
				}
			}
			
			return upper;
		}
		
		public int findPrevIndex(int loc) { 
			if (loc <= points.get(0).getLocation()) { return 0; }
			if(loc >= points.get(points.size()-1).getLocation()) { return points.size()-1; }
			
			int lower = 0, upper = points.size()-1;
			
			while(upper-lower > 1) { 
				int middle = (upper+lower)/2;
				int middleLoc = points.get(middle).getLocation();
				
				if(middleLoc < loc) { 
					lower = middle;
				} else if (middleLoc > loc) { 
					upper = middle;
				} else { 
					return middle; 
				}
			}
			
			return lower;			
		}

		public X findOneOverlapping(Point p) {
			int i1 = findNextIndex(p.getLocation());
			X pt = points.get(i1);
			if(pt.getLocation() == p.getLocation()) { 
				return pt;
			} else { 
				return (X)null;
			}
		}

		public Collection<X> findOverlapping(Region r) { 
			LinkedList<X> overlaps = new LinkedList<X>();
			int i1 = findNextIndex(r.getStart());
			int i2 = findPrevIndex(r.getEnd());
			
			for(int i = i1; i <= i2; i++) { 
				/*
				if(i < 0 || i >= points.size()) { 
					System.out.println(String.format("[%d, %d] not in [0, %d)", i1, i2, points.size()));
					throw new IllegalStateException();
				}
				*/
				X pt = points.get(i);
				if(r.contains(pt)) { 
					overlaps.add(pt);
				}
			}
			
			return overlaps;			
		}
	}

}
