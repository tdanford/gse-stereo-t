/*
 * Created on Nov 29, 2007
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma.metabolism;

import java.util.*;
import java.util.regex.*;

public class ORFSet {
    
    private TreeSet<String> orfs;

    public ORFSet() { 
    	orfs = new TreeSet<String>();
    }
    
    public ORFSet(String expr) { 
    	this();
        String[] a = expr.split("\\s+");
        for(int i = 0; i < a.length; i++) {
            if(a[i].length() > 0 && !a[i].equals("(") && !a[i].equals(")") && 
                    !a[i].equals("and") && !a[i].equals("or")) { 
                orfs.add(a[i]);
            }
        }
    }
    
    public ORFSet(String... array) { 
    	this();
    	for(int i = 0; i < array.length; i++) { 
    		orfs.add(array[i]);
    	}
    }
    
    public ORFSet(Collection<String> strs) { 
    	this();
    	orfs.addAll(strs);
    }
    
    public void addORFSet(ORFSet os) { orfs.addAll(os.orfs); }
    
    public boolean containsORF(String orf) { return orfs.contains(orf); }
    public Set<String> getORFs() { return orfs; }
    public int size() { return orfs.size(); }

	public void addORF(String value) {
		orfs.add(value);
	}
}
