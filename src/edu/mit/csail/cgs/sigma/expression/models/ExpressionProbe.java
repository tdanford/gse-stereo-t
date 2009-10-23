package edu.mit.csail.cgs.sigma.expression.models;

import java.util.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.Genome;

public class ExpressionProbe extends StrandedPoint {
	
	private String exptKey;
	private double[] vals;
	
	private Double mean; 

	public ExpressionProbe(Genome g, String c, int start, char str, String ek, Collection<Double> vs) {
		super(g, c, start, str);
		exptKey = ek;
		vals = new double[vs.size()];
		int i =0;
		for(Double v : vs) { vals[i++] = v; }
		mean = null;
	} 

	public ExpressionProbe(Genome g, String c, int start, char str, String ek, double v) {
		super(g, c, start, str);
		vals = new double[1];
		vals[0] = v;
		exptKey = ek;
		mean = null;
	} 
	
	public int numValues() { return vals.length; }
	public double getValue(int i) { return vals[i]; }
	public String getExptKey() { return exptKey; }
	
	public int offset(int bp) { 
		if(getStrand() == '+') { 
			return getLocation() - bp;
		} else { 
			return bp - getLocation();
		}
	}
    
    public int offset(StrandedRegion r) { 
        if(r.getStrand() == '+') { 
            return offset(r.getStart());
        } else { 
            return offset(r.getEnd());
        }
    }
	
	public Collection<Double> getAllValues() { 
		LinkedList<Double> vs = new LinkedList<Double>();
		for(int i = 0; i < vals.length; i++) { 
			vs.addLast(vals[i]);
		}
		return vs;
	}
	
	public double getLogValue(int i) { return Math.log(vals[i]); }
	public boolean isPositive(int i) { return vals[i] > 0.0; }
	
	public double max() { 
		double m = vals[0];
		for(int i = 1; i < vals.length; i++) { 
			m = Math.max(m, vals[i]); 
		}
		return m;
	}
	
	public double min() { 
		double m = vals[0];
		for(int i = 1; i < vals.length; i++) { 
			m = Math.min(m, vals[i]); 
		}
		return m;
	}
	
	public Double meanlog() {
		double sum = 0.0;
		int count = 0;
		for(int i = 0; i < vals.length; i++) { 
			double v = Math.log(vals[i]);
			if(!Double.isInfinite(v) && !Double.isNaN(v)) { 
				sum += v;
				count += 1;
			}
		}
		return count > 0 ? sum / (double)count : Double.NaN;		
	}
	
	public double mean() {
		if(mean != null) { return mean; }
		double sum = 0.0;
		for(int i = 0; i < vals.length; i++) { sum += vals[i]; }
		mean = sum / (double)(vals.length);
		return mean;
	}
	
	public double std_deviation() { 
		double mean = mean();
		double sum = 0.0;
		for(int i = 0; i < vals.length; i++) { 
			double diff = (vals[i] - mean);
			sum += diff*diff;
		}
		return Math.sqrt(sum / (double)vals.length);
	}
	
	public int hashCode() { 
		int code = super.hashCode();
		code += exptKey.hashCode(); code *= 37;
		return code;
	}

	public boolean equals(Object o) { 
		if(!(o instanceof ExpressionProbe)) { return false; }
		ExpressionProbe ep = (ExpressionProbe)o;
		if(!exptKey.equals(ep.exptKey)) { return false; }
		return super.equals(ep);
	}
	
	public String toString() {
		String msg = String.format(getChrom() + " : " + getStrand() + " : " + getLocation() + " : " + mean());
		return msg;
	}

}



