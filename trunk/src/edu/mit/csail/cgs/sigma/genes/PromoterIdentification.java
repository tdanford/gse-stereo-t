package edu.mit.csail.cgs.sigma.genes;

import java.io.*;
import java.util.*;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.StrandFilter;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.datasets.chippet.RunningOverlapSum;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;

public class PromoterIdentification implements Expander<Region,Promoter> {
	
	public static void main(String[] args) { 
		GeneAnnotationProperties gaps = new GeneAnnotationProperties();
		SigmaProperties props = gaps.getSigmaProperties();
		Set<String> strains = new TreeSet<String>();
		strains.add("sigma");
		strains.add("s288c");
		
		for(String strain : strains) { 
			PromoterIdentification promId = new PromoterIdentification(gaps, strain);
			GenomeExpander<Promoter> genomePromoters = new GenomeExpander<Promoter>(promId);
			Genome g = props.getGenome(strain);
			Iterator<Promoter> promoters = genomePromoters.execute(g);

			String filename = String.format("%s_promoters.txt", strain);
			File outputFile = new File(props.getBaseDir(), filename);

			try { 
				PrintStream ps = new PrintStream(new FileOutputStream(outputFile));

				while(promoters.hasNext()) {
					Promoter p = promoters.next();
					String line = String.format("%s \t%d  \t%d  ", 
							p.getChrom(), p.getStart(), p.getEnd());
					for(String id : p.getDownstreamIDs()) { 
						line += String.format("\t%s", id);
					}
					ps.println(line);
				}

				ps.close();
			} catch(IOException ie) { 
				ie.printStackTrace(System.err);
			}
		}
	}
	
	private GeneAnnotationProperties gaProps;
	private SigmaProperties props;
	private String strain;
	private Genome genome;
	private Expander<Region,Gene> geneGenerator;
	private StrandFilter<Gene> watson, crick;
	
	public PromoterIdentification(GeneAnnotationProperties ps, String str) {
		gaProps = ps;
		props = gaProps.getSigmaProperties();
		strain = str;
		genome = props.getGenome(strain);
		geneGenerator = props.getGeneGenerator(strain);
		watson = new StrandFilter<Gene>('+');
		crick = new StrandFilter<Gene>('-');
	}
	
	public Iterator<Promoter> getAllPromoters() { 
		File promFile = gaProps.getPromoterFile(strain); 
		try {
			if(promFile.exists()) { 
				SavedFile<Promoter> saved = new SavedFile<Promoter>(new Promoter.Decoder(genome),
						new Promoter.Encoder(), promFile);
				saved.load();
				return saved.getValues().iterator();

			} else { 
				Iterator<Promoter> itr = searchForPromoters();
				LinkedList<Promoter> loaded = new LinkedList<Promoter>();
				while(itr.hasNext()) { 
					loaded.addLast(itr.next());
				}

				SavedFile<Promoter> saved = new SavedFile<Promoter>(new Promoter.Decoder(genome),
						new Promoter.Encoder(), promFile, loaded);
				saved.save();
				return loaded.iterator();
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public Iterator<Promoter> searchForPromoters() { 
		GenomeExpander<Promoter> ge = new GenomeExpander<Promoter>(this);
		return ge.execute(genome);
	}
	
	public Iterator<Promoter> execute(Region r) { 
		RunningOverlapSum sum = new RunningOverlapSum(genome, r.getChrom());
		Iterator<Gene> genes = geneGenerator.execute(r);
		LinkedList<Gene> geneList = new LinkedList<Gene>();
		while(genes.hasNext()) { 
			Gene g = genes.next();
			geneList.addLast(g);
			sum.addRegion(g);
		}
		OverlappingRegionFinder<Gene> overlapper = new OverlappingRegionFinder<Gene>(geneList);
		
		TreeSet<Region> codingRegions = new TreeSet<Region>(sum.collectRegions(1));
		Region[] codingArray = codingRegions.toArray(new Region[codingRegions.size()]);
		
		LinkedList<Promoter> promoters = new LinkedList<Promoter>();
		for(int i = 1; i < codingArray.length; i++) { 
			int start = codingArray[i-1].getEnd();
			int end = codingArray[i].getStart();
			if(start < end) { 
				Promoter p = new Promoter(genome, r.getChrom(), start, end);
				Iterator<Gene> left =
					new FilterIterator<Gene,Gene>(crick,
					overlapper.findOverlapping(new Point(genome, r.getChrom(), start)).iterator());
				Iterator<Gene> right =
					new FilterIterator<Gene,Gene>(watson,
					overlapper.findOverlapping(new Point(genome, r.getChrom(), end)).iterator());
				
				while(left.hasNext()) { p.addLeftIdentifier(left.next().getID()); }
				while(right.hasNext()) { p.addRightIdentifier(right.next().getID()); }
				
				promoters.add(p);
			}
		}
		
		return promoters.iterator();
	}
}


