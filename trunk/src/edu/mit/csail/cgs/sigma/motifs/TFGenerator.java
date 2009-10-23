package edu.mit.csail.cgs.sigma.motifs;

import java.util.*;

import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.GeneGenerator;
import edu.mit.csail.cgs.sigma.SigmaProperties;

/**
 * Iterates over Gene objects that are also transcription factors.  
 * 
 * @author tdanford
 */
public class TFGenerator implements Generator<Gene> {
	
	private TFList list;
	private GeneGenerator geneGenerator;
	
	public TFGenerator(MotifProperties mps, String species) { 
		list = new TFList(mps);
		SigmaProperties sps = mps.getSigmaProperties();
		geneGenerator = sps.getGeneGenerator(species);
	}

	public Iterator<Gene> execute() {
		LinkedList<Gene> genes = new LinkedList<Gene>();
		for(String name : list.getTFs()) { 
			Iterator<Gene> tfGenes = geneGenerator.byName(name);
			while(tfGenes.hasNext()) { 
				genes.add(tfGenes.next());
			}
		}
		return genes.iterator();
	}
}
