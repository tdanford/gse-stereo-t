package edu.mit.csail.cgs.sigma.expression.segmentation.sharing;

import java.util.*;

public class ParameterSharing {
	
	public static void main(String[] args) { 
		String spec = "1 3 1 3 5 2";
		ParameterSharing ps = new ParameterSharing(spec);
		System.out.println(ps.toString());
	}

	private Double weight;
	private Integer[] patterns;
	private Integer[][] channels;
	
	public ParameterSharing(int chan, boolean shared) {
		this(0.0, chan, shared);
	}
	
	public ParameterSharing(double w, int chan, boolean shared) {
		weight = w;
		if(shared) { 
			patterns = new Integer[chan];
			channels = new Integer[1][chan];
			for(int i = 0; i < chan; i++) { 
				patterns[i] = 0;
				channels[0][i] = i;
			}
		} else { 
			patterns = new Integer[chan];
			channels = new Integer[chan][1];
			for(int i = 0; i < chan; i++) { 
				patterns[i] = i;
				channels[i][0] = i;
			}
		}
	}
	
	public ParameterSharing(String line) { 
		this(0.0, splitIntegers(line));
	}
	
	public ParameterSharing(double w, String line) { 
		this(w, splitIntegers(line));
	}
	
	public ParameterSharing(double w, Integer[] patts) {
		weight = w;
		patterns = patts.clone();
		Map<Integer,Integer> pmap = new HashMap<Integer,Integer>();
		int p = 0;
		for(int i = 0; i < patterns.length; i++) { 
			if(!pmap.containsKey(patterns[i])) { 
				pmap.put(patterns[i], p);
				patterns[i] = p;
				p++;
			} else { 
				patterns[i] = pmap.get(patterns[i]);
			}
		}
		
		int numPatts = pmap.size();
		channels = new Integer[numPatts][];
		for(p = 0; p < numPatts; p++) { 
			int numChan = 0;
			for(int i = 0; i < patterns.length; i++) { 
				if(patterns[i] == p) { 
					numChan += 1;
				}
			}
			
			channels[p] = new Integer[numChan];
			int j = 0;
			for(int i = 0; i < patterns.length; i++) { 
				if(patterns[i] == p) { 
					channels[p][j++] = i;
				}
			}
		}
	}
	
	public Integer[] getChannels(int pattern) { return channels[pattern]; }
	public int getNumPatterns() { return channels.length; }
	public int getNumChannels(int pattern) { return channels[pattern].length; }
	public int getNumTotalChannels() { return patterns.length; }
	public Double getWeight() { return weight; }
	
	public String toString() { 
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for(int i = 0; i < patterns.length; i++) { 
			if(i > 0) { sb.append(" "); }
			sb.append(String.format("#%d", patterns[i])); 
		}
		sb.append("]");
		for(int i = 0; i < channels.length; i++) {
			sb.append(String.format(" #%d:(", i));
			for(int j = 0; j < channels[i].length; j++) { 
				if(j > 0) { sb.append(" "); }
				sb.append(String.valueOf(channels[i][j]));
			}
			sb.append(")");
		}
		return sb.toString();
	}
	
	public int hashCode() { 
		int code = 17;
		for(int i = 0; i < patterns.length; i++) { 
			code += patterns[i]; code *= 37;
		}
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof ParameterSharing)) { return false; }
		ParameterSharing ps = (ParameterSharing)o;
		
		if(patterns.length != ps.patterns.length) { return false; }
		for(int i = 0; i < patterns.length; i++) { 
			if(patterns[i] != ps.patterns[i]) { 
				return false;
			}
		}
		
		return true;
	}

	private static Integer[] splitIntegers(String line) { 
		String[] array = line.split("\\s+");
		Integer[] values = new Integer[array.length];
		for(int i = 0; i < array.length; i++) { 
			values[i] = Integer.parseInt(array[i]);
		}
		return values;
	}
}
