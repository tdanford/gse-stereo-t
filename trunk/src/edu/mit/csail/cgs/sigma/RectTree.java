/*
 * Author: tdanford
 * Date: May 20, 2009
 */
package edu.mit.csail.cgs.sigma;

import java.util.*;
import java.awt.*;
import java.awt.geom.*;

import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;

public class RectTree<X extends RectModel> {
	
	public static enum Dim { LEAF, X, Y };

	private int split;
	private Dim dim;
	private Collection<X> spanning;
	private RectTree<X> lesser, greater;

	public RectTree(Collection<X> span) {
		this(Dim.LEAF, 0, span, null, null);
	}
	
	public RectTree(Dim d, int spl, Collection<X> span, RectTree<X> ls, RectTree<X> gt) { 
		dim = d;
		split = spl;
		spanning = new ArrayList<X>(span);
		lesser = ls;
		greater = gt;
	}
	
	public int size() { 
		int s = spanning.size();
		if(!dim.equals(Dim.LEAF)) { 
			s += lesser.size();
			s += greater.size();
		}
		return s;
	}
	
	public Collection<X> rects() { 
		ArrayList<X> rs = new ArrayList<X>();
		if(dim.equals(Dim.LEAF)) { 
			rs.addAll(spanning);
		} else { 
			rs.addAll(lesser.rects());
			rs.addAll(spanning);
			rs.addAll(greater.rects());
		}
		return rs;
	}
	
	public Iterator<X> findContaining(int x, int y) {
		Iterator<X> itr = 
			new FilterIterator<X,X>(new ContainsPointFilter<X>(x, y),
				spanning.iterator()); 
		if(dim.equals(Dim.X)) {
			if(x < split) { 
				itr = new SerialIterator<X>(lesser.findContaining(x, y), itr);
			} else if (x > split) { 
				itr = new SerialIterator<X>(itr, greater.findContaining(x, y));
			}
		} else if (dim.equals(Dim.Y)) { 
			if(y < split) { 
				itr = new SerialIterator<X>(lesser.findContaining(x, y), itr);
			} else if (y > split) { 
				itr = new SerialIterator<X>(itr, greater.findContaining(x, y));
			}
		}
		return itr; 
	}
	
	public void split(Dim d) { 
		if(!dim.equals(Dim.LEAF)) { 
			throw new IllegalArgumentException(dim.toString()); 
		}
	
		split = findSplit(spanning, d);
		dim = d;
		lesser = new RectTree<X>(findOrdered(spanning, RectModel.Ordering.LESSER, split, d));
		greater = new RectTree<X>(findOrdered(spanning, RectModel.Ordering.GREATER, split, d));
		spanning = findOrdered(spanning, RectModel.Ordering.SPANNING, split, d);
	}
	
	public static <X extends RectModel> Collection<X> findOrdered(Collection<X> rs, RectModel.Ordering ordering, int value, Dim dim) { 
		if(dim.equals(Dim.X)) { 
			return RectModel.findXOrdered(rs, ordering, value);			
		} else if (dim.equals(Dim.Y)) { 
			return RectModel.findYOrdered(rs, ordering, value);
		} else { 
			throw new IllegalArgumentException(dim.toString());
		}
	}
	
	public static <X extends RectModel> Integer findSplit(Collection<X> rs, Dim d) { 
		Integer split = null;
		if(d.equals(Dim.LEAF)) { throw new IllegalArgumentException(); }
		
		Integer[] possible = new Integer[rs.size()];
		int i = 0;
		for(X r : rs) { 
			if(d.equals(Dim.X)) { 
				possible[i++] = r.x;
			} else { 
				possible[i++] = r.y;
			}
		}
		Arrays.sort(possible);
		
		return split;
	}
	
	public static class ContainsPointFilter<R extends RectModel> implements Filter<R,R> { 
		private int x, y;
		public ContainsPointFilter(int x, int y) { 
			this.x = x;
			this.y = y;
		}
		public R execute(R a) {
			return a.contains(x, y) ? a : null;
		}
	}
	
	public static class OverlapsRectFilter<R extends RectModel> implements Filter<R,R> { 
		
		private RectModel m;
		
		public OverlapsRectFilter(RectModel rm) { 
			rm = m;
		}
		public R execute(R a) {
			return a.overlaps(m) ? a : null;
		}
	}
	
	public static class ContainsRectFilter<R extends RectModel> implements Filter<R,R> { 
		
		private RectModel m;
		
		public ContainsRectFilter(RectModel rm) { 
			rm = m;
		}
		public R execute(R a) {
			return a.contains(m) ? a : null;
		}
	}
	
	public static class ContainedByRectFilter<R extends RectModel> implements Filter<R,R> { 
		
		private RectModel m;
		
		public ContainedByRectFilter(RectModel rm) { 
			rm = m;
		}
		public R execute(R a) {
			return m.contains(a) ? a : null;
		}
	}
	
	public static class DimComparator<R extends RectModel> implements Comparator<R> { 
		private Dim dim; 
		
		public DimComparator(Dim d) { 
			dim = d;
		}
		
		public int compare(R r1, R r2) { 
			if(dim.equals(Dim.X)) { 
				if(r1.x < r2.x) { return -1; }
				if(r1.x > r2.x) { return 1; }
				if(r1.x2() < r2.x2()) { return -1; }
				if(r1.x2() > r2.x2()) { return -1; }
			} else if (dim.equals(Dim.Y)) { 
				if(r1.y < r2.y) { return -1; }
				if(r1.y > r2.y) { return 1; }
				if(r1.y2() < r2.y2()) { return -1; }
				if(r1.y2() > r2.y2()) { return -1; }				
			} 
			return 0;
		}
	}
}

