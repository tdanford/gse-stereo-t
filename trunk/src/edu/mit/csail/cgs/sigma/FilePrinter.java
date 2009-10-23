/*
 * Author: tdanford
 * Date: Jan 6, 2009
 */
package edu.mit.csail.cgs.sigma;

import java.util.*;
import java.io.*;

public class FilePrinter<X> implements edu.mit.csail.cgs.utils.Closeable {
	
	private PrintStream ps;

	public FilePrinter(File f) throws IOException { 
		ps = new PrintStream(new FileOutputStream(f));
	}
	
	public FilePrinter() { 
		ps = System.out;
	}
	
	public boolean isClosed() { return ps == null; }

	public void close() { 
		ps.close();
		ps = null;
	}
	
	public void printItems(Iterator<X> pitr) { 
		while(pitr.hasNext()) {
			X value = pitr.next();
			if(value instanceof Printable) { 
				((Printable)value).print(ps);
			} else { 
				ps.println(value.toString());
			}
		}
	}
	
	public void printAndClose(Iterator<X> pitr) { 
		printItems(pitr);
		close();
	}
}
