/*
 * Author: tdanford
 * Date: Jan 20, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.io.File;
import java.io.IOException;
import java.util.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.RandomInputGenerator;
import edu.mit.csail.cgs.sigma.expression.transcription.*;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.TAFit;
import edu.mit.csail.cgs.sigma.expression.workflow.models.FileInputData;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.utils.Closeable;

/**
 * Loads Workflow data, for display in the visualizer code.
 * 
 * @author tdanford
 *
 */
public class WorkflowInputGenerator implements RandomInputGenerator {
	
	private WorkflowProperties props;
	private String rootKey;
	private InputSegmentation[] data;
	
	private FileInputData current;
	private int first, last;
	private RegionKey key;
	private LinkedList<Segment> segments;
	
	private ArrayList<TranscriptCallModel> calls;
	private ArrayList<Call> currentCalls;
	
	public WorkflowInputGenerator(WorkflowProperties props, String key) throws IOException { 
		this(new File(props.getDirectory(), String.format("%s.packages", key)),
			new File(props.getDirectory(), String.format("%s.segments", key)));
		rootKey = key;
		this.props = props;
		currentCalls = new ArrayList<Call>();
	}
	
	private WorkflowInputGenerator(File packages, File segs) throws IOException { 
		segments = new LinkedList<Segment>();
		calls = new ArrayList<TranscriptCallModel>();
		boolean loadCalls = false;
		if(segs.exists()) { 
			System.out.println("**** Loading Segments File: " + segs.getName());
			load(new WorkflowSegmentationReader(segs));
		} else if (packages.exists()) { 
			System.out.println("**** Loading Packages File: " + packages.getName());
			load(0, new WorkflowPackageReader(packages));
		} else { 
			throw new IllegalArgumentException(packages.getAbsolutePath());
		}
	}
	
	private void load(int channel, Iterator<FileInputData> ds) { 
		ArrayList<InputSegmentation> dataList = new ArrayList<InputSegmentation>();
		int c = 0;
		while(ds.hasNext()) { 
			FileInputData d = ds.next();
			InputSegmentation is = new InputSegmentation(d, false);
			dataList.add(is);
			c += 1;
		}
		System.out.println(String.format("Loaded %d FileInputData packages...", c));
		
		if(ds instanceof Closeable) { 
			((Closeable)ds).close();
		}
		
		data = dataList.toArray(new InputSegmentation[0]);
		Arrays.sort(data);

		for(int i = 0; i < data.length; i++) { 
			System.out.print(String.format("%s:%d-%d ", data[i].input.chrom, data[i].input.start(), data[i].input.end()));
		}
		System.out.println();
	}
	
	private void load(Iterator<InputSegmentation> ds) { 
		ArrayList<InputSegmentation> dataList = new ArrayList<InputSegmentation>();
		int c = 0;
		while(ds.hasNext()) { 
			InputSegmentation is = ds.next();
			dataList.add(is);
			c += 1;
		}
		System.out.println(String.format("Loaded %d InputSegmentation packages...", c));
		
		if(ds instanceof Closeable) { 
			((Closeable)ds).close();
		}

		data = dataList.toArray(new InputSegmentation[0]);
		Arrays.sort(data);
		
		for(int i = 0; i < data.length; i++) { 
			System.out.print(String.format("%s:%d-%d ", data[i].input.chrom, data[i].input.start(), data[i].input.end()));
		}
		System.out.println();
	}
	
	private static class TranscriptCallModel extends Call { 
		
		public String chrom, strand;
		
		public TranscriptCallModel(String c, String s, Integer st, Integer ed, Double val) { 
			super(st, ed, val);
			chrom = c; strand = s;
		}
		
		public int hashCode() { 
			int code = super.hashCode(); 
			code += chrom.hashCode();  code *= 37;
			code += strand.hashCode(); code *= 37;
			return code;
		}
		
		public boolean equals(Object o) { 
			if(!(o instanceof TranscriptCallModel)) { return false; }
			TranscriptCallModel m = (TranscriptCallModel)o;
			if(!chrom.equals(m.chrom) || !strand.equals(m.strand)) { return false; }
			return super.equals(m);
		}
		
		public int compareTo(Call c) { 
			if(!(c instanceof TranscriptCallModel)) { return super.compareTo(c); }
			TranscriptCallModel m = (TranscriptCallModel)c;
			if(!strand.equals(m.strand)) { return strand.compareTo(m.strand); }
			if(!chrom.equals(m.chrom)) { return chrom.compareTo(m.chrom); }
			return super.compareTo(m);
		}
		
		public boolean containedIn(RegionKey key) { 
			return chrom.equals(key.chrom) && strand.equals(key.strand) && 
				key.start <= start && key.end >= end;
		}
	}
	
	
	public void generate() {
		generate(key.chrom, key.start, key.end, key.strand);
	}
	
	public void generate(String chr, int start, int end, String str) { 
		RegionKey newKey = new RegionKey(chr, start, end, str);
		if(key != null && key.equals(newKey)) { return; }

		key = newKey;
		segments.clear();
		int i = 0;

		for(i = 0; i < data.length && 
			data[i].input.regionKey().compareTo(chr, str, start) < 0; i++) {
			// do nothing.
		}
		first = i;
		
		for(i = first; i < data.length && data[i].input.chrom().equals(chr) && 
			data[i].input.regionKey().compareTo(chr, str, end) < 1; i++) {
			// do nothing.
		}
		last = i;

		FileInputData df = new FileInputData(1);
		int offset = 0;

		for(i = first; i < last; offset += data[i].input.length(), i++) { 
			df.append(data[i].input);
			
			for(int j = 0; j < data[i].segments.length; j++) { 
				int sstart = data[i].segments[j].start, send = data[i].segments[j].end;
				int segStart = data[i].input.locations[sstart];
				int segEnd = data[i].input.locations[Math.min(data[i].input.locations.length-1, send)];
				
				if(segStart >= start && segEnd < end) { 
					Segment ss = new Segment(data[i].segments[j], offset);
					segments.add(ss);
				}
			}
		}
		System.out.println(String.format("%s:%d-%d:%s covers %d packages.", chr, start, end, str, (last-first)));
		
		current = df;
		int[] insets = current.subsetLocations(start, end);
		
		for(Segment s : segments) {
			s.start -= insets[0];
			s.end -= insets[0];
		}
		
		currentCalls.clear();
		for(TranscriptCallModel m : calls) { 
			if(m.containedIn(key)) { 
				currentCalls.add(new Call(m.start, m.end, m.value));
			}
		}
		
		System.out.println(String.format("Filtered to: %d transcript calls", currentCalls.size()));
	}

	public InputData inputData() {
		return current;
	}

	public Collection<Segment> segments() {
		return segments;
	}

	public Collection<Segment> segments(int channel) {
		LinkedList<Segment> segs = new LinkedList<Segment>();
		for(Segment s : segments) { 
			if(s.channel.equals(channel)) { 
				segs.add(s);
			}
		}
		return segs;
	}
	
	public Collection<Call> calls() { 
		return currentCalls;
	}
	
	public String getKey() { return rootKey; }
	public RegionKey getRegion() { return key; }
}
