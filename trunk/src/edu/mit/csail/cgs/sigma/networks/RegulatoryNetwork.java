package edu.mit.csail.cgs.sigma.networks;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;
import java.util.logging.*;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.motifs.*;
import edu.mit.csail.cgs.sigma.genes.*;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.SetTools;
import edu.mit.csail.cgs.utils.database.DatabaseFactory;
import edu.mit.csail.cgs.utils.database.Sequence;
import edu.mit.csail.cgs.utils.database.UnknownRoleException;
import edu.mit.csail.cgs.utils.graphs.DirectedGraph;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.motifs.WeightMatrix;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.ewok.verbs.motifs.WeightMatrixHit;
import edu.mit.csail.cgs.ewok.verbs.motifs.WeightMatrixScoreProfile;
import edu.mit.csail.cgs.ewok.verbs.motifs.WeightMatrixScorer;

/**
 * @author tdanford
 * 
 * RegulatoryNetwork captures, for a single strain, the total set of genes, the subset of those
 * genes that are TFs, and the regulatory connections between TFs and Genes as dictated by a set
 * of motifs embedded in the sequence.  
 */
public class RegulatoryNetwork {
	
	public static void main(String[] args) { 
		buildNetwork();
		//printScores();
	}
	
	private  static void buildNetwork(){
		SigmaProperties sp = new SigmaProperties();
		MotifProperties mp = new MotifProperties(sp, "default");
		GeneAnnotationProperties gp = new GeneAnnotationProperties(sp, "default");
        SudeepExpressionProperties ep = new SudeepExpressionProperties(sp, "sudeep");
		
		String s288c = "s288c";
		String sigma = "sigma";
        
        Collection<String> cells = ep.getStrainCellKeys(s288c);
        
        Logger logger = ep.getLogger("differental_regulation");
		
        logger.log(Level.INFO, "Building s288c Network");
		RegulatoryNetwork s288cNetwork = new RegulatoryNetwork(sp, mp, gp, s288c);
		s288cNetwork.loadData();
		
        logger.log(Level.INFO, "Building sigma Network");
		RegulatoryNetwork sigmaNetwork = new RegulatoryNetwork(sp, mp, gp, sigma);
		sigmaNetwork.loadData();
        
	}
	
    private static void printScores(){
		SigmaProperties sp = new SigmaProperties();
		MotifProperties mp = new MotifProperties(sp, "default");
		GeneAnnotationProperties gp = new GeneAnnotationProperties(sp, "default");
        SudeepExpressionProperties ep = new SudeepExpressionProperties(sp, "sudeep");
		
		String s288c = "s288c";
		String sigma = "sigma";
		
		RegulatoryNetwork s288cNetwork = new RegulatoryNetwork(sp, mp, gp, s288c);
		s288cNetwork.loadData();
		//s288cNetwork.printMotifScoreProfiles("YBR296C");
		//s288cNetwork.printBindingMatrix();
    }

	private SigmaProperties props;
	private MotifProperties motifProps;
	private GeneAnnotationProperties geneProps;
	
	private String strain;
	private Genome genome;
	private Map<String,RegulatoryUnit> regUnits;
	private Map<String,Gene> genes;
	private Map<String,Promoter> promoters;
	private Map<String,WeightMatrix> tfMatrices;
	
    private Logger logger;
	private TFList tfs;
	private GeneNameAssociation geneNamer;
	private Motifs motifs;
	private int promoterWidth;
	
	public RegulatoryNetwork(SigmaProperties sp, MotifProperties mps, 
			GeneAnnotationProperties gps, String str) { 
		props = sp;
		motifProps = mps;
		geneProps = gps;
		promoterWidth = 500;
		
        logger = props.getLogger("RegulatoryNetwork");
		strain = str;
		geneNamer = geneProps.getGeneNameAssociation(strain);
		genome = props.getGenome(strain);
		tfs = new TFList(motifProps);
		
		regUnits = new HashMap<String,RegulatoryUnit>();
		tfMatrices = new HashMap<String,WeightMatrix>();
		genes = new TreeMap<String,Gene>();
		promoters = new TreeMap<String,Promoter>();
		
		motifs = new Motifs(mps, strain);
	}
    
