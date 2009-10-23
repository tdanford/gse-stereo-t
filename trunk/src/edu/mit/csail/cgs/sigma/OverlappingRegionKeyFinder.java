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
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;

public class OverlappingRegionKeyFinder<X extends RegionKey> {
    
	private int size;
    private Map<String,Set<X>> chromMap;
    
    public OverlappingRegionKeyFinder() { 
    	chromMap = new HashMap<String,Set<X>>();
    	size = 0;
    }
    
    public OverlappingRegionKeyFinder(Collection<X> targets) {
    	this(targets.iterator());
    }

    public OverlappingRegionKeyFinder(Iterator<X> targets) {
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
    	if(!chromMap.containsKey(reg.chrom)) { 
    		chromMap.put(reg.chrom, new HashSet<X>());
    	}
    	chromMap.get(reg.chrom).add(reg);
    	size += 1;
    }
    
    public int size() { return size; }
    
    public boolean hasOverlapping(RegionKey r) { 
        if(!chromMap.containsKey(r.chrom)) { 
        	return false;
        }
        LinkedList<X> lines = new LinkedList<X>();
        for(X line : chromMap.get(r.chrom)) { 
            if(line.strandInvariantOverlaps(r)) { 
            	return true;
            }
        }
    
        return false;
    }

    public Collection<X> findOverlapping(RegionKey r) {
        if(!chromMap.containsKey(r.chrom)) { 
            return new LinkedList<X>();
        }
        LinkedList<X> lines = new LinkedList<X>();
        for(X line : chromMap.get(r.chrom)) { 
            if(line.strandInvariantOverlaps(r)) { 
                lines.add(line);
            }
        }
        
        return lines;
    }

    public Collection<X> findStrandedOverlapping(RegionKey r) {
        if(!chromMap.containsKey(r.chrom)) { 
            return new LinkedList<X>();
        }
        LinkedList<X> lines = new LinkedList<X>();
        for(X line : chromMap.get(r.chrom)) { 
            if(line.overlaps(r)) { 
                lines.add(line);
            }
        }
        
        return lines;
    }

    public X findLeftNearest(RegionKey r) { return findLeftNearest(r.chrom, r.start); }
    
    public X findLeftNearest(String chrom, int loc) { 
    	
    	Integer d = null;
    	X value = null;
    	
    	if(!chromMap.containsKey(chrom)) { 
    		return null;
    	}
    	
    	for(X line : chromMap.get(chrom)) {
    		if(line.start <= loc) { 
    			Integer dist = loc - line.end;
    			if(d == null || (d < 0 && dist < 0 && dist > d) ||
    					(d > 0 && dist > 0 && dist < d)) { 
    				d = dist;
    				value = line;
    			}
    		}
    	}
    	
    	return value;
    }
    
    public X findRightNearest(RegionKey r) { return findRightNearest(r.chrom, r.end); }
    
    public X findRightNearest(String chrom, int loc) { 
    	
    	Integer d = null;
    	X value = null;

    	if(!chromMap.containsKey(chrom)) { 
    		return null;
    	}
    	
    	for(X line : chromMap.get(chrom)) {
    		if(line.end >= loc) { 
    			Integer dist = line.start - loc;
    			if(d == null || (d < 0 && dist < 0 && dist > d) ||
    					(d > 0 && dist > 0 && dist < d)) { 
    				d = dist;
    				value = line;
    			}
    		}
    	}
    	
    	return value;
    }
    
    public class ClassifyingMapper implements Mapper<RegionKey,String> { 
    	public ClassifyingMapper() {  
    	}
    	
    	public String execute(RegionKey r) { 
    		Collection<X> over = findOverlapping(r);
    		return over.isEmpty() ? "empty" : "overlapping";
    	}
    }

	public Mapper<RegionKey,String> createClassifyingMapper() {
		return new ClassifyingMapper();
	}
    
}