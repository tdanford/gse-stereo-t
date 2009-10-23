/*
 * Author: tdanford
 * Date: Jun 2, 2009
 */
package edu.mit.csail.cgs.sigma.litdata.steinmetz;

import java.util.*;

import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.expression.StrandFilter;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.workflow.models.FileInputData;

public class SteinmetzPackager {
	
	private SteinmetzProbeGenerator generator;
	private LinkedList<FileInputData> datalist;
	
	private Filter<ExpressionProbe,ExpressionProbe> watsonFilter, crickFilter;
	private int maxProbes;

	public SteinmetzPackager(SteinmetzProperties prps) { 
		this(prps, 1000);
	}
	
	public SteinmetzPackager(SteinmetzProperties prps, int maxprobes) { 
		generator = new SteinmetzProbeGenerator(prps);
		datalist = new LinkedList<FileInputData>();
		maxProbes = maxprobes;

		Genome genome = prps.getGenome();
		load(genome);
	}
	
	private void load(Genome g) { 
		GenomeExpander<ExpressionProbe> exp = new GenomeExpander<ExpressionProbe>(generator);
		watsonFilter = new StrandFilter<ExpressionProbe>('+');
		crickFilter = new StrandFilter<ExpressionProbe>('-');
		load(new FilterIterator<ExpressionProbe,ExpressionProbe>(watsonFilter, exp.execute(g)));
		load(new FilterIterator<ExpressionProbe,ExpressionProbe>(crickFilter, exp.execute(g)));
	}
	
	private void load(Iterator<ExpressionProbe> itr) {
		ExpressionProbe next = null;
		LinkedList<ExpressionProbe> probelist = 
			new LinkedList<ExpressionProbe>();
		
		while(itr.hasNext()) { 
			next = itr.next();
			
			if(!probelist.isEmpty() && 
					(separate(next, probelist.getLast()) ||
					 probelist.size() > maxProbes)) { 
				datalist.add(packageProbes(probelist));
				probelist.clear();
			}
			
			probelist.add(next);
		}
		
		if(!probelist.isEmpty()) { 
			datalist.add(packageProbes(probelist));			
		}
	}
	
	private boolean separate(ExpressionProbe next, ExpressionProbe last) {
		return !last.getChrom().equals(next.getChrom()) ||
			Math.abs(last.getLocation() - next.getLocation()) >= 100;
	}

	private FileInputData packageProbes(Collection<ExpressionProbe> ps) {
		String chrom = null, strand = null;
		Vector<Integer> locs = new Vector<Integer>();
		Vector<Double[]> vals = new Vector<Double[]>();
		
		for(ExpressionProbe p : ps) { 
			if(chrom == null) { 
				chrom = p.getChrom();
				strand = String.valueOf(p.getStrand());
			}
			
			locs.add(p.getLocation());
			vals.add(new Double[] { p.getValue(0) });
		}
		
		return new FileInputData(chrom, strand, locs, vals);
	}
	
	public Iterator<FileInputData> data() { return datalist.iterator(); }
	
	public void remove() {
		throw new UnsupportedOperationException();
	}
}

