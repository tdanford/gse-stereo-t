/*
 * Author: tdanford
 * Date: May 20, 2009
 */
package edu.mit.csail.cgs.sigma;

import java.util.*;
import java.awt.Point;
import java.awt.Rectangle;

import edu.mit.csail.cgs.utils.models.Model;


public class RectModel extends Model {

	public static enum Ordering { LESSER, SPANNING, GREATER };
	
	public Integer x, y;
	public Integer width, height;
	
	public RectModel() {}
	
	public RectModel(Integer x, Integer y, Integer w, Integer h) { 
		this.x = x;
		this.y = y;
		width = w;
		height = h;
		
		if(x == null || y == null || width==null || height==null) { 
			throw new IllegalArgumentException();
		}
		if(width <= 0 && height <= 0) { 
			throw new IllegalArgumentException(
					String.format("%d,%d", width, height));
		}
	}
	
	public RectModel(Rectangle r) { 
		this(r.x, r.y, r.width, r.height);
	}
	
	public RectModel(Point p1, Point p2) { 
		this(p1.x, p1.y, p2.x-p1.x, p2.y-p1.y);
	}
	
	public boolean contains(int px, int py) { 
		return px >= x && py >= y && px < (x+width) && py < (y+height);
	}
	
	public boolean overlaps(RectModel m) { 
		return ((x <= m.x && (x+width) > m.x) || (m.x <= x && (m.x+m.width) > x)) &&
			((y <= m.y && (y+height) > m.y) || (m.y <= y && (m.y+m.height) > y));
	}
	
	public boolean contains(RectModel m) { 
		return x <= m.x && y <= m.y && 
			(x+width) >= (m.x+m.width) && (y+height) >= (m.y+m.height); 
	}
	
	public Integer x2() { return x + width - 1; }
	public Integer y2() { return y + height - 1; }
	
	public Ordering xOrdering(int px) {
		if(px >= x + width) { 
			return Ordering.LESSER;
		} else if (px < x) { 
			return Ordering.GREATER; 
		} else { 
			return Ordering.SPANNING;
		}
	}
	
	public Ordering yOrdering(int py) {
		if(py >= y + height) { 
			return Ordering.LESSER;
		} else if (py < y) { 
			return Ordering.GREATER; 
		} else { 
			return Ordering.SPANNING;
		}
	}
	
	public int area() { 
		return width * height;
	}
	
	public int hashCode() { 
		int code = 17;
		code += x; code *= 37;
		code += y; code *= 37;
		code += width; code *= 37;
		code += height; code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof RectModel)) { return false; }
		RectModel m = (RectModel)o;
		return m.x.equals(x) && m.y.equals(y) && 
			width.equals(m.width) && height.equals(m.height);
	}
	
	public String toString() { 
		return String.format("[%d,%d %dx%d]", x, y, width, height);
	}

	public static <X extends RectModel> Collection<X> findXOrdered(Collection<X> rs, Ordering ord, int v) { 
		ArrayList<X> rms = new ArrayList<X>();
		for(X r : rs) {
			if(r.xOrdering(v).equals(ord)) { 
				rms.add(r);
			}
		}
		return rms;
	}
	
	public static <X extends RectModel> Collection<X> findYOrdered(Collection<X> rs, Ordering ord, int v) { 
		ArrayList<X> rms = new ArrayList<X>();
		for(X r : rs) {
			if(r.yOrdering(v).equals(ord)) { 
				rms.add(r);
			}
		}
		return rms;
	}
	
}
