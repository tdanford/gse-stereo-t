package edu.mit.csail.cgs.sigma.expression.models;

import java.util.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.utils.Pair;

public class ExpressionTwoChannelProbe extends StrandedPoint {
	
	private String exptKey;
    private ExpressionProbe fgp, bgp;

	public ExpressionTwoChannelProbe(Genome g, String c, int start, char str, String ek, ExpressionProbe f, ExpressionProbe b) {
		super(g, c, start, str);
		exptKey = ek;
        fgp = f; 
        bgp = b;
        
        if(!fgp.getGenome().equals(g) ||
           !bgp.getGenome().equals(g) || 
           !fgp.getChrom().equals(c) ||
           !bgp.getChrom().equals(c) || 
           fgp.getLocation() != start ||
           bgp.getLocation() != start || 
           fgp.getStrand() != str ||
           bgp.getStrand() != str || 
           !fgp.getExptKey().equals(exptKey) ||
           !bgp.getExptKey().equals(exptKey)) { 
            throw new IllegalArgumentException();
        }
	} 

	public int numValues(boolean fg) { return fg ? fgp.numValues() : bgp.numValues(); }
	public double getValue(boolean fg, int i) { return fg ? fgp.getValue(i) : bgp.getValue(i); }
	public String getExptKey() { return exptKey; }
	
	public int offset(int bp) { 
        return fgp.offset(bp);
	}
    
    public int offset(StrandedRegion r) { 
        return fgp.offset(r);
    }
	
	public Collection<Pair<Double,Double>> getAllValues() { 
		LinkedList<Pair<Double,Double>> vs = new LinkedList<Pair<Double,Double>>();
		for(int i = 0; i < Math.min(fgp.numValues(), bgp.numValues()); i++) {  
            vs.addLast(new Pair<Double,Double>(fgp.getValue(i), bgp.getValue(i)));
		}
		return vs;
	}
	
	public double getLogValue(boolean fg, int i) { return fg ? fgp.getLogValue(i) : bgp.getLogValue(i); }
	public boolean isPositive(boolean fg, int i) { return fg ? fgp.isPositive(i) : bgp.isPositive(i); }
	
    public double max(boolean fg) { return fg ? fgp.max() : bgp.max(); } 
    public double min(boolean fg) { return fg ? fgp.min() : bgp.min(); } 
    public double meanlog(boolean fg) { return fg ? fgp.meanlog() : bgp.meanlog(); } 
    public double mean(boolean fg) { return fg ? fgp.mean() : bgp.mean(); } 
    public double std_deviation(boolean fg) { return fg ? fgp.std_deviation() : bgp.std_deviation(); } 
	
	public int hashCode() { 
		int code = super.hashCode();
		code += exptKey.hashCode(); code *= 37;
		return code;
	}

	public boolean equals(Object o) { 
		if(!(o instanceof ExpressionTwoChannelProbe)) { return false; }
		ExpressionTwoChannelProbe ep = (ExpressionTwoChannelProbe)o;
		if(!exptKey.equals(ep.exptKey)) { return false; }
		return super.equals(ep);
	}
}
