/*
 * Author: tdanford
 * Date: May 10, 2008
 */
package edu.mit.csail.cgs.sigma.blots;

import java.util.*;
import java.awt.*;

public class AffineQuadrangle {

	private Point p1, p2, p3, p4;
	private Point[] vertices;
	private Rectangle bbox;
	
	public AffineQuadrangle(Point pp1, Point pp2, Point pp3, Point pp4) {
		init(pp1, pp2, pp3, pp4);
	}
	
	private void init(Point pp1, Point pp2, Point pp3, Point pp4) {
		p1 = pp1; p2 = pp2; p3 = pp3; p4 = pp4;
		int x1 = Math.min(Math.min(p1.x, p2.x), Math.min(p3.x, p4.x));
		int x2 = Math.max(Math.max(p1.x, p2.x), Math.max(p3.x, p4.x));
		int y1 = Math.min(Math.min(p1.y, p2.y), Math.min(p3.y, p4.y));
		int y2 = Math.max(Math.max(p1.y, p2.y), Math.max(p3.y, p4.y));
		bbox = new Rectangle(x1, y1, x2-x1, y2-y1);
		if(bbox.width==0 || bbox.height==0) { 
			throw new IllegalArgumentException();
		}
		
		vertices = new Point[4];
		
		AffineLine l12 = new AffineLine(p1, p2);
		AffineLine l13 = new AffineLine(p1, p3);
		AffineLine l14 = new AffineLine(p1, p4);
		
		double[] thetas = new double[3];
		thetas[0] = l12.theta(); thetas[1] = l13.theta(); thetas[2] = l14.theta();
		Point[] pts = new Point[3];
		pts[0] = p2; pts[1] = p3; pts[2] = p4;
		
		boolean unsorted = false;
		do {
			unsorted = false;
			for(int i = 1; i < thetas.length && !unsorted; i++) { 
				if(thetas[i] < thetas[i-1]) { 
					unsorted = true;
					
					double temp = thetas[i];
					thetas[i] = thetas[i-1];
					thetas[i-1] = temp;
					
					Point tp = pts[i];
					pts[i] = pts[i-1];
					pts[i-1] = tp;
				}
			}
		} while(unsorted);
		
		vertices[0] = p1;
		vertices[1] = pts[0];
		vertices[2] = pts[1];
		vertices[3] = pts[2];
	}
	
	public Point[] getVertices() { return vertices; }
	
	public Point getPoint(double c1, double c2, double c3) { 
		double c4 = 1.0 - (c1 + c2 + c3);
		int x = (int)Math.round(c1 * (double)p1.x + c2 * (double)p2.x + 
				c3 * (double)p3.x + c4 * (double)p4.x);
		int y = (int)Math.round(c1 * (double)p1.y + c2 * (double)p2.y + 
				c3 * (double)p3.y + c4 * (double)p4.y);
		return new Point(x, y);
	}
	
	/**
	 * Returns true if the Point p is on the <i>interior</i> of the quadrangle, 
	 * or false otherwise.
	 * 
	 * @param p The point whose containment is to be tested.
	 * @return
	 */
	public boolean contains(Point p) { 
		// apply a quick test, to see if we can rule the point out completely.
		if(!bboxContains(p)) { return false; }
		
		// It'd be nice to avoid having to create this array, for speed reasons,
		// but something like this is easy to think about when we can't rely on 
		// the points p1...p4 to be ordered in any way.
		int[] quads = new int[8];
		for(int i = 0; i < quads.length; i++) { quads[i] = 0; }
		
		return internalContains(p, quads);
	}
	
	private boolean bboxContains(Point p) { 
		return p.x>= bbox.x && p.y >= bbox.y && 
			p.x < bbox.x + bbox.width && p.y < bbox.y + bbox.height; 
	}
	
	private boolean internalContains(Point p, int[] quads) { 
		quads[new AffineLine(p, p1).quadrant()]++;
		quads[new AffineLine(p, p2).quadrant()]++;
		quads[new AffineLine(p, p3).quadrant()]++;
		quads[new AffineLine(p, p4).quadrant()]++;
		
		for(int i = 0; i < 4; i++) { 
			if(quads[i] == 0) { return false; }
		}
		return true;
	}

	public Collection<Point> getAllPoints() { 
		LinkedList<Point> pts = new LinkedList<Point>();

		int[] quads = new int[8];
		
		for(int x = bbox.x; x < bbox.x+bbox.width; x++) { 
			for(int y = bbox.y; x < bbox.y+bbox.height; y++) { 
				for(int i = 0; i < 4; i++) { quads[i] = 0; }
				Point p = new Point(x, y);
				if(internalContains(p, quads)) { 
					pts.addLast(p);
				}
			}
		}

		return pts;
	}	
}
