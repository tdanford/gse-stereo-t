package edu.mit.csail.cgs.sigma.expression;

import java.io.*;
import java.util.*;

import Jama.Matrix;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.ewok.ExpressionProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.ewok.StandardProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.sigma.expression.models.Transcript;
import edu.mit.csail.cgs.sigma.expression.noise.*;
import edu.mit.csail.cgs.sigma.expression.regression.*;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.probability.ExponentialDistribution;
import edu.mit.csail.cgs.utils.probability.NormalDistribution;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.ChromRegionIterator;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;

public class ExpressionSearch extends ExprRegression implements ExpressionFinder {

	private BaseExpressionProperties props;
	private String exptKey, strain;
    private Genome genome;
    private int segmentNumProbes;
    private boolean fg;
	
	private ExpressionProbeGenerator prober;
	private StrandFilter<ExpressionProbe> watson, crick;
    private ExponentialDistribution noise;
	
    public ExpressionSearch(BaseExpressionProperties ps, ExponentialDistribution nd) { 
        super(ps);
        props = ps;
        watson = new StrandFilter<ExpressionProbe>('+');
        crick = new StrandFilter<ExpressionProbe>('-');
        segmentNumProbes = props.getMinSegmentProbes();
        noise = nd;
        fg = true;
    }
    
    public ExpressionSearch(BaseExpressionProperties ps, ExponentialDistribution nd, boolean fg) { 
        super(ps);
        props = ps;
        watson = new StrandFilter<ExpressionProbe>('+');
        crick = new StrandFilter<ExpressionProbe>('-');
        segmentNumProbes = props.getMinSegmentProbes();
        noise = nd;
        this.fg = fg;
    }
    
	public void loadData(String ek) {
		super.loadData(ek);
		exptKey = ek;
		strain = props.parseStrainFromExptKey(ek);
        genome = props.getGenome(strain);
		prober = new StandardProbeGenerator(props, exptKey, fg);
	}
	
	public void closeData() {
		super.closeData();
		exptKey = strain = null;
		prober.close();
		prober = null;
        genome=null;
	}
    
    public Collection<Transcript> findExpression() { 
        return analyzeGenome(props.getGenome(strain), noise);
    }
    
    public ExponentialDistribution getNoise() { 
    	return noise;
    }
    
	public Collection<Transcript> 
		analyzeGenome(Genome g, ExponentialDistribution bgDist) {
		
		ChromRegionIterator chroms = new ChromRegionIterator(g);
		LinkedList<Transcript> ts = new LinkedList<Transcript>();
		while(chroms.hasNext()) { 
			NamedRegion chrom = chroms.next();
			ts.addAll(analyzeRegion(chrom, bgDist));
		}
		return ts;
	}

	public Collection<Transcript> analyzeRegion(Region r) { 
		return analyzeRegion(r, noise);
	}

	public Collection<Transcript> analyzeRegion(Region r, ExponentialDistribution d) {
		LinkedList<Transcript> ts = new LinkedList<Transcript>();
		ts.addAll(analyzeStrandedRegion(new StrandedRegion(r, '+'), d));
		ts.addAll(analyzeStrandedRegion(new StrandedRegion(r, '-'), d));		
		return ts;
	}

	public Collection<Transcript> analyzeStrandedRegion(StrandedRegion r) { 
		return analyzeStrandedRegion(r, noise);
	}

	public Collection<Transcript> analyzeStrandedRegion(StrandedRegion r, ExponentialDistribution bgDist) {
		
		NoiseModel m = new PValueNoiseModel(bgDist, 0.1);
		
		Iterator<ExpressionProbe> probes = probes(r);
		SeedArray array = new SeedArray(props, probes, r.getStrand() == '+', m);
		Collection<Seed> seeds = array.seeds(this);
       
		LinkedList<Transcript> trans = new LinkedList<Transcript>();
		for(Seed s : seeds) { 
            if(s.transcript != null) { 
                trans.add(s.transcript);
            }
		}
		return trans;
	}
	
