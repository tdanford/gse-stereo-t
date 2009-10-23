/*
 * Created on Nov 29, 2007
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma.metabolism;

public class Reactant implements Comparable<Reactant> {

    private String name;
    private String location;
    private boolean trivial;
    
    public Reactant(String n, String l, MetabolismProperties ps) { 
        name = n;
        location = l;
        trivial = ps.getTrivialReactants().contains(name);
    }
    
    public String getName() { return name; }
    public String getLocation() { return location; }
    
    public int hashCode() { 
        int code = 17;
        code += name.hashCode(); code *= 37;
        code += location.hashCode(); code *= 37;
        return code;
    }
    
    public boolean equals(Object o) { 
        if(!(o instanceof Reactant)) { return false; }
        Reactant r = (Reactant)o;
        return r.name.equals(name) && location.equals(r.location);
    }
    
    public int compareTo(Reactant r) { 
        int c = name.compareTo(r.name);
        if(c != 0) { return c; }
        c = location.compareTo(r.location);
        return c;
    }
    
    public boolean isTrivial() { 
    	return trivial;
    }
    
    public String toString() {
    	return String.format("%s [%s]", name, location); 
    }
}

