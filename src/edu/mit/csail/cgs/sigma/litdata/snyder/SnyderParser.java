/*
 * Author: tdanford
 * Date: May 24, 2009
 */
package edu.mit.csail.cgs.sigma.litdata.snyder;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.datasets.species.Organism;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.parsing.alignment.*;

public class SnyderParser {
	
	public static void main(String[] args) { 
		File finput = new File(args[0]);
		int binSize = Integer.parseInt(args[1]);
		int extension = Integer.parseInt(args[2]);
		File foutput = new File(args[3]);
		try {
			SnyderParser parser = new SnyderParser(finput);
			Genome g = Organism.findGenome("SGDv1");
			
			SnyderCoverage cover = new SnyderCoverage(g, binSize, extension);
			parser.addToCoverage(cover);
			
			cover.save(foutput);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	private static String parseChrom(String c) {
		Matcher m = chromPattern.matcher(c);
		if(m.matches()) { 
			return m.group(1);
		}
		return c;
	}
	
	private static Pattern chromPattern = Pattern.compile("(?:chr)(.*)");

	private Map<String,RegionKey> hits;

	public SnyderParser(File f) throws IOException { 
		Set<String> duplicates = new TreeSet<String>();
		BlatPSLParser parser = new BlatPSLParser(true);
		Iterator<BlatPSLEntry> entries = parser.parse(f);
		hits = new HashMap<String,RegionKey>();
		
		while(entries.hasNext()) { 
			BlatPSLEntry e = entries.next();
			String hitName = e.getQname();
			if(e.getMismatch() == 0) { 
				if(hits.containsKey(e.getQname())) { 
					duplicates.add(e.getQname());
					hits.remove(e.getQname());
				} else if(!duplicates.contains(e.getQname())) {
					String chrom = parseChrom(e.getTname());
					int start = e.getTstart();
					int end = e.getTend();
					String strand = String.valueOf(e.getStrand());
					RegionKey key = new RegionKey(chrom, start, end, strand);
					hits.put(e.getQname(), key);
				}
				
				if(hits.size() % 100000 == 0) { 
					System.out.println(String.format("Parsed %d hits.", hits.size()));
				}
			} 
		}
		
		System.out.println(String.format("Finished parsing: %d hits", hits.size()));
	}
	
	public void addToCoverage(SnyderCoverage c) {
		for(String hit : hits.keySet()) {
			RegionKey key = hits.get(hit);
			c.addCover(key);
		}
	}
}
