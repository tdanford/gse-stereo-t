package edu.mit.csail.cgs.sigma.expression.models;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Mapper;

public class DivergentSite extends Region {
	
	private Transcript watson, crick;
	
	public DivergentSite(Region r, Transcript wat, Transcript crk) { 
		super(r);
		watson = wat;
		crick = crk;
	}
	
	public Transcript getWatsonTranscript() { return watson; }
	public Transcript getCrickTranscript() { return crick; }
	
	public static class EncodingMapper implements Mapper<DivergentSite,String> {
		public String execute(DivergentSite a) {
			return DivergentSite.encode(a);
		} 
	}
	
	public static class DecodingMapper implements Mapper<String,DivergentSite> {
		
		private Genome genome;
		
		public DecodingMapper(Genome g) { 
			genome = g;
		}

		public DivergentSite execute(String a) {
			return DivergentSite.decode(genome, a);
		}
	}
	
	public static Pattern sitePattern = Pattern.compile("([^:]+):(\\d+)-(\\d+)"); 
	
	public static String encode(DivergentSite s) { 
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s:%d-%d", s.getChrom(), s.getStart(), s.getEnd()));
		sb.append(String.format("/%s", Transcript.encode(s.watson)));
		sb.append(String.format("/%s", Transcript.encode(s.crick)));		
		return sb.toString();
	}
	
	public static DivergentSite decode(Genome g, String line) {
		String[] array = line.split("/");
		Matcher m = sitePattern.matcher(array[0]);
		if(array.length != 3 || !m.matches()) { throw new IllegalArgumentException(line); }
		String chrom = m.group(1);
		int start = Integer.parseInt(m.group(2));
		int end = Integer.parseInt(m.group(3));
		Region r = new Region(g, chrom, start, end);
		Transcript watson = Transcript.decode(g, array[1]);
		Transcript crick = Transcript.decode(g, array[2]);
		return new DivergentSite(r, watson, crick);
	}
}
