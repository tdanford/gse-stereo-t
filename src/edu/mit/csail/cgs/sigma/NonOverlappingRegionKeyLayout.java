/*
 * Created on Mar 27, 2006
 */
package edu.mit.csail.cgs.sigma;

import java.util.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.ewok.nouns.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.utils.*;

/**
 * @author tdanford
 * 
 */
public class NonOverlappingRegionKeyLayout<X extends RegionKey> {
    
    private static class RegionComparator implements Comparator<RegionKey> { 
        public int compare(RegionKey r1, RegionKey r2) { 
            if(!r1.chrom.equals(r2.chrom)) { return r1.chrom.compareTo(r2.chrom); }
            if(r1.start < r2.start) { return -1; }
            if(r1.start > r2.start) { return 1; }
            if(r1.end < r2.end) { return -1; }
            if(r2.end > r2.end) { return 1; }
            return 0;
        }
    }
    
    private static class LayoutTrack<Y extends RegionKey> { 
        
        private Vector<Y> regions;
        private int index;
        
        public LayoutTrack(int ind) { 
            regions = new Vector<Y>();
            index = ind;
        }
        
        public int getIndex() { return index; }
        public Vector<Y> getRegions() { return regions; }
        
        public void addRegion(Y r) { regions.add(r); }
        
        public boolean acceptsRegion(Y r) { 
            for(Y rt : regions) { 
                if(rt.overlaps(r)) { return false; }
            }
            return true;
        }
    }
    
    private static Comparator<RegionKey> comp;
    
    static { 
        comp = new RegionComparator();
    }
    
    private RegionKey[] regions;
    private Vector<LayoutTrack<X>> tracks;
    private Map<RegionKey,LayoutTrack> trackMap;

    public NonOverlappingRegionKeyLayout() {
        regions = null;
        tracks = new Vector<LayoutTrack<X>>();
        trackMap = new HashMap<RegionKey,LayoutTrack>();
    }
    
    public void setRegions(Iterator<X> rs) { 
    	ArrayList<X> rrs = new ArrayList<X>();
    	while(rs.hasNext()) { 
    		rrs.add(rs.next());
    	}
    	setRegions(rrs);
    }
    
    public void setRegions(Collection<X> rs) {
        tracks.clear();
        trackMap.clear();

        regions = rs.toArray(new RegionKey[rs.size()]);
        Arrays.sort(regions, comp);
        
        doLayout();
    }
    
    public Collection<X> getRegions() { 
    	ArrayList<X> rrs = new ArrayList<X>();
    	for(LayoutTrack<X> track : tracks) {
    		rrs.addAll(track.getRegions());
    	}
    	return rrs;
    }
    
    private void doLayout() {
        for(int i = 0; i < regions.length; i++) { 
            X r = (X)(regions[i]);
            int currentTrack = 0;
            
            while(currentTrack < tracks.size() && 
            		!tracks.get(currentTrack).acceptsRegion(r)) { 
                currentTrack += 1;
            }
            
            if(currentTrack >= tracks.size()) { 
                tracks.add(new LayoutTrack(tracks.size()));
            }
            
            tracks.get(currentTrack).addRegion(r);
            trackMap.put(r, tracks.get(currentTrack));
        }
    }
    
    public boolean hasTrack(Region r) { return trackMap.containsKey(r); }
    public int getNumTracks() { return tracks.size(); }
    public int getTrack(RegionKey r) { return trackMap.get(r).getIndex(); }
}
