/*
 * Author: tdanford
 * Date: Jan 7, 2009
 */
package edu.mit.csail.cgs.sigma;

import java.io.PrintStream;
import java.util.*;

import edu.mit.csail.cgs.utils.Closeable;

public class PassthroughPrinter<X extends Printable> implements Iterator<X>, Closeable {
	
	private PrintStream ps;
	private Iterator<X> itr;
	
	public PassthroughPrinter(PrintStream p, Iterator<X> i) { 
		ps = p;
		itr = i;
	}

	public boolean hasNext() {
		return itr.hasNext();
	}

	public X next() {
		X value = itr.next();
		value.print(ps);
		return value;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public void close() {
		ps.close();
		ps= null;
		if(itr instanceof Closeable) {
			Closeable c = (Closeable)itr;
			if(!c.isClosed()) { 
				c.close();
			}
		}
		itr = null;
	}

	public boolean isClosed() {
		return ps == null || itr == null;
	}

}
