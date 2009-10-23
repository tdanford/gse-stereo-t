/*
 * Author: tdanford
 * Date: May 4, 2008
 */
package edu.mit.csail.cgs.sigma;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.mit.csail.cgs.utils.parsing.sexprs.*;

public class NameMapping { 
    
    private Map<String,String> forward, reverse;
    
    public NameMapping() { 
        forward = new TreeMap<String,String>();
        reverse = new TreeMap<String,String>();
    }

	public SExpr toSExpr() {
		CompoundExpr e = new CompoundExpr();
		for(String n1 : forward.keySet()) { 
			String n2 = forward.get(n1);
			e.addSubExpr(new CompoundExpr(new SymbolExpr(n1), new SymbolExpr(n2)));
		}
		return e;
	}

    public int size() { return forward.size(); }
    
    public String forward(String n) { 
        if(forward.containsKey(n)) { 
            return forward.get(n); 
        } else { 
            return n;
        }
    }
    
    public String reverse(String n) { 
        if(reverse.containsKey(n)) { 
            return reverse.get(n); 
        } else { 
            return n;
        }
    }
    
    public Collection<String> forwardSet(Collection<String> ns) { 
    	LinkedList<String> mapped = new LinkedList<String>();
    	for(String n : ns) { 
    		mapped.add(forward(n));
    	}
    	return mapped;
    }
    
    public Collection<String> reverseSet(Collection<String> ns) { 
    	LinkedList<String> mapped = new LinkedList<String>();
    	for(String n : ns) { 
    		mapped.add(reverse(n));
    	}
    	return mapped;
    }
    
    public Set<String> domain() { return forward.keySet(); }
    public Set<String> range() { return reverse.keySet(); }
    
    public boolean isInDomain(String n) { return forward.containsKey(n); }
    public boolean isInRange(String n) { return reverse.containsKey(n); }
    
    public void addMapping(String n1, String n2) { 
        if(forward.containsKey(n1) || reverse.containsKey(n2)) {
            String msg = String.format("%s,%s", n1, n2);
            throw new IllegalArgumentException(msg);
        }

        forward.put(n1, n2);
        reverse.put(n2, n1);
    }
    
    public Map<String,String> forwardMapping() { 
        Map<String,String> map = new TreeMap<String,String>();
        for(String n1 : forward.keySet()) { 
            if(!forward.get(n1).equals(n1)) { 
                map.put(n1, forward.get(n1));
            }
        }
        return map;
    }
    
    public Map<String,String> reverseMapping() { 
        Map<String,String> map = new TreeMap<String,String>();
        for(String n2 : reverse.keySet()) { 
            if(!reverse.get(n2).equals(n2)) { 
                map.put(n2, reverse.get(n2));
            }
        }
        return map;
    }

}