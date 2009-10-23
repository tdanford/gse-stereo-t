/*
 * Author: tdanford
 * Date: May 29, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.assessment;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.IteratorCacher;
import edu.mit.csail.cgs.sigma.OverlappingRegionKeyFinder;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;
import edu.mit.csail.cgs.sigma.tgraphs.GeneKey;

/**
 * Tries to assign a particular expression value to a given (arbitrary) region
 * of coordinates.  
 * 
 * Built, originally, for Robin's analysis of Josh's KHD1 data.
 * 
 * @author tdanford
 */
public class RegionExpression {
	
	public static void main(String[] args) {
		int i = 0;
		File f = args.length > 0 ? new File(args[i++]) : new File("C:\\Documents and Settings\\tdanford\\Desktop\\sigma_extended_transcripts.txt");
		String expt = args.length > i ? args[i++] : "matalpha";
		String key = args.length > i ? args[i++] : "txnsigma";
		
		try {
			RegionExpression exp = new RegionExpression(key, expt, f);
			exp.estimateExpression();
			exp.printExpressionEstimates(System.out);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private WorkflowProperties props;
	private String key, expt;
	private WorkflowIndexing indexing;
	private WholeGenome genome; 
	private IteratorCacher<GeneKey> regions;
	private OverlappingRegionKeyFinder<DataSegment> segs;
	
	private Map<GeneKey,Double> expressionEstimates;
	
	public RegionExpression(String k, String e, File regs) throws IOException {
		key = k; expt = e;
		regions = new IteratorCacher<GeneKey>(regions(regs));
		props = new WorkflowProperties();
		indexing = props.getIndexing(key);
		genome = WholeGenome.loadWholeGenome(props, key);
		genome.loadIterators();
		
		segs = new OverlappingRegionKeyFinder<DataSegment>(
				genome.getTranscribedSegments(expt));
	}
	
	public void printExpressionEstimates(PrintStream ps) { 
		for(GeneKey gk : expressionEstimates.keySet()) { 
			Double exp = expressionEstimates.get(gk);
			ps.println(String.format("%s\t%.3f", gk.id, exp));
		}
	}
	
	public void estimateExpression() { 
		expressionEstimates = new HashMap<GeneKey,Double>();
		Iterator<GeneKey> itr = regions.iterator();
		
		String strain = props.parseStrainFromKey(key);
		Integer[] channels = indexing.findChannels(strain, expt);
		
		while(itr.hasNext()) { 
			GeneKey g = itr.next();
			RegionKey threeprime = threePrimeZone(g);
			int threePrimePoint = g.strand.equals("+") ? g.end : g.start;
			
			Collection<DataSegment> oversegs = segs.findStrandedOverlapping(threeprime);
			if(oversegs.isEmpty()) { 
				oversegs = segs.findStrandedOverlapping(g);
			}

			overSearch: for(DataSegment s : oversegs) { 
				double predicted = s.predicted(channels, threePrimePoint);
				expressionEstimates.put(g, predicted);

				break overSearch;
			}
		}
	}
	
	public RegionKey threePrimeZone(GeneKey g) {
		return g.threePrimeZone();
	}
	
	public Iterator<GeneKey> regions(File f) throws IOException { 
		String line = null;
		ArrayList<GeneKey> gks = new ArrayList<GeneKey>();
		BufferedReader br = new BufferedReader(new FileReader(f));
		while((line = br.readLine()) != null) {
			line = line.trim();
			if(line.length() > 0) { 
				GeneKey gk = parseRegion(line);
				gks.add(gk);
			}
		}
		br.close();
		
		return gks.iterator();
	}
	
	public static GeneKey parseRegion(String line) { 
		String[] a = line.split("\\s+");
		String chrom = a[0];
		String id = a[1];
		int start = Integer.parseInt(a[2]);
		int end = Integer.parseInt(a[3]);
		String strand = a[4].substring(0, 1);
		return new GeneKey(chrom, start, end, strand, id);
	}
}

