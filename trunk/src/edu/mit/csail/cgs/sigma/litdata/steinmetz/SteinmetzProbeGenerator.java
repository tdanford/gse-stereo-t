/**
 * 
 */
package edu.mit.csail.cgs.sigma.litdata.steinmetz;

import java.io.File;
import java.io.IOException;
import java.util.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.OverlappingPointFinder;
import edu.mit.csail.cgs.sigma.expression.ewok.ExpressionProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.utils.iterators.EmptyIterator;
import edu.mit.csail.cgs.ewok.verbs.*;

/**
 * Holds all the Steinmetz probes in memory, searches them dynamically.  This is (now) just 
 * a thin wrapper around the OverlappingPointFinder, which is doing the semi-intelligent thing: 
 * indexing the points by chromosome, and then searching each chromosome using a binary search.  
 * 
 * @author tdanford
 */
public class SteinmetzProbeGenerator implements ExpressionProbeGenerator {

	private Map<String,TreeSet<SteinmetzProbe>> probes;
	private String key;
	
	public SteinmetzProbeGenerator() { 
		this(new SteinmetzProperties());
	}
	
	public SteinmetzProbeGenerator(SteinmetzProperties props) { 
		this(props.getGenome(), props.getDataFile());
	}
	
	public SteinmetzProbeGenerator(Genome g, File f) { 
		try {
			probes = new TreeMap<String,TreeSet<SteinmetzProbe>>();
			key = f.getName();
			System.out.println(String.format("Loading Steinmetz Data: %s", f.getName()));
			Iterator<ExpressionProbe> pitr = new SteinmetzProbeParser(g, f);
			int c = 0;
			while(pitr.hasNext()) { 
				ExpressionProbe p = pitr.next();
				
				int loc = p.getLocation();
				double val = p.getValue(0);
				char strand = p.getStrand();
				String chrom = p.getChrom();

				if(!probes.containsKey(chrom)) { 
					probes.put(chrom, new TreeSet<SteinmetzProbe>());
				}
				probes.get(chrom).add(new SteinmetzProbe(loc, val, strand));
				c += 1;
			}
			
			System.out.println(String.format("\tLoaded: %d probes", c));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

	public Iterator<ExpressionProbe> execute(Region a) {
		if(!probes.containsKey(a.getChrom())) { 
			return new EmptyIterator<ExpressionProbe>();
		} else { 
			return new MapperIterator<SteinmetzProbe,ExpressionProbe>(
					new SteinmetzExpressionProbeMapper(a.getGenome(), a.getChrom(), key),
					new FilterIterator<SteinmetzProbe,SteinmetzProbe>(
							new SteinmetzProbeContainmentFilter(a),
							probes.get(a.getChrom()).iterator()));
		}
	}

	public void close() {
		probes = null;
	}

	public boolean isClosed() {
		return probes == null;
	}
}

class SteinmetzExpressionProbeMapper implements Mapper<SteinmetzProbe,ExpressionProbe> { 
	private Genome genome;
	private String chrom;
	private String key;
	
	public SteinmetzExpressionProbeMapper(Genome g, String c, String k) { 
		genome = g; 
		chrom = c;
		key = k;
	}
	
	public ExpressionProbe execute(SteinmetzProbe a) {
		return new ExpressionProbe(genome, chrom, a.location, a.strand, key, a.value);
	}
}

class SteinmetzProbeContainmentFilter implements Filter<SteinmetzProbe,SteinmetzProbe> { 
	private Region region;
	
	public SteinmetzProbeContainmentFilter(Region r) { 
		region = r;
	}
	
	public SteinmetzProbe execute(SteinmetzProbe p) { 
		return region.getStart() <= p.location && region.getEnd() >= p.location ? 
				p : null;
	}
}

class SteinmetzProbe implements Comparable<SteinmetzProbe> { 
	
	public Character strand;
	public Integer location;
	public Double value;
	
	public SteinmetzProbe(int l, double v, char c) { 
		location = l; 
		strand = c;
		value = v;
	}
	
	public int compareTo(SteinmetzProbe p) { 
		return location - p.location;
	}
	
	public int hashCode() { 
		int code = 17;
		code += location; code *= 37;
		code += (int)strand.charValue(); code *= 37;
		return code;
	}
	
	public String toString() { return String.format("%d:%c:%.3f", location, strand, value); }
	
	public boolean equals(Object o) { 
		if(!(o instanceof SteinmetzProbe)) { 
			return false;
		}
		SteinmetzProbe p = (SteinmetzProbe)o;
		return p.location.equals(location) && 
			strand.equals(p.strand) && 
			Math.abs(p.value - value) <= 1.0e-6;
	}
}