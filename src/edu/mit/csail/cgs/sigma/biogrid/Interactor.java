/*
 * Author: tdanford
 * Date: Apr 24, 2008
 */
package edu.mit.csail.cgs.sigma.biogrid;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public class Interactor { 
    
    private String id, symbol;
    private Set<String> aliases;
    private String organism;
    
    public Interactor(String i, String s, String a, String o) { 
        id = i;
        symbol = s;
        organism = o;
        aliases = new TreeSet<String>();
        
        if(!a.equals("N/A")) { 
            String[] array = a.split("|");
            for(int k = 0; k < array.length; k++) { 
                aliases.add(array[k]);
            }
        }
    }
    
    public String getID() { return id; }
    public String getSymbol() { return symbol; }
    public String getOrganism() { return organism; }
    public Collection<String> getAliases() { return aliases; }
    
    public boolean equals(Object o) { 
        if(!(o instanceof Interactor)) { return false; }
        Interactor i = (Interactor)o;
        return id.equals(i.id) && 
            symbol.equals(i.symbol) && organism.equals(i.organism);
    }
    
    public int hashCode() { 
        int code = 17;
        code += id.hashCode(); code *= 37;
        code += symbol.hashCode(); code *= 37;
        code += organism.hashCode(); code *= 37;
        return code;
    }
}