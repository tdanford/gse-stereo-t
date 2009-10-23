/*
 * Created on Mar 4, 2008
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma.networks;

import java.util.*;

import edu.mit.csail.cgs.utils.SetTools;

public class RegulatoryMap {
    
    private RegulatoryNetwork network;
    private Set<String> totalGenes;
    private Map<String,Set<String>> tfGenes;
    private SetTools<String> tools;

    public RegulatoryMap(RegulatoryNetwork net) { 
        network = net;
        totalGenes = new TreeSet<String>(network.getAllGeneIDs());
        tfGenes = new TreeMap<String,Set<String>>();
        tools = new SetTools<String>();
        
        for(String tfName : network.getTFNames()) { 
            tfGenes.put(tfName, network.getDownstreamGeneIDs(tfName));
            System.err.println(String.format("%s %d", tfName, tfGenes.get(tfName).size()));
        }
    }
    
    public Set<String> getTotalGenes() { return totalGenes; }
    
    public Set<String> getTFs() { return tfGenes.keySet(); }
    
    public Set<String> getDownstreamGenes(String tf) { 
        return tfGenes.get(tf);
    }
    
    public Set<String> getDownstreamGenes(Collection<String> tfs) { 
        Set<String> downstream = totalGenes;
        for(String tf : tfs) { 
            downstream = tools.intersection(downstream, getDownstreamGenes(tf));
        }
        return downstream;
    }
}
