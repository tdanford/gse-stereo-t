/*
 * Created on Feb 1, 2008
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma.expression.models;

import java.util.*;
import java.util.regex.*;

import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Mapper;

public class Transcript extends StrandedRegion {
	
    public static Pattern transcriptPattern = Pattern.compile("([^:]*):(\\d+)-(\\d+):([+\\-])\\s+\\((.*)\\)\\s+coeffs\\=([^\\s]*)\\s*.*");
	
	public static Transcript decode(Genome genome, String line) { 
		String[] blocks = line.split("\t");
        Matcher m = transcriptPattern.matcher(blocks[0]);
        if(!m.matches()) { throw new IllegalArgumentException(line); }
        String chrom = m.group(1);
        int start = Integer.parseInt(m.group(2)), end = Integer.parseInt(m.group(3));
        char strand = m.group(4).charAt(0);
        String[] array = m.group(6).split(",");
        double[] params = new double[array.length-1];
        for(int i = 1; i < array.length; i++) { 
            params[i-1] = Double.parseDouble(array[i]);
        }
        return new Transcript(genome, chrom, start, end, strand, params);
	}

    public static String encode(Transcript t) { 
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s:%d-%d:%c", t.getChrom(), t.getStart(), t.getEnd(), t.getStrand()));
        for(int i = 0; i < t.getNumCoefficients(); i++) { 
            sb.append(String.format(",%f", t.getCoefficient(i)));
        }
        return sb.toString();
    }
    
    public static class ParsingMapper implements Mapper<String,Transcript> {
    	
    	private Genome genome;
    	public ParsingMapper(Genome g) { 
    		genome = g;
    	}

		public Transcript execute(String a) {
			return decode(genome, a);
		} 
    	
    }

    private double[] coeffs;
    private TreeSet<Gene> genes;
    
    public Transcript(Genome g, String c, int start, int end, char str, double[] ca) {
        super(g, c, start, end, str);
        coeffs = ca.clone();
        genes = new TreeSet<Gene>();
    } 
    
    public Transcript(Genome g, String c, int start, int end, char str, Double[] ca) {
        super(g, c, start, end, str);
        coeffs = new double[ca.length];
        for(int i = 0; i < ca.length; i++) { coeffs[i] = ca[i]; }
        genes = new TreeSet<Gene>();
    } 
    
    public Transcript(StrandedRegion r, double[] ca) {
        super(r);
        coeffs = ca.clone();
        genes = new TreeSet<Gene>();
    } 
    
    public void addGene(Gene g) { 
        genes.add(g);
    }
    
    public double getCoefficient(int i) { return coeffs[i]; }
    public int getNumCoefficients() { return coeffs.length; }
    
    public Collection<Gene> getGenes() { return genes; }
    
    public double getRMS() { return coeffs[2]; }
    
    public double estimateValue(int bp) { 
    	return Math.exp(estimateLogValue(bp));
    }
    
    public double estimateLogValue(int bp) {
    	/*
        double offset = getStrand() == '+' ? 
                (double)(bp - getStart()) / (double)getWidth() :   
                (double)(getEnd() - bp) / (double)getWidth();
        */
    	double offset = getStrand() == '+' ? 
    			(double)(bp - getStart()) :  
    			(double)(getEnd() - bp);
        return coeffs[0] + offset * coeffs[1];
    }
    
	public double error(ExpressionProbe p) { 
		double predicted = estimateValue(p.getLocation());
		double actual = p.mean();
		return actual - predicted;
	}
    
	public double logError(ExpressionProbe p) { 
		double predicted = estimateLogValue(p.getLocation());
		double actual = Math.log(p.mean());
		return actual - predicted;
	}
    
    // mx + b = 0 ==>
    // x = -b/m
    public Integer findZeroPosition() { 
    	if(coeffs[1] == 0.0) { return null; }
    	int base = getStrand() == '+' ? getStart() : getEnd();
    	int offset = (int)Math.round(-coeffs[0] / coeffs[1]);
    	return getStrand() == '+' ? base + offset : base - offset;
    }
    
    public double getValue() { 
    	return estimateValue((getStrand() == '+' ? getEnd() : getStart())); 
    }
    
    public String toString() { 
    	StringBuilder sb = new StringBuilder(String.format("%s:%c (%.3f)", getLocationString(), getStrand(), getValue()));
    	sb.append(" coeffs=");
    	for(int i = 0; i < coeffs.length; i++) { 
    		if(i > 0) { sb.append(","); }
    		sb.append(coeffs[i]);
    	}
    	return sb.toString();
    }
    
}