/*
 * Created on Apr 24, 2008
 */
package edu.mit.csail.cgs.sigma.biogrid;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;

public class InteractionSet {
    
    private BiogridProperties props;
    
    private Map<String,LinkedList<BiogridEntry>> idInteractions;

    public InteractionSet(BiogridProperties bps) { 
        props = bps;
    }
    
    public void loadData() { 
        idInteractions = new TreeMap<String,LinkedList<BiogridEntry>>();
        
        File biogridFile = props.getBiogridFile();
        try {
            Parser<BiogridEntry> entries = 
                new Parser<BiogridEntry>(biogridFile, new BiogridEntry.BiogridMapper());
            
            while(entries.hasNext()) { 
                BiogridEntry entry = entries.next();
                addInteraction(entry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void addInteraction(BiogridEntry e) {
        Interactor int1 = e.getA(), int2 = e.getB();
        String i1 = int1.getID(), i2 = int2.getID();
        
        if(!idInteractions.containsKey(i1)) { 
            idInteractions.put(i1, new LinkedList<BiogridEntry>());
        }
        if(!idInteractions.containsKey(i2)) { 
            idInteractions.put(i2, new LinkedList<BiogridEntry>());
        }
        
        idInteractions.get(i1).add(e);
        idInteractions.get(i2).add(e);
    }
    
    public void closeData() { 
        idInteractions.clear();
        idInteractions = null;
    }
}
