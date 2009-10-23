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
public class WorkflowFixedSizeChunker implements Iterator<Chunk> {
	
	public static void main(String[] args) { 
		int split = Integer.parseInt(args[0]);
		try {
			WorkflowFixedSizeChunker splitter = new WorkflowFixedSizeChunker(split);
			new FilePrinter<Chunk>().printAndClose(splitter);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int size;
	private ProbeLine nextLine;
	private Chunk nextChunk;
	private Iterator<ProbeLine> lines;
	private int lineCount, chunkCount;
	
	public WorkflowFixedSizeChunker(int spl, Iterator<ProbeLine> l) {
		lineCount = 0;
		chunkCount = 0;
		size = spl;
		lines = l;
		nextLine = lines.hasNext() ? lines.next() : null;
		nextChunk = findNextChunk();
	}
	
	public WorkflowFixedSizeChunker(int spl) throws IOException {
		this(spl, new WorkflowDataLoader());
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
		LinkedList<ProbeLine> chunklines = new LinkedList<ProbeLine>();

		do { 
			lineCount += 1;
			chunklines.addLast(nextLine);
			nextLine = lines.hasNext() ? lines.next() : null;
			
		} while(nextLine != null && 
				chunklines.size() < size && 
				nextLine.chrom.equals(chunklines.getLast().chrom)); 

		Chunk c = new Chunk(chunklines);
		chunkCount += 1;
		
		System.out.println("#Lines: " + chunklines.size());
		
		return c;
	}
	
	public int getLineCount() { return lineCount; }
}