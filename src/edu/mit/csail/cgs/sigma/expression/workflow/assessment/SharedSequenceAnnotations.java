/*
 * Author: tdanford
 * Date: Apr 18, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.assessment;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.GenomeExpander;
import edu.mit.csail.cgs.ewok.verbs.RefGeneGenerator;
import edu.mit.csail.cgs.sigma.SigmaProperties;

public class SharedSequenceAnnotations { 

	private Map<String,Gene> s288cMapped, sgdGene;
	
	public boolean isGene(String id) { return s288cMapped.containsKey(id) || sgdGene.containsKey(id); }
	public boolean isMappedGene(String id) { return s288cMapped.containsKey(id); }
	
	public SharedSequenceAnnotations() { 
		this(new SigmaProperties().getGenome("sigma"));
	}
	
	public SharedSequenceAnnotations(Genome sigmaGenome) {
		s288cMapped = new TreeMap<String,Gene>();
		sgdGene = new TreeMap<String,Gene>();
		
		RefGeneGenerator gen = new RefGeneGenerator(sigmaGenome, "s288cMapped"); 
		
		Iterator<Gene> itr = new GenomeExpander<Gene>(gen).execute(sigmaGenome);
		while(itr.hasNext()) { 
			Gene g = itr.next();
			s288cMapped.put(g.getID(), g);
		}
		gen.close();
		
		gen = new RefGeneGenerator(sigmaGenome, "sgdGene"); 
		itr = new GenomeExpander<Gene>(gen).execute(sigmaGenome);
		while(itr.hasNext()) { 
			Gene g = itr.next();
			if(!s288cMapped.containsKey(g.getID())) { 
				sgdGene.put(g.getID(), g);
			}
		}
		gen.close();
		
		//SGDOtherGenerator otherGen = new SGDOtherGenerator(sigmaGenome, "sgdOther");
	}
}
