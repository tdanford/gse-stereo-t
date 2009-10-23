/*
 * Author: tdanford
 * Date: Jan 7, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.io.*;
import java.util.Iterator;

import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.utils.Closeable;
import edu.mit.csail.cgs.utils.models.ModelInput;
import edu.mit.csail.cgs.utils.models.ModelInputIterator;

/**
 * @author tdanford
 *
 */
public class WorkflowDataLoader 
	implements Iterator<ProbeLine>, edu.mit.csail.cgs.utils.Closeable {
	
	private ProbeLine nextLine;
	private BufferedReader br;
	
	public WorkflowDataLoader(WorkflowProperties props, String name) throws IOException { 
		this(new File(props.getDirectory(), String.format("%s.data", name)));
	}
	
	public WorkflowDataLoader() throws IOException { 
		this(System.in);
	}
	
	public WorkflowDataLoader(File f) throws IOException { 
		this(new FileInputStream(f));
	}
	
	public WorkflowDataLoader(InputStream is) { 
		br = new BufferedReader(new InputStreamReader(is));
		nextLine = findNextLine();
	}

	public boolean hasNext() {
		return nextLine != null;
	}

	public ProbeLine next() {
		ProbeLine p = nextLine;
		nextLine = findNextLine();
		return p;
	}
	
	private ProbeLine findNextLine() { 
		try {
			String line = br.readLine();
			if(line != null) { 
				return new ProbeLine(line);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public void close() {
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		br = null;
	}

	public boolean isClosed() {
		return br == null;
	}	
}
