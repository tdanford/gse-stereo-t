/*
 * Author: tdanford
 * Date: Dec 15, 2008
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.FilePrinter;
import edu.mit.csail.cgs.sigma.expression.workflow.models.Chunk;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;

/**
 * The chunker turns a stream of probes into "chunks", which are split by how far apart
 * the probes are (and a minimum value of the probe intensities).  
 * 
 * @author tdanford
 */
public class WorkflowChunker implements Iterator<Chunk> {
	
	public static void main(String[] args) { 
		try {
			WorkflowChunker splitter = new WorkflowChunker(new WorkflowProperties());
			new FilePrinter<Chunk>().printAndClose(splitter);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int split;
	private double thresh;
	
	private ProbeLine nextLine;
	private Chunk nextChunk;
	private Iterator<ProbeLine> lines;
	private int lineCount, chunkCount;
	
	public WorkflowChunker(int split, Iterator<ProbeLine> l) {
		this(new WorkflowProperties(), l);
		this.split = split;
	}
	
	public WorkflowChunker(int spl) throws IOException { 
		this(new WorkflowProperties());
		split = spl;
	}
	
	public WorkflowChunker(WorkflowProperties ps, Iterator<ProbeLine> l) {
		lineCount = 0;
		chunkCount = 0;
		split = ps.getDefaultSplit();
		thresh = ps.getDefaultThreshold();
		lines = l;
		nextLine = lines.hasNext() ? lines.next() : null;
		nextChunk = findNextChunk();
	}
	
	public WorkflowChunker(WorkflowProperties ps) throws IOException {
		this(ps, new WorkflowDataLoader());
	}
	
	public void remove() { throw new UnsupportedOperationException(); }
	
	public boolean hasNext() {
		return nextChunk != null;
	}
	
	public Chunk next() { 
		Chunk c = nextChunk;
		nextChunk = findNextChunk();
		return c;
	}

	private Chunk findNextChunk() {
		if(nextLine == null) { return null; }
		ArrayList<ProbeLine> chunklines = new ArrayList<ProbeLine>();

		do { 
			lineCount += 1;
			chunklines.add(nextLine);
			nextLine = lines.hasNext() ? lines.next() : null;
		} while(nextLine != null && !isSplit(nextLine, chunklines));

		Chunk c = new Chunk(chunklines);
		chunkCount += 1;
		
		System.out.println("#Lines: " + chunklines.size());
		//System.out.println("#Chunks: " + chunkCount);
		//System.out.println("nextLine: " + nextLine);
		
		return c;
	}
	
	public int getLineCount() { return lineCount; }

	public boolean isBelowThreshold(ArrayList<ProbeLine> lns, int limit) { 
		int i = 0;
		for( ; i < lns.size() && i < limit; i++) { 
			ProbeLine p = lns.get(lns.size()-i-1);
			if(!p.belowThreshold(thresh)) { 
				return false;
			}
		}
		return i >= limit;
	}
	
	public boolean isSplit(ProbeLine p1, ArrayList<ProbeLine> chunklines) {
		int limit = 5;
		int minsize = 50;
		ProbeLine p2 = chunklines.get(chunklines.size()-1);
		return !p1.chrom.equals(p2.chrom) 
			|| Math.abs(p1.offset - p2.offset) >= split
			|| (chunklines.size() >= 10 && 
				//|| (chunklines.size() >= 200 && p1.belowThreshold(thresh));
				(chunklines.size() >= minsize && isBelowThreshold(chunklines, limit))
				);
	}
}
