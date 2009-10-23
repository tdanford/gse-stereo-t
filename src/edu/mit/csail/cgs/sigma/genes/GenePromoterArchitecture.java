/*
 * Author: tdanford
 * Date: Feb 10, 2009
 */
package edu.mit.csail.cgs.sigma.genes;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.ModelInput;

public class GenePromoterArchitecture {
	
	public static void main(String[] args) { 
		try {
			String strain = args[0];
			GeneAnnotationProperties gaps = new GeneAnnotationProperties();
			File output = gaps.getPromoterArchitectureFile(strain);

			GenePromoterArchitecture arch = new GenePromoterArchitecture(gaps, strain);
			arch.save(output);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String strain;
	private File promoterFile;
	private Map<Gene,Promoter> promoters;
	
	public GenePromoterArchitecture(GeneAnnotationProperties ps, String str, File f) 
		throws IOException {
		
		strain = str;
		promoterFile = f;
		promoters = new TreeMap<Gene,Promoter>();
		
		Genome g = ps.getSigmaProperties().getGenome(strain);
		ModelInput<PromoterGenePair> input = 
			new ModelInput.LineReader<PromoterGenePair>(PromoterGenePair.class, 
					new FileInputStream(f));
		PromoterGenePair pgp = null;
		while((pgp = input.readModel()) != null) { 
			Gene gene = pgp.getGene(g);
			Promoter p = pgp.getPromoter(g);
			promoters.put(gene, p);
		}
		input.close();
	}

	public GenePromoterArchitecture(GeneAnnotationProperties ps, String str) 
		throws IOException {
		
		strain = str;
		SigmaProperties sprops = ps.getSigmaProperties();
		promoterFile = ps.getPromoterFile(strain);
		promoters = new TreeMap<Gene,Promoter>();
		
		Genome genome = sprops.getGenome(strain);
		GeneGenerator gen = sprops.getGeneGenerator(strain);
		Iterator<Promoter> proms = new Parser<Promoter>(promoterFile, new Promoter.Decoder(genome));
		while(proms.hasNext()) { 
			Promoter prom = proms.next();
			//System.out.println(prom.toString());
			if(prom.isRightRegulating()) { 
				for(String id : prom.getRightIDs()) { 
					Iterator<Gene> genes = gen.byName(id);
					while(genes.hasNext()) { 
						Gene gene = genes.next();
						if(!promoters.containsKey(gene)) { 
							promoters.put(gene, prom);
							//System.out.println(String.format("\t->%s", gene.getName()));
						}
					}
				}
			}
			if(prom.isLeftRegulating()) { 
				for(String id : prom.getLeftIDs()) { 
					Iterator<Gene> genes = gen.byName(id);
					while(genes.hasNext()) { 
						Gene gene = genes.next();
						if(!promoters.containsKey(gene)) { 
							promoters.put(gene, prom);
							//System.out.println(String.format("\t%s<-", gene.getName()));
						}
					}
				}
			}
		}
		
		gen.close();
	}
	
	public int size() { return promoters.size(); }
	public Iterator<Gene> genes() { return promoters.keySet().iterator(); }
	public Promoter getPromoter(Gene g) { return promoters.get(g); }
	
	public void save(File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		for(Gene g : promoters.keySet()) { 
			Promoter p = promoters.get(g);
			PromoterGenePair pair = new PromoterGenePair(g, p);
			ps.println(pair.asJSON().toString());
		}
		ps.close();
	}
	
	public static class PromoterGenePair extends Model {
		
		public String chrom;
		public String geneID, geneName, geneSource;
		public Integer geneStart, geneEnd, pStart, pEnd;
		public String geneStrand;
		public String[] pLeft, pRight;
		
		public PromoterGenePair() {}
		
		public PromoterGenePair(Gene g, Promoter p) { 
			chrom = g.getChrom();
			geneID = g.getID();
			geneName = g.getName();
			geneSource = g.getSource();
			geneStart = g.getStart();
			geneEnd = g.getEnd();
			pStart = p.getStart();
			pEnd = p.getEnd();
			geneStrand = String.valueOf(g.getStrand());
			
			Set<String> pleft = p.getLeftIDs();
			Set<String> pright = p.getRightIDs();
			
			pLeft = pleft.toArray(new String[0]);
			pRight = pright.toArray(new String[0]);
		}
		
		public Gene getGene(Genome g) { 
			return new Gene(g, chrom, geneStart, geneEnd, geneID, 
					geneName, geneStrand.charAt(0), geneSource);
		}
		
		public Promoter getPromoter(Genome g) { 
			Promoter p = new Promoter(g, chrom, pStart, pEnd);
			for(int i = 0; i < pLeft.length; i++) { 
				p.addLeftIdentifier(pLeft[i]);
			}
			for(int i = 0; i < pRight.length; i++) { 
				p.addRightIdentifier(pRight[i]);
			}
			return p;
		}
	}
}
