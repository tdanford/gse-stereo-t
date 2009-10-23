/*
 * Author: tdanford
 * Date: Mar 17, 2009
 */
package edu.mit.csail.cgs.sigma.genes;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.ewok.verbs.MapperIterator;
import edu.mit.csail.cgs.sigma.GeneGenerator;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.utils.models.ModelInput;
import edu.mit.csail.cgs.utils.models.ModelInputIterator;

public class GeneAnnotationGenerator implements GeneGenerator {
	
	private Genome genome;
	private OverlappingRegionFinder<Gene> finder;
	private Map<String,Set<Gene>> named;
	
	public GeneAnnotationGenerator(Genome g, File f) throws IOException { 
		genome = g;
		
		BufferedReader br = new BufferedReader(new FileReader(f));
		
		Iterator<GeneAnnotation> annots = 
			new ModelInputIterator<GeneAnnotation>(
					new ModelInput.LineReader<GeneAnnotation>(GeneAnnotation.class, br));

		Iterator<Gene> genes = new MapperIterator<GeneAnnotation,Gene>(
				new Mapper<GeneAnnotation,Gene>() { 
					public Gene execute(GeneAnnotation a) { 
						String id = a.id;
						String name = a.id;
						char strand = a.strand.charAt(0);
						String src = "GeneAnnotation";
						return new Gene(genome, a.chrom, a.start, a.end, name, id, strand, src);
					}
				}, annots);
		
		finder = new OverlappingRegionFinder<Gene>(genes);
		br.close();
		
		named = new TreeMap<String,Set<Gene>>();
		Iterator<Gene> all = finder.allRegions();
		while(all.hasNext()) { 
			Gene gene = all.next();
			if(!named.containsKey(gene.getID())) { 
				named.put(gene.getID(), new HashSet<Gene>());
			}
			if(!named.containsKey(gene.getName())) { 
				named.put(gene.getName(), new HashSet<Gene>());
			}
			
			named.get(gene.getID()).add(gene);
			named.get(gene.getName()).add(gene);
		}
	}

	public Iterator<Gene> byName(String name) {
		if(named.containsKey(name)) { 
			return named.get(name).iterator();
		} else { 
			return new EmptyIterator<Gene>();
		}
	}

	public Iterator<Gene> execute(Region a) {
		return finder.findOverlapping(a).iterator();
	}

	public void close() {
		finder = null;
		named = null;
	}

	public boolean isClosed() {
		return finder==null;
	}

}
