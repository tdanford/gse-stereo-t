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
import edu.mit.csail.cgs.utils.models.ModelInput;
import edu.mit.csail.cgs.utils.models.ModelInputIterator;

/**
 * The chunker turns a stream of probes into "chunks", which are split by how far apart
 * the probes are (and a minimum value of the probe intensities).  
 * 
 * @author tdanford
 */
public class WorkflowChunkReader extends ModelInputIterator<Chunk> {
	
	public WorkflowChunkReader() { 
		this(System.in);
	}
	
	public WorkflowChunkReader(File f) throws IOException { 
		this(new FileInputStream(f));
	}
	
	public WorkflowChunkReader(InputStream is) {
		super(new ModelInput.LineReader<Chunk>(Chunk.class, is));
	}
	
	public Chunk next() { 
		Chunk c = super.next();
		return c;
	}
}


