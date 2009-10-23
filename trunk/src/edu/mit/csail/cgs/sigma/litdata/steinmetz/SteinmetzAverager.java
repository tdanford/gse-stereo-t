package edu.mit.csail.cgs.sigma.litdata.steinmetz;

import java.util.*;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;

public class SteinmetzAverager {

	private SteinmetzProbeGenerator gen;
	
	public SteinmetzAverager(SteinmetzProbeGenerator spg) { 
		gen = spg;
	}
	
	public SteinmetzAverager(SteinmetzProperties props) { 
		this(new SteinmetzProbeGenerator(props));
	}
	
	public SteinmetzAverager() { 
		this(new SteinmetzProperties());
	}
	
	public void close() { 
		gen.close();
		gen = null;
	}
	
	public Double mean(StrandedRegion region) { 
		Iterator<ExpressionProbe> probes = gen.execute(region);
		double sum = 0.0;
		int count = 0;
		while(probes.hasNext()) { 
			ExpressionProbe p = probes.next();
			if(p.getStrand() == region.getStrand()) { 
				double value = p.mean();
				sum += value;
				count += 1;
			}
		}
		
		return count > 0 ? sum / (double)count : null;
	}

	public Double var(StrandedRegion region) { 
		Iterator<ExpressionProbe> probes = gen.execute(region);
		LinkedList<Double> vals = new LinkedList<Double>();
		double sum = 0.0;
		int count = 0;
		while(probes.hasNext()) { 
			ExpressionProbe p = probes.next();
			if(p.getStrand() == region.getStrand()) { 
				double value = p.mean();
				vals.add(value);
				sum += value;
				count += 1;
			}
		}

		if(count == 0) { return null; }
		double mean = sum / (double)count;
		sum = 0.0;
		
		for(Double v : vals) { 
			double vv = v - mean;
			sum += (vv * vv);
		}
		
		return sum / (double)count;
	}
}