	public Iterator<ExpressionProbe> probes(StrandedRegion r) { 
		Iterator<ExpressionProbe> probed = 
			new FilterIterator<ExpressionProbe,ExpressionProbe>(
					r.getStrand()=='+' ? watson : crick, prober.execute(r));
		return probed;
	}
	
	public Transcript createTranscript(StrandedRegion r) {
		return createTranscript(r, probes(r));
	}
}

class SeedArray { 
	
	public ExpressionProbe[] probes;
	public boolean strand;
	public Genome genome;
	public String chrom;
	public int seedWidth;
	public NoiseModel noiseModel;
	
	public SeedArray(BaseExpressionProperties props, 
		Iterator<ExpressionProbe> ps, boolean s, NoiseModel nm) {

		strand = s;
		noiseModel = nm;
		seedWidth = props.getSeedWidth();
		ArrayList<ExpressionProbe> pl = new ArrayList<ExpressionProbe>();
		genome = null;
		chrom = null;
		while(ps.hasNext()) { 
			ExpressionProbe p = ps.next();
			pl.add(p);
			
			if(genome==null) { 
				genome = p.getGenome();
				chrom = p.getChrom();
			} else { 
				if(!p.getGenome().equals(genome) || 
					!p.getChrom().equals(chrom)) { 
					throw new IllegalArgumentException(String.format("%s:%s != %s:%s", 
							genome.toString(), chrom, p.getGenome().toString(),
							p.toString()));
				}
			}
		}
		probes = pl.toArray(new ExpressionProbe[pl.size()]);
		//System.out.println(String.format("Array: %d probes", probes.length));
	}
    
	public boolean getStrand() { return strand; }
	
	public StrandedRegion intervalRegion(int s, int e) { 
		int start = probes[s].getLocation();
		int end = probes[e].getLocation();
		return new StrandedRegion(genome, chrom, start, end, strand ? '+' : '-');
	}
	
	public Tuple<ExpressionProbe> intervalProbes(int s, int e) {
		return new ProbeTuple(this, s, e+1);
	}
	
	public Tuple<ExpressionProbe> allProbes() {
		return new ProbeTuple(this, 0, probes.length);
	}
	
	public LinkedList<Seed> seeds(ExpressionSearch search) { 
		LinkedList<Seed> sl = new LinkedList<Seed>();
        int i = 0;
        while(i < probes.length) {
			if(isSeed(i)) { 
				int start = i;
				while(isSeed(i)) { 
                    i++;
                }

				int end = i+seedWidth-1;
                Seed s = new Seed(search, this, start, end);
				sl.addLast(s);
			} else { 
			    i++;
            }
		}
		return sl;
	}
	
	public boolean isSeed(int idx) { 
		if(idx >= 0 && idx+seedWidth < probes.length) {
            int diffPos = Math.abs(probes[idx].getLocation() - probes[idx+seedWidth].getLocation());
            if(diffPos > seedWidth*100) { return false; }
            
			for(int i = idx; i < idx+seedWidth; i++) { 
				if(noiseModel.isNoise(probes[i], probes[i].mean())) { 
					return false;
				}
			}
			
			return true;
		}
		return false;
	}
}

class Seed implements Comparable<Seed> { 
    
    public SeedArray array;
    public ExpressionSearch expr;
    
    public int startIdx, endIdx;
    public StrandedRegion region;
    public Transcript transcript;
    public double errorVar;
    public NormalDistribution errorDist;
    
    public Seed(ExpressionSearch es, SeedArray a, int s, int e) { 
        array = a;
        expr = es;
        startIdx = s;
        endIdx = e;
        region = array.intervalRegion(startIdx, endIdx);
        updateTranscript();
    }
    
    public Seed(Seed s) { 
        array = s.array;
        expr = s.expr;
        startIdx = s.startIdx;
        endIdx = s.endIdx;
        region = s.region;
        transcript = s.transcript;
        errorVar = s.errorVar;
        errorDist = s.errorDist;
    }
    
    public int compareTo(Seed s) { 
        return region.compareTo(s.region);
    }
    
