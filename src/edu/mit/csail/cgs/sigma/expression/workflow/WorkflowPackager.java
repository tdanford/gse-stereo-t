/*
 * Author: tdanford
 * Date: Dec 16, 2008
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.workflow.models.Chunk;
import edu.mit.csail.cgs.sigma.expression.workflow.models.FileInputData;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;

/** 
 * @author tdanford
 *
 */
public class WorkflowPackager implements Iterator<FileInputData> {
	
	private Integer[] channels;
	private Iterator<Chunk> chunks;
	private PackageLines lines;
	private Double missingReplacement;
	
	public WorkflowPackager(Integer[] ch, Iterator<Chunk> citr) { 
		chunks = citr;
		lines = findNextLines();
		missingReplacement = 1.01;
		channels = ch.clone();
	}
	
	public WorkflowPackager(Integer ch, Iterator<Chunk> citr) { 
		this(new Integer[] { ch }, citr);
	}
	
	public void remove() { throw new UnsupportedOperationException(); }
	
	private PackageLines findNextLines() { 
		PackageLines ls = null;
		if(chunks.hasNext()) { 
			ls = new PackageLines(chunks.next());
		}
		return ls;
	}
	
	public boolean hasNext() { 
		return lines != null;
	}
	
	public FileInputData next() { 
		FileInputData data = lines.asData();
		data.subsetChannels(channels);
		lines = findNextLines();
		return data;
	}
	
	private class PackageLines { 
		public String chrom, strand;
		public Vector<Integer> offsets;
		public Vector<Double[]> values;
		
		public PackageLines(Chunk c) {
			chrom = c.getChrom();
			strand = c.getStrand();
			offsets = new Vector<Integer>();
			values = new Vector<Double[]>();
			
			Iterator<ProbeLine> lines = c.lines();
			int cc = 0;
			while(lines.hasNext()) {
				ProbeLine pl = lines.next();
				addLine(pl);
				cc += 1;
			}
			//System.out.println("PL: " + cc);
		}
		
		public void addLine(ProbeLine line) { 
			int offset = line.offset;
			int len = line.values.length;
			
			if(chrom != null && !line.chrom.equals(chrom)) { 
				throw new IllegalArgumentException();
			}
			if(!values.isEmpty() && values.get(values.size()-1).length != len) { 
				throw new IllegalArgumentException(line.toString());
			}

			if(missingValue(line.values)) { 
				replaceValues(line.values); 
			}
			
			offsets.add(offset);
			values.add(line.values);
		}
		
		public void replaceValues(Double[] varray) { 
			int c = 0;
			Double replacement = 0.0;
			for(int i = 0; i < varray.length; i++) { 
				if(varray[i] != null) { 
					c += 1; 
					replacement += varray[i];
				}
			}
			
			if(c > 0 ) { 
				replacement /= (double)c;
			} else { 
				replacement = missingReplacement;
			}
			
			for(int i = 0; i < varray.length; i++) { 
				if(varray[i] == null) { 
					varray[i] = replacement;
				}
			}
		}
		
		public boolean missingValue(Double[] varray) { 
			for(int i = 0; i < varray.length; i++) { 
				if(varray[i] == null) { return true; }
			}
			return false;
		}

		public FileInputData asData() { 
			return new FileInputData(chrom, strand, offsets, values);
		}
	}
}
