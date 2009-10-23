/*
 * Author: tdanford
 * Date: Sep 15, 2008
 */
package edu.mit.csail.cgs.sigma;

import java.io.*;
import java.util.*;

public class StringGenerator implements Iterator<String>, edu.mit.csail.cgs.utils.Closeable {
	
	private BufferedReader reader;
	private String nextLine;

	public StringGenerator(BufferedReader br) { 
		reader = br;
		findNextLine();
	}
	
	private void findNextLine() { 
		try {
			nextLine = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			nextLine = null;
		}
		
		if(nextLine == null) { 
			close();
		}
	}

	public boolean hasNext() {
		return nextLine != null;
	}

	public String next() {
		String line = nextLine;
		findNextLine();
		return line;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	public void close() { 
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		reader = null;
	}
	
	public boolean isClosed() { 
		return reader == null;
	}
}
