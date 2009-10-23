/*
 * Author: tdanford
 * Date: May 10, 2008
 */
package edu.mit.csail.cgs.sigma.blots;

import java.util.*;
import java.awt.Point;

public class AffineLine {
	
	public static double PI2 = Math.PI / 2.0;
	public static double TWOPI = Math.PI * 2.0;
	
	private Point p1, p2;
	
	public AffineLine(Point f, Point s) { 
		p1 = new Point(f); p2 = new Point(s);
		
		if(isVertical() && isHorizontal()) { 
			throw new IllegalArgumentException();
		}
	}
	
	public AffineLine(AffineLine al) { 
		p1 = new Point(al.p1);
		p2 = new Point(al.p2);
	}
	
	public AffineLine lineThrough(Point p) {
		int dx = p2.x - p1.x, dy = p2.y - p1.y;
		return new AffineLine(p, new Point(p.x+dx, p.y + dy));
	}
	
	public boolean isParallel(AffineLine line) { 
		if(isVertical() && line.isVertical()) { return true; }
		if(isHorizontal() && line.isHorizontal()) { return true; }
		
		double unit = scale() / line.scale();
		Point np2 = line.getPoint(unit);
		
		int dx = p2.x - p1.x, dy = p2.y - p1.y;
		int ldx = np2.x - line.p1.x, ldy = np2.y - line.p1.y;
		
		return (dx == ldx && dy == ldy) || 
			(dx == -ldx && dy == -ldy);
	}
	
	public boolean isVerticalish() { 
		if(isVertical()) { return true; }
		if(isHorizontal()) { return false; }
		double slope = Math.abs(slope());
		return slope > 1.0 / slope;
	}
    
    public Point intersection(AffineLine line) { 
        return _intersection(line);
    }
    
	private Point _intersection(AffineLine line) {
        if(isParallel(line)) { throw new IllegalArgumentException(line.toString()); }
		
		// they can't *both* be vertical, otherwise they'd be parallel.
		if(line.isVertical()) { 
			return findPointWithX(line.p1.x);
		} else if (isVertical()) { 
			return line.findPointWithX(p1.x);
		}
        
        double m1 = slope(), m2 = line.slope();
        double numer = m1 * (double)p1.x - m2 * (double)line.p1.x - (double)p1.y + (double)line.p1.y;
        double denom = m1-m2;
        double x = numer / denom;
        double y = m1 * (double)(x - p1.x) + (double)p1.y;

        return new Point((int)Math.round(x), (int)Math.round(y));
	}
    
    private int dx() { return p1.x - p2.x; }
    private int dy() { return p1.y - p2.y; }
	
	public double slope() { 
		if(isVertical()) { throw new IllegalArgumentException(); }
		double m = (double)dy() / (double)dx();
		return m;
	}
	
	public double intercept() { 
		double m = slope();
		double b = (double)p1.y - m * (double)p1.x;
		return b;
	}
	
	public Point getP1() { return p1; }
	public Point getP2() { return p2; }
	
	/**
	 *       5
	 *       |   
	 *     1 | 0
	 * 6 ---------- 4
	 *     2 | 3
	 *       |
	 *       7 
	 *     
	 * Returns one of eight integers, as above, depending on which 
	 * quadrant or axis the line is "pointing into," according to its
	 * "natural direction." 
	 * 
	 * @return { 0, 1, 2, 3, 4, 5, 6, 7 }
	 */
	public int quadrant() { 
		int dx = p2.x - p1.x, dy = p2.y - p1.y;
		if(dx == 0) { 
			if(dy > 0) { 
				return 7;
			} else { 
				return 5;
			}
		}
		if(dy == 0) { 
			if(dx > 0) { 
				return 4;
			} else { 
				return 6;
			}
		}
		
		if(dx > 0) { 
			if(dy > 0) { 
				return 3;
			} else { 
				return 0; 
			}
		} else { 
			if(dy > 0) { 
				return 2;
			} else { 
				return 1; 
			}			
		}
	}
	
	public boolean contains(Point p) { 
		if(isVertical()) { 
			return p.equals(findPointWithY(p.y));
		} else {
			int dx = Math.abs(p2.x - p1.x), dy = Math.abs(p2.y-p1.y);
			if(dx > dy) { 
				return p.equals(findPointWithX(p.x));
			} else { 
				return p.equals(findPointWithY(p.y));				
			}
		}
	}
	
	public Collection<Point> findAllPoints(double c1, double c2) { 
		Point cp1 = getPoint(c1), cp2 = getPoint(c2);
		LinkedList<Point> pts = new LinkedList<Point>();
		
		double diffx = Math.abs(cp2.x - cp1.x), diffy = Math.abs(cp2.y - cp1.y);
		
		if(diffx >= diffy) { 
			for(int x = Math.min(cp1.x, cp2.x); x < Math.max(cp1.x, cp2.x); x++) { 
				Point p = findPointWithX(x);
				pts.add(p);
			}
		} else { 
			for(int y = Math.min(cp1.y, cp2.y); y < Math.max(cp1.y, cp2.y); y++) { 
				Point p = findPointWithY(y);
				pts.add(p);
			}			
		}
		
		return pts;
	}
	
	public double pointCoord(Point p) {
		double c = 0.0;
		double cx = (double)(p.x-p2.x) / (double)(p1.x-p2.x);
		double cy = (double)(p.y-p2.y) / (double)(p1.y-p2.y);
		
		// this is probably the dumb way to do it, but okay.
		c = (cx+cy)/2.0;
		
		return c;
	}

	public Point getPoint(double coord) { 
		double ncoord = 1.0 - coord;
		int x = (int)Math.round(coord * (double)p1.x + ncoord * (double)p2.x);
		int y = (int)Math.round(coord * (double)p1.y + ncoord * (double)p2.y);
		return new Point(x, y);
	}
	
	public double theta() { 
		int dx = p2.x - p1.x, dy = p2.y - p1.y;
		double ratio = (double)dx / scale();
		if(dy > 0 || (dy == 0 && dx > 0)) {  
			return Math.acos(ratio);
		} else { 
			return TWOPI - Math.acos(ratio);
		}
	}
	
	public double scale() { 
		int dx = dx(), dy = dy();
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	public boolean isVertical() { return p1.x == p2.x; }
	public boolean isHorizontal() { return p1.y == p2.y; }
	
	public Point findPointWithY(int y) {
		if(isHorizontal()) { throw new IllegalArgumentException(); }
		//d=y-y2/(y1-y2)
		double d = (double)(y-p2.y) / (double)(p1.y-p2.y);
		return getPoint(d);
	}

	public Point findPointWithX(int x) {
		if(isVertical()) { throw new IllegalArgumentException(); }
		//d=x-x2/(x1-x2)
		double d = (double)(x-p2.x) / (double)(p1.x-p2.x);
		return getPoint(d);
	}

	private static Point complexMultiply(Point p1, Point p2) { 
		// (x1 + i*y1) * (x2 + i*y2) = 
		// x1*x2 - y1*y2 + i*(x1*y2 + y1*x2)
		int x = p1.x*p2.x - p1.y*p2.y;
		int y = p1.x*p2.y + p1.y*p2.x;
		return new Point(x, y);
	}

	private static double complexLength(double x, double y) { 
		return Math.sqrt(x*x + y*y);
	}
    
    private static Point averagePoints(Point p1, Point p2) { 
        int x = (p1.x + p2.x) / 2;
        int y = (p1.y + p2.y) / 2;
        return new Point(x, y);
    }
}
