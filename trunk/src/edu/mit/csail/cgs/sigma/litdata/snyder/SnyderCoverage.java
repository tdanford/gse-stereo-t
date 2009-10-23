/*
 * Author: tdanford
 * Date: Aug 30, 2009
 */
package edu.mit.csail.cgs.sigma.litdata.snyder;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;

public class SnyderCoverage {
	
	public static void main(String[] args) { 
		File finput = new File(args[0]);
		try {
			SnyderCoverage cover = new SnyderCoverage(finput);
			System.out.println("Loaded Coverage.");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Map<String,int[]> bins;
	private int binSize, extension;
	
	public SnyderCoverage(Genome g, int size, int ext) { 
		bins = new TreeMap<String,int[]>();
		binSize = size;
		extension = ext;
		for(String chrom : g.getChromList()) { 
			int chromLength = g.getChromLength(chrom);
			int chromBins = chromLength / binSize + 1;
			bins.put(chrom, new int[chromBins]);
		}
	}
	
	public SnyderCoverage(File f) throws IOException { 
		DataInputStream dis = new DataInputStream(new FileInputStream(f));
		binSize = dis.readInt();
		extension = dis.readInt();
		int s = dis.readInt();
		
		bins = new TreeMap<String,int[]>();
		
		for(int i = 0; i < s; i++) { 
			String c = dis.readUTF();
			int cs = dis.readInt();
			int[] b = new int[cs];
			for(int k = 0; k < b.length; k++) { 
				b[k] = dis.readInt();
			}
			bins.put(c, b);
		}
		
		dis.close();
	}
	
	public void save(File f) throws IOException { 
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
		
		dos.writeInt(binSize);
		dos.writeInt(extension);
		dos.writeInt(bins.size());
		
		for(String c : bins.keySet()) { 
			dos.writeUTF(c);
			int[] b = bins.get(c);
			dos.writeInt(b.length);
			for(int i = 0; i < b.length; i++) { 
				dos.writeInt(b[i]);
			}
		}

		dos.close();
	}
	
	public int bin(int offset) { 
		int b = offset / binSize;
		return b;
	}
	
	public int binSize() { return binSize; }
	
	public int[] coverage(RegionKey k) { 
		int[] b = new int[k.end-k.start+1];
		int[] kb = bins.get(k.chrom);
		for(int i = 0; i < b.length; i++) { 
			int bin = bin(k.start+i);
			if(bin < 0) { bin = 0; } 
			if(bin >= kb.length) { bin = kb.length-1; }
			b[i] = kb[bin];
		}
		return b;
	}
	
	public void addCover(RegionKey k) { 
		if(bins.containsKey(k.chrom)) {
			int start = k.start;
			int end = k.end;
			if(k.strand.equals("+")) { 
				start -= extension;
			} else { 
				end += extension;
			}
			
			int[] b = bins.get(k.chrom);
			int s1 = Math.max(0, bin(start));
			int s2 = Math.min(bin(end), b.length-1);
			
			for(int i = s1; i <= s2 && i < b.length; i++) { 
				b[i] += 1;
			}
		}
	}
	
}
