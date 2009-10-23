/*
 * Author: tdanford
 * Date: Jan 7, 2009
 */
package edu.mit.csail.cgs.sigma;

import java.util.LinkedList;

import edu.mit.csail.cgs.utils.Closeable;

public class CloseableHandler implements Closeable {
	
	private LinkedList<Closeable> closeables;
	
	public CloseableHandler() { 
		closeables = new LinkedList<Closeable>();
	}
	
	public void add(Closeable c) { 
		closeables.add(c);
	}

	public boolean isClosed() { 
		return closeables == null;
	}
	
	public void close() {
		for(Closeable c : closeables) { 
			if(!(c.isClosed())) { 
				c.close();
			}
		}
		closeables = null;
	}
}
