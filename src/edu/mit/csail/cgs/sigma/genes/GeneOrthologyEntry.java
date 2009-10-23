package edu.mit.csail.cgs.sigma.genes;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.Parser;


public class GeneOrthologyEntry {
	
	public static void main(String[] args) { 
		File f = new File(args[0]);
		try {
			Parser<GeneOrthologyEntry> entries = 
				new Parser<GeneOrthologyEntry>(f, new GeneOrthologyEntry.ParsingMapper());
			while(entries.hasNext()) { 
				GeneOrthologyEntry entry = entries.next();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static enum Flag { PERFECT, SIZESHIFT, MISSING, MERGED, CONTAINSN, UNKNOWN };
	public static Pattern coordsPattern = Pattern.compile("([^:]+):(\\d+)-(\\d+):([-+])");

	private String geneName;
	private Coords s288cCoords, sigmaCoords;
	private Double identity;
	private Set<Flag> flags;
	private Coords orfCoords;
	
	public GeneOrthologyEntry(String line) { 
		String[] a = line.split("\\s");
		geneName = a[0];
		s288cCoords = parseCoordsString(a[1]);
		sigmaCoords = parseCoordsString(a[2]);
        try { 
            identity = Double.parseDouble(a[3]);
        } catch(NumberFormatException e) { 
            identity = null;
        }
		flags = new HashSet<Flag>();
        orfCoords = null;

		if(a.length > 4) { 
		    String[] fa = a[4].split(",");
		    for(int i = 0; i < fa.length; i++) { 
                try { 
                    if(fa[i].length() > 0) { 
                        flags.add(Flag.valueOf(fa[i]));
                    } else { 
                        flags.add(Flag.UNKNOWN);
                    }
                } catch(IllegalArgumentException e) { 
                    System.err.println(String.format("Unknown FLAG: \"%s\"", fa[i]));
                    flags.add(Flag.UNKNOWN);
                }
		    }

		    if(a.length > 5) { 
		        orfCoords = parseCoordsString(a[5]);
		    }
		}
	}
	
	public String getGeneName() { return geneName; }
	public Coords getS288CCoords() { return s288cCoords; }
	public Coords getSigmaCoords() { return sigmaCoords; }
	public Double getPctIdentity() { return identity; }
	public Collection<Flag> getFlags() { return flags; }
	public boolean hasFlag(Flag f) { return flags.contains(f); }
	public Coords getORFCoords() { return orfCoords; }
	
	public int hashCode() { 
		int code = 17;
		code += geneName.hashCode(); code *= 37;
		code += s288cCoords.hashCode(); code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof GeneOrthologyEntry)) { return false; }
		GeneOrthologyEntry e = (GeneOrthologyEntry)o;
		if(!geneName.equals(e.geneName)) { return false; }
		if(!s288cCoords.equals(e.s288cCoords)) { return false; }
        
        if(sigmaCoords != null && e.sigmaCoords != null) { 
            if(!sigmaCoords.equals(e.sigmaCoords)) { return false; }
        } else { 
            if(sigmaCoords != null || e.sigmaCoords != null) { return false; }
        }
        
		return true;
	}
	
	public static class ParsingMapper implements Mapper<String,GeneOrthologyEntry> { 
		public ParsingMapper() { 
			
		}
		
		public GeneOrthologyEntry execute(String a) { 
			return new GeneOrthologyEntry(a);
		}
	}
    
    public static Coords parseCoordsString(String str) {
        str = str.trim();
        if(str.equals("NA") || str.length() == 0) { 
            return null;
        } else { 
            return new Coords(str);
        }
    }

	public static class Coords { 
		private String chrom;
		private int start, end;
		private char strand; 
		
		public Coords(String entry) { 
			Matcher m = coordsPattern.matcher(entry);
			if(!m.matches()) { throw new IllegalArgumentException(entry); }
			chrom = m.group(1);
			start = Integer.parseInt(m.group(2));
			end = Integer.parseInt(m.group(3));
			strand = m.group(4).charAt(0);
		}
		
		public String toString() { return String.format("%s:%d-%d:%c", chrom, start, end, strand); }
		
		public String getChrom() { return chrom; } 
		public int getStart() { return start; }
		public int getEnd() { return end; }
		public char getStrand() { return strand; }
		
		public StrandedRegion createRegion(Genome g) { 
			return new StrandedRegion(g, chrom, start, end, strand);
		}
		
		public int hashCode() { 
			int code = 17;
			code += chrom.hashCode(); code *= 37;
			code += start; code *= 37;
			return code;
		}
		
		public boolean equals(Object o) { 
			if(!(o instanceof Coords)) { return false; }
			Coords c = (Coords)o;
			if(!chrom.equals(c.chrom)) { return false; }
			if(strand != c.strand) { return false; }
			if(start != c.start || end != c.end) { return false; }
			return true;
		}
	}
}

