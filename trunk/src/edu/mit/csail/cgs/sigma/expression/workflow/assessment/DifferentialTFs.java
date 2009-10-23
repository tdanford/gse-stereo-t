/*
 * Author: tdanford
 * Date: Jun 4, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.assessment;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.genes.*;
import edu.mit.csail.cgs.sigma.motifs.*;
import edu.mit.csail.cgs.sigma.networks.*;
import edu.mit.csail.cgs.sigma.tgraphs.GeneKey;
import edu.mit.csail.cgs.utils.SetTools;

public class DifferentialTFs {
	
	public static void main(String[] args) { 
		WorkflowProperties props = new WorkflowProperties();
		String key = "txns288c";
		String expt = "matalpha";
		String diffFilename = String.format("%s_%s_diffgenes.txt", key, expt);
		File diffFile = new File(props.getDirectory(), diffFilename);
		try {
			DifferentialTFs tfs = new DifferentialTFs(props, key, expt, diffFile);
			tfs.printSummary(System.out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private TFList tfList;
	private TreeSet<String> tfIDs, diffTFs;
	
	private WorkflowProperties props;
	private String key, strain, expt;
	private GeneNameAssociation assoc;
	
	//private DifferentialExpression diffExpr;
	private GeneSet diffExpr;
	
	private GenePromoterArchitecture architecture;
	private RegulatoryNetwork regNetwork;
	
	private Map<String,Gene> geneMap;
	private Map<String,Set<String>> diffRegulators;

	public DifferentialTFs(WorkflowProperties prps, String k, String e, File diffFile) 
		throws IOException { 
		
		MotifProperties mps = new MotifProperties();
		GeneAnnotationProperties gps = new GeneAnnotationProperties();
		
		tfList = new TFList(mps);
		props = prps; 
		key = k; expt = e;

		strain = props.parseStrainFromKey(key);
		assoc = gps.getGeneNameAssociation(strain);
		
		tfIDs = new TreeSet<String>();
		
		for(String tf : tfList.getTFs()) { 
			String id = tf;
			if(assoc.containsName(id)) { 
				tfIDs.addAll(assoc.getIDs(id));
			} else if(assoc.containsID(id)) { 
				tfIDs.add(id);
			}
		}
		
		System.out.println(String.format("Found %d TFs.", tfIDs.size()));
		
		File archFile = gps.getPromoterArchitectureFile(strain);
		architecture = new GenePromoterArchitecture(gps, strain, archFile);
		
		geneMap = new TreeMap<String,Gene>();
		Iterator<Gene> genes = architecture.genes();
		while(genes.hasNext()) {
			Gene gene = genes.next();
			geneMap.put(gene.getID(), gene);
		}
		System.out.println(String.format("Loaded %d Genes/Promoters", geneMap.size()));
		
		SetTools<String> sets = new SetTools<String>();
		int numTFs = tfIDs.size();
		Set<String> removed = sets.subtract(tfIDs, geneMap.keySet());
		tfIDs = new TreeSet<String>(sets.intersection(tfIDs, geneMap.keySet()));
		if(tfIDs.size() < numTFs) { 
			System.out.println(String.format("\tRemoved %d tfs without promoters.", numTFs-tfIDs.size()));
			for(String id : removed) { 
				System.out.println("\t" + assoc.getName(id));
			}
		}
		
		//diffExpr = new DifferentialExpression(props, key, expt);
		diffExpr = new GeneSet(assoc, diffFile);
		
		diffTFs = new TreeSet<String>();
		for(String tfID : tfIDs) {
			if(isDifferentialGene(tfID)) { 
				System.out.println(String.format("+ %s", assoc.getName(tfID)));
				diffTFs.add(tfID);
			}
		}
		
		regNetwork = new RegulatoryNetwork(props.getSigmaProperties(), 
				mps, gps, strain);
		regNetwork.loadData();
		
		diffRegulators = new TreeMap<String,Set<String>>();
		
		for(String id : geneMap.keySet()) { 
			RegulatoryUnit regUnit = regNetwork.getRegulatoryUnit(id);
			Set<String> regTFs = regUnit.getRegulatingTFs();
			//System.out.println(String.format("%s -> %s", id, regTFs.toString()));
			Set<String> regs = namesToIDs(regTFs);
			Set<String> diffregs = sets.intersection(regs, diffTFs);
			diffRegulators.put(id, diffregs);
		}
	}
	
	public boolean isDifferentialGene(String id) { 
		Gene gene = geneMap.get(id);
		GeneKey geneKey = new GeneKey(gene);
		//return diffExpr.strandedDifferentiallyExpressed(geneKey.threePrimeZone());
		return diffExpr.containsKey(id);
	}
	
	public void printSummary(PrintStream ps) { 
		ps.println(String.format("%d / %d differential TFs",
				diffTFs.size(), tfIDs.size()));
		int maxDiffReg = 0;
		for(String id : diffRegulators.keySet()) { 
			maxDiffReg = Math.max(maxDiffReg, diffRegulators.get(id).size());
		}
		
		ps.println(String.format("#Diffs:\t#Genes:"));
		for(int i = 0; i <= maxDiffReg; i++) { 
			Set<String> idset = new TreeSet<String>();
			Set<String> diffset = new TreeSet<String>();
			for(String id : diffRegulators.keySet()) { 
				if(diffRegulators.get(id).size() == i) { 
					idset.add(id);
					if(isDifferentialGene(id)) { 
						diffset.add(id);
					}
				}
			}
			
			ps.println(String.format("%d \t%d / %d", i, diffset.size(), idset.size()));
		}
	}
	
	public Set<String> idsToNames(Set<String> ids) { 
		Set<String> names = new TreeSet<String>();
		for(String id : ids) { names.add(assoc.getName(id)); }
		return names;
	}

	public Set<String> namesToIDs(Set<String> names) { 
		Set<String> ids = new TreeSet<String>();
		for(String name : names) { ids.addAll(assoc.getIDs(name)); }
		return ids;
	}
}
