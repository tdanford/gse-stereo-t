/*
 * Created on Mar 4, 2008
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma;

import java.util.*;

public class RankedObject<X extends Comparable,Y> implements Comparable<RankedObject<X,Y>> {
    
    private X key;
    private Y value;
    
    public RankedObject(X k, Y v) { 
        key = k; 
        value = v;
    }
    
    public X getKey() { return key; }
    public Y getValue() { return value; }
    
    public String toString() { return String.format("%s %s", key.toString(), value.toString()); }

    public int compareTo(RankedObject<X, Y> r) {
        return key.compareTo(r.key); 
    }

    public boolean equals(Object o) { 
        if(!(o instanceof RankedObject)) { return false; }
        RankedObject ro = (RankedObject)o;
        return ro.value.equals(value);
    }
    
    public int hashCode() { 
        int code = 17;
        code += value.hashCode(); code *= 37;
        return code;
    }
}
