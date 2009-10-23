/*
 * Author: tdanford
 * Date: Jan 5, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.models;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.Printable;
import edu.mit.csail.cgs.utils.ArrayUtils;
import edu.mit.csail.cgs.utils.models.Model;

public class Chunk extends Model implements Printable {
	
	public ProbeLine[] lines;
	
	public Chunk() { 
	}
	
	public Chunk(Collection<ProbeLine> ls) {
		lines = ls.toArray(new ProbeLine[0]);
	}
	
	public int size() { return lines.length; }
	
	public String getChrom() { return lines[0].chrom; }
	public String getStrand() { return lines[0].strand; }
	
	public void print(PrintStream ps) { 
		for(ProbeLine l : lines) { 
			ps.println(l.toString());
		}
		ps.println("----------------------------");
	}
	
	public Iterator<ProbeLine> lines() { 
		return ArrayUtils.asIterator(lines);
	}
}