	public void loadData() {
        loadWeightMatrices();
        loadGenes();
        loadRegulatoryUnits();
        loadPromoters();
	}
	
	public void closeData() { 
		closeWeightMatrices();
		closeGenes();
		closeRegulatoryUnits();
		closePromoters();
	}

	public Region getPromoter(Gene g) {
		if(promoters.containsKey(g.getID())) {
			return promoters.get(g.getID());
		} else { 
			int s = 0, e = genome.getChromLength(g.getChrom());
			if(g.getStrand() == '+') {
				s = Math.max(s, g.getStart()-promoterWidth);
				e = g.getStart();
			} else { 
				s = g.getEnd();
				e = Math.min(e, g.getEnd()+promoterWidth);
			}
			return new Region(g.getGenome(), g.getChrom(), s, e);
		}
	}

	public Set<String> getTFNames() { 
		TreeSet<String> sorted = new TreeSet<String> (tfMatrices.keySet());
		return sorted; 
	}
    public WeightMatrix getWeightMatrix(String tf){
    	return tfMatrices.get(tf);
    }
    public Set<String> getAllGeneIDs() { 
        return regUnits.keySet(); 
    }
    public RegulatoryUnit getRegulatoryUnit(String targetGene) { 
        return regUnits.get(targetGene); 
    }
    
    public Set<String> getDownstreamGeneIDs(String tf) { 
        TreeSet<String> downstream = new TreeSet<String>();
        for(String id : regUnits.keySet()) { 
            if(regUnits.get(id).containsMotif(tf)) { 
                downstream.add(id);
            }
        }
        return downstream;
    }
    
    public GeneNameAssociation getGeneNameAssociation(){
    	return geneNamer;
    }
    
	public DirectedGraph createNetworkGraph() { 
		DirectedGraph dg = new DirectedGraph();
		for(String g : regUnits.keySet()) { 
			dg.addVertex(g);
		}
		
		for(String gname : regUnits.keySet()) { 
			RegulatoryUnit ru = regUnits.get(gname);
			Set<String> tfs = ru.getRegulatingTFs();
			for(String tf : tfs) {
				for(String tfid : geneNamer.getIDs(tf)) {
					if(regUnits.containsKey(tfid)) { 
						dg.addEdge(tfid, gname);
					}
				}
			}
		}
		
		return dg;
	}
	
	public void compareToNetwork(RegulatoryNetwork net) { 
		SetTools<String> tools = new SetTools<String>();
		Set<String> commonGenes = tools.intersection(regUnits.keySet(), net.regUnits.keySet());
		Set<String> myGenes = tools.subtract(regUnits.keySet(), net.regUnits.keySet());
		
		for(String g : commonGenes) { 
			RegulatoryUnit myUnit = regUnits.get(g);
			RegulatoryUnit otherUnit = net.regUnits.get(g);
			
			myUnit.updateChanges(otherUnit);
		}
		
		for(String g : myGenes) { 
			regUnits.get(g).setStrainCode(RegulatoryUnit.StrainUniqueCode.STRAIN_UNIQUE);
		}
	}

    private void loadPromoters() { 
    	PromoterIdentification ident = new PromoterIdentification(geneProps, strain);
    	Iterator<Promoter> all = ident.getAllPromoters();
    	while(all.hasNext()) { 
    		Promoter p = all.next();
    		for(String id : p.getDownstreamIDs()) { 
    			promoters.put(id, p);
    		}
    	}
    }
    
    private void closePromoters() { 
    	promoters.clear();
    }
    
    private void loadWeightMatrices() { 
        motifs.loadData();
        
        for(WeightMatrix wm : motifs.getMatrices()) { 
            tfMatrices.put(wm.name, wm);
            //System.out.println(String.format("MATRIX: %s", wm.name));
        }
    }
    