    public int hashCode() { 
        return region.hashCode();
    }
    
    public boolean equals(Object o) { 
        if(!(o instanceof Seed)) { 
            return false;
        }
        return ((Seed)o).region.equals(region);
    }
    
    public void combine(Seed s) {
        if(!region.getChrom().equals(s.region.getChrom()) || region.getStrand() != s.region.getStrand()) { 
            throw new IllegalArgumentException();
        }
        
        startIdx = Math.min(s.startIdx, startIdx);
        endIdx = Math.max(s.endIdx, endIdx);
        region = new StrandedRegion(region.getGenome(), region.getChrom(), 
                Math.min(region.getStart(), s.region.getStart()),
                Math.max(region.getEnd(), s.region.getEnd()),
                region.getStrand());
        errorVar = Math.max(errorVar, s.errorVar);
    }
    
    public int startLocation() { 
        if(array.getStrand()) { 
            return array.probes[startIdx].getLocation();
        } else { 
            return array.probes[endIdx].getLocation();
        }
    }
    
    public Vector<ExpressionProbe> probes() { 
        Vector<ExpressionProbe> ps = new Vector<ExpressionProbe>();
        for(int i = startIdx; i <= endIdx; i++) { 
            ps.add(array.probes[i]);
        }
        return ps;
    }
    
    public void updateTranscript() {
        Vector<ExpressionProbe> probes = probes();
        transcript = expr.createTranscript(region, probes.iterator());
        int count = 0;
        double errorSum = 0.0;
        
        if(transcript != null) { 
            for(ExpressionProbe p : probes) { 
                double error = transcript.logError(p);
                if(!Double.isNaN(error) && !Double.isInfinite(error)) { 
                    errorSum += error * error;
                    count += 1;
                }
            }
        }
        
        errorVar = count > 0 ? errorSum / (double)count : 1.0;
        errorDist = new NormalDistribution(0.0, errorVar);
    }
    
    public boolean getStrand() { return array.getStrand(); }
    
    public double logLikelihood(ExpressionProbe p) { 
        if(transcript != null) { 
            return errorDist.calcLogProbability(transcript.logError(p));
        } else { 
            return -9999.9;
        }
    }
    
}

class ProbeTuple extends Tuple.Immutable<ExpressionProbe> {
	
	private int start, end;
	private SeedArray array;
	
	public ProbeTuple(SeedArray a, int s, int e) { 
		start = s; 
		end = e;
		array = a;
	}

	public ExpressionProbe get(int i) {
		return array.probes[start+i];
	}

	public boolean contains(Object o) {
		if(!(o instanceof ExpressionProbe)) { return false; }
		ExpressionProbe ep = (ExpressionProbe)o;
		for(int i = 0; i < size(); i++) { 
			if(ep.equals(get(i))) { 
				return true;
			}
		}
		return false;
	}

	public boolean isEmpty() {
		return start == end;
	}

	public Iterator<ExpressionProbe> iterator() {
		return new ProbeIterator();
	}

	public int size() {
		return end - start;
	}

	public Tuple<ExpressionProbe> subTuple(int i1, int i2) {
		if(i2 < i1) {
			String msg = String.format("i1=%d,i2=%d", i1, i2);
			throw new IllegalArgumentException(msg); 
		}
		return new ProbeTuple(array, start+i1, start+i2);
	}

	public Object[] toArray() {
		Object[] pa = new Object[size()];
		for(int i = 0; i < size(); i++) { 
			pa[i] = get(i);
		}
		return pa;
	}

	public <T> T[] toArray(T[] array) {
		for(int i = 0; i < array.length && i < size(); i++) { 
			array[i] = (T)get(i);
		}
		return array;
	}

	private class ProbeIterator implements Iterator<ExpressionProbe> {
		
		private int idx;
		
		public ProbeIterator() { 
			idx = 0;
		}

		public boolean hasNext() {
			return idx < size();
		}

		public ExpressionProbe next() {
			return get(idx++);
		}

		public void remove() {
			throw new UnsupportedOperationException();
		} 
	}

}
