/*
 * Author: tdanford
 * Date: May 21, 2008
 */
package edu.mit.csail.cgs.sigma.genes;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.GeneGenerator;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;

public class GeneAssignmentExpander<X extends Region> implements Expander<X,Gene> {
	
	private GeneAnnotationProperties props;
	private int distance;
	private GeneGenerator gener;
	private String strain;
	
	public GeneAssignmentExpander(GeneAnnotationProperties gaps, String strain) { 
		props = gaps;
		distance = props.getGeneAssignmentDistance();
		this.strain = strain;
		gener = props.getSigmaProperties().getGeneGenerator(strain);
	}
	
	public GeneAssignmentExpander(GeneAnnotationProperties gaps, String strain, int dist) { 
		props = gaps;
		distance = dist;
		this.strain = strain;
		gener = props.getSigmaProperties().getGeneGenerator(strain);
	}
	
	public Iterator<Gene> execute(X r) {
		Region window = new Region(r.getGenome(), r.getChrom(), r.getStart()-distance, r.getEnd()+distance);
		Iterator<Gene> genes = gener.execute(window);
		genes = new FilterIterator<Gene,Gene>(new DownstreamFilter(r), genes);
		return genes;
	}
	
	private static class DownstreamFilter implements Filter<Gene,Gene> {
		private Region base;
		public DownstreamFilter(Region b) { 
			base = b;
		}
		
		public Gene execute(Gene g) { 
			return g.overlaps(base) || 
				(g.getStrand() == '+' && g.getStart() > base.getEnd()) || 
				(g.getStrand() == '-' && g.getEnd() < base.getStart()) ? 
						g : null;
		}
	}
}
