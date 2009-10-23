/*
 * Author: tdanford
 * Date: May 6, 2009
 */
package edu.mit.csail.cgs.sigma;

import java.util.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import edu.mit.csail.cgs.utils.models.Model;

public class JSONOutputIterator<M extends Model> implements Iterator<M> {
	
	private Iterator<M> itr;
	private PrintStream ps;
	private String filename;
	private int count;
	
	public JSONOutputIterator(Iterator<M> i, File f) throws IOException {
		filename = f.getName();
		ps = new PrintStream(new FileOutputStream(f));
		itr = i;
		count = 0;
	}
	
	public boolean hasNext() {
		boolean itrnext = itr.hasNext();
		if(!itrnext) { 
			ps.close();
			System.out.println(String.format("File %s --> JSON Output %d", 
					filename, count));
		}
		return itrnext;
	}

	public M next() {
		M model = itr.next();
		ps.println(model.asJSON().toString());
		count += 1;
		return model;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