    private void closeWeightMatrices() { 
    	tfMatrices.clear();
    }
    
    private void loadGenes() { 
        ChromRegionIterator chroms = new ChromRegionIterator(genome);
        Iterator<Region> rchroms = 
            new MapperIterator<NamedRegion,Region>(new CastingMapper<NamedRegion,Region>(), chroms);
        Iterator<Gene> geneitr = new ExpanderIterator<Region,Gene>(props.getGeneGenerator(strain), rchroms);
        while(geneitr.hasNext()) { 
            Gene g = geneitr.next();
            RegulatoryUnit ru = new RegulatoryUnit(g);
            
            genes.put(g.getID(), g);
        }        
    }
    
    private void closeGenes() { 
    	genes.clear();
    }
    
    private void closeRegulatoryUnits() {
    	regUnits.clear();
    }
    
    private void loadRegulatoryUnits() {
        regUnits.clear();
        
        String filename = String.format("%s_regulatory_network.txt", strain);
        File regFile = new File(props.getBaseDir(), filename);

        try { 
            if(regFile.exists()) { 
                for(String gid : genes.keySet()) { 
                    Gene g = genes.get(gid);
                    RegulatoryUnit ru = new RegulatoryUnit(g);
                    regUnits.put(g.getID(), ru);
                }
                
                String line = null;
                BufferedReader br = new BufferedReader(new FileReader(regFile));
                while((line = br.readLine()) != null) { 
                    line = line.trim();
                    if(line.length() > 0) { 
                        String[] a = line.split("\\s+");
                        String id = a[0];
                        if(regUnits.containsKey(id)) { 
                            regUnits.get(id).load(line);
                        }
                    }
                }
                br.close();
                
            } else { 
                PrintStream ps = new PrintStream(new FileOutputStream(regFile));
                int i = 0;

                for(String gid : genes.keySet()) { 
                    Gene g = genes.get(gid);
                    RegulatoryUnit ru = new RegulatoryUnit(g);
                    regUnits.put(g.getID(), ru);

                    Region prom = getPromoter(g);
                    
                    Collection<WeightMatrixHit> hits = motifs.findAllMotifs(tfMatrices.values(), prom);
                    for(WeightMatrixHit h : hits) { 
                        ru.addWeightMatrixHit(h);
                    }                    

                    ru.save(ps);
                    logger.log(Level.INFO, String.format("%d Regulatory Unit: %s", i++, g.getID()));
                }

                ps.close();
            }

        } catch(IOException e) { 
            e.printStackTrace(System.err);
        }
    }
    
    // print the motif score for each position of the promoter
    private void printMotifScoreProfiles(String gene){
        Gene g = genes.get(gene);
        Region promoter = getPromoter(g);
		Collection<WeightMatrix> matrices = motifs.getMatrices();
		Map<String, WeightMatrixScoreProfile> profiles = new HashMap<String, WeightMatrixScoreProfile>();

		for(WeightMatrix m : matrices) { 
	        WeightMatrixScorer scorer = new WeightMatrixScorer(m);
	        WeightMatrixScoreProfile profile = scorer.execute(promoter);
	        profiles.put(m.name, profile);
		}
		
		File file = new File(gene+"_motif_score_profiles.txt");
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(file));
			StringBuilder sb = new StringBuilder("");
			for (String p:profiles.keySet()){
				sb.append(p+"\t");
				double[] scores = profiles.get(p).getForwardScores();
				for (int i=0;i<scores.length;i++){
					sb.append(scores[i]+"\t");
				}
				sb.append("\n");
				sb.append(p+"_R\t");
				scores = profiles.get(p).getReverseScores();
				for (int i=0;i<scores.length;i++){
					sb.append(scores[i]+"\t");
				}
				sb.append("\n");
				ps.print(sb.toString());
				sb = new StringBuilder("");
			}
			ps.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }
    

}

class Ranking { 
    public int index, total;
    
    public Ranking(int i, int t) { 
        index = i;
        total = t;
    }
}
