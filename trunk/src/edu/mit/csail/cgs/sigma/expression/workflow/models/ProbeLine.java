/*
 * Author: tdanford
 * Date: Jan 6, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.models;

import java.util.*;
import java.util.regex.*;

import edu.mit.csail.cgs.utils.json.JSONObject;
import edu.mit.csail.cgs.utils.models.Model;

/**
 * Takes a String representing a probe with the given information:<br>
 * <pre>
 * chrom	strand	genomic_coord	channel_1/exp_1	channel_2/exp_2 ... channel_K/exp_K
 * 
 * E.g.
 * 7	+	53	-0.52
 * </pre>
 * and stores it as an object with the corresponding fields.
 * @author tdanford
 *
 */
public class ProbeLine extends Model implements Comparable<ProbeLine> {
	
	private static Pattern matchingRegex = Pattern.compile("^([^\\s]+)\\s+([+-])\\s+(\\d+)\\s+(.*)$");
	
	public String chrom;
	public String strand;
	public Integer offset;
	public Double[] values;
	
	public ProbeLine() { 
	}
	
	public ProbeLine(JSONObject obj) { 
		super(obj);
	}
	
	public ProbeLine(String c, String s, Integer off, Double[] val) { 
		chrom = c;
		strand = s;
		offset = off;
		values = val.clone();
	}
	
	public ProbeLine(String line) { 
		this();
		Matcher m = matchingRegex.matcher(line);
		if(!m.matches()) { throw new IllegalArgumentException(line); }
		chrom = m.group(1);
		strand = m.group(2);
		offset = Integer.parseInt(m.group(3));
		String[] arrstr = m.group(4).split("\\s+");
		values = new Double[arrstr.length];
		for(int i = 0; i < values.length; i++) {
			try { 
				Double v = Double.parseDouble(arrstr[i]);
				values[i] = v;
			} catch(NumberFormatException e) { 
				//throw new IllegalArgumentException(e.getMessage());
				values[i] = null;
			}
		}
	}
	
	public ProbeLine(ProbeLine a) {
		chrom = a.chrom;
		strand = a.strand;
		offset = a.offset;
		values = a.values.clone();
	}
	
	public int compareTo(ProbeLine p) { 
		if(!chrom.equals(p.chrom)) { return chrom.compareTo(p.chrom); }
		if(!strand.equals(p.strand)) { return strand.compareTo(p.strand); }
		if(offset < p.offset) { return -1; }
		if(offset > p.offset) { return 1; }
		return 0;
	}
	
	public ProbeLine logLine() { 
		ProbeLine p = new ProbeLine(this);
		p.log();
		//System.out.println(String.format("log(%s) = %s", this.toString(), p.toString()));
		return p;
	}
	
	public ProbeLine expLine() { 
		ProbeLine p = new ProbeLine(this);
		p.exp();
		//System.out.println(String.format("exp(%s) = %s", this.toString(), p.toString()));
		return p;
	}
	
	public void log() { 
		for(int i = 0; i < values.length; i++) { 
			if(isValidValue(i)) { 
				values[i] = Math.log(values[i]);
				if(!isValidValue(i)) { 
					values[i] = null;
				}
			}
		}
	}
	
	public void exp() { 
		for(int i = 0; i < values.length; i++) { 
			if(isValidValue(i)) { 
				values[i] = Math.exp(values[i]);
			}
		}
	}
	
	public boolean isValidValue(int i) { 
		return i >= 0 && i < values.length && values[i] != null && 
			!Double.isInfinite(values[i]) && !Double.isNaN(values[i]);
	}

	public boolean belowThreshold(double t) { 
		for(int i = 0; i < values.length; i++) { 
			if(values[i] != null && values[i] >= t) { 
				return false;
			}
		}
		return true;
	}
	
	public String toString() { 
		StringBuilder sb = 
			new StringBuilder(String.format("%s\t%s\t%d", chrom, strand, offset));
		for(int i = 0; i < values.length; i++) { 
			sb.append(String.format("\t%.2f", values[i]));
		}
		return sb.toString();
	}
	
	public int hashCode() { 
		int code = 17;
		code += chrom.hashCode(); code *= 37;
		code += offset; code *= 37;
		code += strand.hashCode(); code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof ProbeLine)) { return false; }
		ProbeLine l = (ProbeLine)o;
		return offset == l.offset && l.chrom.equals(chrom) && l.strand.equals(strand);
	}
}
