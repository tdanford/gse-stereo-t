/*
 * Author: tdanford
 * Date: Jan 7, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.models;

import java.io.*;
import java.util.regex.*;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import edu.mit.csail.cgs.sigma.Printable;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.utils.ArrayUtils;
import edu.mit.csail.cgs.utils.json.JSONObject;
import edu.mit.csail.cgs.utils.models.Model;

public class FileInputData extends Model implements InputData, Comparable<FileInputData> {
	
	public String chrom, strand;
	public Integer[] locations;
	public Double[][] values;
	public String[] flags;
	
	public FileInputData() {
		chrom = null;
		strand = null;
		locations = new Integer[0];
		values = new Double[0][];
	}
	
	public FileInputData(Integer channels) { 
		this();
		values = new Double[channels][];
	}
	
	public void addFlag(String flag) { 
		if(flags == null) { 
			flags = new String[] { flag }; 
		} else { 
			flags = ArrayUtils.append(flags, flag);
		}
	}
	
	public Set<String> flags() { 
		TreeSet<String> fs = new TreeSet<String>();
		for(int i = 0; flags != null && i < flags.length; i++) { 
			fs.add(flags[i]);
		}
		return fs;
	}
	
	/**
	 * This constructor transposes the vals input data.
	 * @param c
	 * @param str
	 * @param locs
	 * @param vals
	 */
	public FileInputData(String c, String str, Vector<Integer> locs, Vector<Double[]> vals) { 
		chrom = c;
		strand = str;
		locations = new Integer[locs.size()];
		int channels = vals.size() > 0 ? vals.get(0).length : 1;
		values = new Double[channels][locations.length];
		
		for(int i = 0; i < locs.size(); i++) { 
			locations[i] = locs.get(i);
			for(int k = 0; k < values.length; k++) { 
				values[k][i] = vals.get(i)[k];
			}
		}
	}
	
	public FileInputData(String c, String str, Integer[] locs, Double[][] vals) { 
		chrom = c;
		strand = str;
		locations = locs;
		values = vals;
	}
	
	public FileInputData(JSONObject obj) { 
		super(obj);
	}
	
	public FileInputData(InputData data) { 
		this(data.chrom(), data.strand(), data.locations(), data.values());
	}
	
	public int[] subsetLocations(int start, int end) { 
		int count = 0, firstInset = 0, lastInset = 0;
		for(int i = 0; i < locations.length; i++) { 
			if(locations[i] >= start && locations[i] <= end) { 
				count += 1;
			} else { 
				if(locations[i] < start) { 
					firstInset += 1;
				} else { 
					lastInset += 1;
				}
			}
		}
		
		Integer[] newLocations = new Integer[count];
		Double[][] newValues = new Double[values.length][count];
		
		for(int i = 0, j = 0; i < locations.length; i++) { 
			if(start <= locations[i] && end >= locations[i]) { 
				newLocations[j] = locations[i];
				for(int k = 0; k < values.length; k++) { 
					newValues[k][j] = values[k][i];
				}
				j += 1;
			}
		}
		
		int[] insets = new int[] { firstInset, lastInset };
		
		locations = newLocations;
		values = newValues;
		
		return insets;
	}
	
	public void append(FileInputData data) {
		if(chrom == null) { 
			chrom = data.chrom;
			strand = data.strand();
			locations = data.locations.clone();
			values = data.values.clone();
			return;
		}
		
		if(!chrom.equals(data.chrom) || !strand.equals(data.strand)) { 
			throw new IllegalArgumentException(String.format("%s:%s != %s:%s",
					chrom, strand, data.chrom, data.strand));
		}
		
		if(locations[locations.length-1] >= data.locations[0]) { 
			throw new IllegalArgumentException(String.format("%d >= %d", 
					locations[locations.length-1], data.locations[0]));
		}
		
		if(values.length != data.values.length) { 
			throw new IllegalArgumentException(String.format("channels %d != %d",
					values.length, data.values.length));
		}
		
		if(locations.length == 0 || data.locations.length == 0) { 
			throw new IllegalArgumentException(String.format("%d, %d" , locations.length, data.locations.length));
		}
		
		int newLength = locations.length + data.locations.length;
		Integer[] newLocations = new Integer[newLength];
		Double[][] newValues = new Double[values.length][newLength];
		
		int last = locations[locations.length-1];
		
		for(int i = 0; i < newLength; i++) {
			boolean inData = i >= locations.length;
			int idx = inData ? i-locations.length : i;
			newLocations[i] = inData ? data.locations[idx] : locations[idx];
			
			if(inData && newLocations[i] < last) { 
				throw new IllegalArgumentException(
					String.format("%d < %d", newLocations[i], last));
			}

			for(int k = 0; k < values.length; k++) { 
				newValues[k][i] = inData ? data.values[k][idx] : values[k][idx];
			}
		}
		
		locations = newLocations;
		values = newValues;
	}
	
	public void subsetChannels(Integer[] channels) { 
		Double[][] newValues = new Double[channels.length][locations.length];
		for(int i = 0; i < channels.length; i++) { 
			int c = channels[i];
			if(c < 0 || c >= values.length) { 
				String chs = "";
				for(int k = 0; k < channels.length; k++) { chs += ":" + channels[k]; }
				throw new IllegalArgumentException(String.format("Illegal channel: %d (%s)", c, chs));
			}
			newValues[i] = values[c];
		}
		values = newValues;
	}
	
	public void stack(InputData data) { 
		Integer[] locs = data.locations();
		if(locs.length != locations.length) { throw new IllegalArgumentException(); }
		for(int i = 0; i < locations.length; i++) { 
			if(!locations[i].equals(locs[i])) { 
				throw new IllegalArgumentException();
			}
		}
		
		if(!chrom.equals(data.chrom()) || !strand.equals(data.strand())) { 
			throw new IllegalArgumentException();
		}
		
		Double[][] vals = data.values();
		Double[][] newValues = new Double[values.length + vals.length][locations.length];
		for(int i = 0; i < values.length; i++) { 
			newValues[i] = values[i];
		}
		
		for(int i = 0; i < vals.length; i++) { 
			newValues[values.length+i] = vals[i];
		}
		
		values = newValues;
	}	
	
	public String chrom() { return chrom; }
	public String strand() { return strand; }
	
	public void printData(PrintStream ps) { 
		ps.println(String.format("%s\t%s", chrom, strand));
		
		int len = length();
		for(Integer off : locations) { 
			ps.print(String.format("%d ", off));
		}
		ps.println();
		
		for(Double[] val : values) { 
			for(int i = 0; i < len; i++) { 
				ps.print(String.format("%.2f ", val[i]));
			}
			ps.println();
		}		
	}
	
	public void print(PrintStream ps) {
		printData(ps);
		ps.println("----------------------");
	}

	public int channels() {
		return values.length;
	}

	public int length() {
		return locations.length;
	}

	public Integer[] locations() {
		return locations;
	}

	public Double[][] values() {
		return values;
	}
	
	public int hashCode() { 
		int code = 17;
		code += chrom.hashCode(); code *= 37;
		code += strand.hashCode(); code *= 37;
		return code;
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof FileInputData)) { 
			return false;
		}
		
		FileInputData d = (FileInputData)o;
		if(!chrom.equals(d.chrom)) { return false; }
		if(!strand.equals(d.strand)) { return false; }
		
		if(locations.length != d.locations.length) { return false; }
		for(int i = 0; i < locations.length; i++) { 
			if(locations[i] != d.locations[i]) { 
				return false; 
			}
		}
		
		return true;
	}
	
	public int compareTo(FileInputData d) { 
		int c = chrom.compareTo(d.chrom);
		if(c != 0) { return c; }
		c = strand.compareTo(d.strand); 
		if(c != 0) { return c; }
		for(int i = 0; i < Math.min(locations.length, d.locations.length); i++) { 
			if(locations[i] < d.locations[i]) { 
				return -1; 
			}
			if(locations[i] > d.locations[i]) { 
				return 1; 
			}
		}
		
		if(locations.length < d.locations.length) { return -1; }
		if(locations.length > d.locations.length) { return 1; }
		
		return 0;
	}
	
	public Integer start() { return locations[0]; }
	public Integer end() { return locations[locations.length-1]; }
	
	public RegionKey regionKey() { return new RegionKey(chrom, start(), end(), strand); }
}
