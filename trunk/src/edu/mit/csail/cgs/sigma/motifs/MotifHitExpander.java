/*
 * Author: tdanford
 * Date: Mar 23, 2009
 */
package edu.mit.csail.cgs.sigma.motifs;

import java.io.*;
import java.util.*;

import edu.mit.csail.cgs.datasets.general.NamedRegion;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.motifs.WeightMatrix;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.ewok.verbs.motifs.WeightMatrixHit;

public class MotifHitExpander implements Expander<Region,MotifHit> {
	
	public static void main(String[] args) { 
		MotifProperties mps = new MotifProperties();
		
		String strain = args[0];
		Motifs mtfs = new Motifs(mps, strain);
		MotifHitExpander expander = new MotifHitExpander(mtfs);
		
		File output = new File(mps.getMotifDirectory(), 
				String.format("%s_motifs.txt", strain));
		
		Genome genome = mps.getSigmaProperties().getGenome(strain);
		ChromRegionIterator chroms = new ChromRegionIterator(genome);
		Iterator<Region> rchroms = new MapperIterator<NamedRegion,Region>(
				new CastingMapper<NamedRegion,Region>(),
				chroms);
		Iterator<MotifHit> hits = new ExpanderIterator<Region,MotifHit>(
				expander, rchroms);
		
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(output));
			
			while(hits.hasNext()) { 
				MotifHit h = hits.next();
				ps.println(h.asJSON().toString());
			}
			
			ps.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private Motifs motifs;
	private Map<String,WeightMatrix> tfMatrices;

	public MotifHitExpander(Motifs mtfs) { 
		motifs = mtfs;
        tfMatrices = new TreeMap<String,WeightMatrix>();
		
		motifs.loadData();
        for(WeightMatrix wm : motifs.getMatrices()) { 
            tfMatrices.put(wm.name, wm);
        }
	}

	public Iterator<MotifHit> execute(Region a) {
        Collection<WeightMatrixHit> hits = 
        	motifs.findAllMotifs(tfMatrices.values(), a);
        
        ArrayList<MotifHit> hitlist = new ArrayList<MotifHit>();
        
        for(WeightMatrixHit h : hits) {
        	hitlist.add(new MotifHit(h));
        }     
        
        return hitlist.iterator();
	}
}
