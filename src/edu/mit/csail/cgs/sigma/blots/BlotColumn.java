/*
 * Author: tdanford
 * Date: May 10, 2008
 */
package edu.mit.csail.cgs.sigma.blots;

import java.util.*;
import java.util.regex.*;
import java.awt.Point;
import java.io.PrintStream;

public class BlotColumn {

	private AffineLine line;
	private String description;
	private Blot blot;
	
	public BlotColumn(Blot b, String d, AffineLine l) { 
		blot = b;
		description = d;
		line = new AffineLine(l);
		if(line.isHorizontal()) { throw new IllegalArgumentException(); }
	}
	
	public BlotColumn(Blot b, String saved) {
		blot = b;
		Matcher m = savePattern.matcher(saved);
		if(!m.matches()) { 
			throw new IllegalArgumentException(saved);
		}
		
		int x1 = Integer.parseInt(m.group(1));
		int y1 = Integer.parseInt(m.group(2));
		int x2 = Integer.parseInt(m.group(3));
		int y2 = Integer.parseInt(m.group(4));
		Point p1 = new Point(x1, y1), p2 = new Point(x2, y2);
		line = new AffineLine(p1, p2);
		description = m.group(5);
	}
	
	private static Pattern savePattern = Pattern.compile(
			"^(\\d+),(\\d+)\\s(\\d+),(\\d+)\\s(.*)$");
	
	public void save(PrintStream ps) { 
		ps.println(String.format("%d,%d %d,%d %s", 
				line.getP1().x, line.getP1().y, line.getP2().x, line.getP2().y,
				description));
	}
	
	public Blot getBlot() { return blot; }

	public Point getPoint(double dist) {
		return line.getPoint(dist);
	}
	
	public Point findPoint(int y) { 
		return line.findPointWithY(y);
	}
	
	public AffineLine getLine() { return line; }
	
	public int hashCode() { 
		int code = 17;
		code += description.hashCode(); code *= 37;
		code += line.getP1().hashCode(); code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof BlotColumn)) { return false; }
		BlotColumn r = (BlotColumn)o;
		if(!r.description.equals(description)) { return false; }
		if(!line.getP1().equals(r.line.getP1())) { return false; }
		return true;
	}
	
}
