/*
 * Author: tdanford
 * Date: Jan 20, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.models;

import java.util.regex.*;
import edu.mit.csail.cgs.utils.models.Model;

public class RegionKey extends Model implements Comparable<RegionKey> { 
	
	public String chrom, strand;
	public Integer start, end;
	
	public RegionKey() { 
	}
	
	public RegionKey(RegionKey k) { 
		chrom = k.chrom;
		start = k.start; 
		end = k.end; 
		strand = k.strand;
	}
	
	private static Pattern regionPattern = Pattern.compile("([^:]+):(\\d+)-(\\d+):([+-])");
	
	public RegionKey(String unparsed) { 
		Matcher m = regionPattern.matcher(unparsed);
		if(!m.matches()) { throw new IllegalArgumentException(unparsed); }
		chrom = m.group(1);
		start = Integer.parseInt(m.group(2));
		end = Integer.parseInt(m.group(3));
		strand = m.group(4);
	}
	
	public RegionKey(String c, int st, int ed, String str) { 
		chrom = c;
		start = st;
		end = ed;
		strand = str;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof RegionKey)) { return false; }
		RegionKey k = (RegionKey)o;
		return chrom.equals(k.chrom) && strand.equals(k.strand) && 
			start.equals(k.start) && end.equals(k.end);
	}
	
	public Integer fivePrime() { return strand.equals("+") ? start : end; }
	public Integer threePrime() { return strand.equals("+") ? end : start; }
	
	public String toString() { 
		return String.format("%s:%d-%d:%s", chrom, start, end, strand);
	}
	
	public int hashCode() { 
		int code = 17;
		code += chrom.hashCode(); code *= 37;
		code += strand.hashCode(); code *= 37;
		code += start; code *= 37;
		code += end; code *= 37;
		return code;
	}
	
	public boolean contains(int pos) { 
		return start <= pos && end >= pos;
	}
	
	public int compareTo(String chr, String str, int pos) { 
		if(!chrom.equals(chr)) { return chrom.compareTo(chr); }
		if(end < pos) { return -1; }
		if(start > pos) { return 1; }
		if(!strand.equals(str)) { return strand.compareTo(str); }
		return 0;
	}
	
	public boolean strandInvariantOverlaps(RegionKey k) { 
		if(!chrom.equals(k.chrom)) { 
			return false;
		}
		return (start <= k.start && end >= k.start) ||
			(k.start <= start && k.end >= start);		
	}
	
	public boolean overlaps(RegionKey k) { 
		if(!strand.equals(k.strand) || !chrom.equals(k.chrom)) { 
			return false;
		}
		return (start <= k.start && end >= k.start) ||
			(k.start <= start && k.end >= start);
	}
	
	public int compareTo(RegionKey k) { 
		int c1 = chrom.compareTo(k.chrom);
		if(c1 != 0) { return c1; }
		if(start < k.start) { return -1; }
		if(start > k.start) { return 1; }
		if(end < k.end) { return -1; }
		if(end > k.end) { return 1; }
		c1 = strand.compareTo(k.strand); 
		if(c1 != 0) { return c1; }
		return 0;
	}

	public int width() {
		return end - start + 1;
	}
}
