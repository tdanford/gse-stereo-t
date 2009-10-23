package edu.mit.csail.cgs.sigma;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import edu.mit.csail.cgs.datasets.general.Point;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.ExpanderIterator;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;

public class OverlappingRegionFinder<X extends Region> implements OverlappingFinder<X> {
    
	private int size;
    private Map<String,Set<X>> chromMap;
    
    public OverlappingRegionFinder() { 
    	chromMap = new HashMap<String,Set<X>>();
    	size = 0;
    }
    
    public OverlappingRegionFinder(Collection<X> targets) {
    	this(targets.iterator());
    }

    public OverlappingRegionFinder(Iterator<X> targets) {
    	this();
        while(targets.hasNext()) { 
            X line = targets.next();
            addRegion(line);
        }
    }
    
    public Iterator<X> allRegions() { 
    	return new ExpanderIterator<String,X>(
    			new Expander<String,X>() { 
    				public Iterator<X> execute(String a) { 
    					return chromMap.get(a).iterator();
    				}
    			}, chromMap.keySet().iterator());
    }
    
    public void addRegion(X reg) { 
    	if(!chromMap.containsKey(reg.getChrom())) { 
    		chromMap.put(reg.getChrom(), new HashSet<X>());
    	}
    	chromMap.get(reg.getChrom()).add(reg);
    	size += 1;
    }
    
    public int size() { return size; }
    
    public boolean hasOverlapping(Region r) { 
        if(!chromMap.containsKey(r.getChrom())) { 
        	return false;
        }
        LinkedList<X> lines = new LinkedList<X>();
        for(X line : chromMap.get(r.getChrom())) { 
            if(line.overlaps(r)) { 
            	return true;
            }
        }
    
        return false;
    }

    public Collection<X> findOverlapping(Region r) {
        if(!chromMap.containsKey(r.getChrom())) { 
            return new LinkedList<X>();
        }
        LinkedList<X> lines = new LinkedList<X>();
        for(X line : chromMap.get(r.getChrom())) { 
            if(line.overlaps(r)) { 
                lines.add(line);
            }
        }
        
        return lines;
    }

    public Collection<X> findOverlapping(Point p) {
        if(!chromMap.containsKey(p.getChrom())) { 
            return new LinkedList<X>();
        }
        LinkedList<X> lines = new LinkedList<X>();
        for(X line : chromMap.get(p.getChrom())) { 
            if(line.contains(p)) { 
                lines.add(line);
            }
        }
        
        return lines;
    } 
    
    public X findLeftNearest(Region r) { return findLeftNearest(r.startPoint()); }
    
    public X findLeftNearest(Point p) { 
    	
    	Integer d = null;
    	X value = null;
    	
    	if(!chromMap.containsKey(p.getChrom())) { 
    		return null;
    	}
    	
    	for(X line : chromMap.get(p.getChrom())) {
    		if(line.getStart() <= p.getLocation()) { 
    			Integer dist = p.getLocation() - line.getEnd();
    			if(d == null || (d < 0 && dist < 0 && dist > d) ||
    					(d > 0 && dist > 0 && dist < d)) { 
    				d = dist;
    				value = line;
    			}
    		}
    	}
    	
    	return value;
    }
    
    public X findRightNearest(Region r) { return findRightNearest(r.endPoint()); }
    
    public X findRightNearest(Point p) { 
    	
    	Integer d = null;
    	X value = null;

    	if(!chromMap.containsKey(p.getChrom())) { 
    		return null;
    	}
    	
    	for(X line : chromMap.get(p.getChrom())) {
    		if(line.getEnd() >= p.getLocation()) { 
    			Integer dist = line.getStart() - p.getLocation();
    			if(d == null || (d < 0 && dist < 0 && dist > d) ||
    					(d > 0 && dist > 0 && dist < d)) { 
    				d = dist;
    				value = line;
    			}
    		}
    	}
    	
    	return value;
    }
    
    public class ClassifyingMapper implements Mapper<Region,String> { 
    	public ClassifyingMapper() {  
    	}
    	
    	public String execute(Region r) { 
    		Collection<X> over = findOverlapping(r);
    		return over.isEmpty() ? "empty" : "overlapping";
    	}
    }

	public Mapper<Region,String> createClassifyingMapper() {
		return new ClassifyingMapper();
	}
    
}