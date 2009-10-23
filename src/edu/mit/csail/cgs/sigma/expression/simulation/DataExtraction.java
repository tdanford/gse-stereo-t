/*
 * Author: tdanford
 * Date: Jul 21, 2009
 */
package edu.mit.csail.cgs.sigma.expression.simulation;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowDataLoader;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;

public class DataExtraction {
	
	public static void main(String[] args) {
		String regionString = args.length > 0 ? args[0] : "5:320000-326000";
		RegionKey wregion = new RegionKey(String.format("%s:+", regionString));
		RegionKey cregion = new RegionKey(String.format("%s:-", regionString));
		
		String key = args.length > 1 ? args[1] : "txns288c";
		
		System.out.println(String.format("Extracting : %s", regionString));

		try {

			WorkflowProperties props = new WorkflowProperties();

			DataExtraction wsim = new DataExtraction(props, key, wregion);
			DataExtraction csim = new DataExtraction(props, key, cregion);

			File dir = props.getDirectory();

			deleteFiles(dir, "test_");
			deleteFiles(dir, "test-");

			File woutput = new File(dir, 
					String.format("test_plus.data"));
			File coutput = new File(dir,
					String.format("test_negative.data"));

			wsim.save(woutput);
			System.out.println(String.format("Saved: %s", 
					woutput.getAbsolutePath()));

			csim.save(coutput);
			System.out.println(String.format("Saved: %s", 
					coutput.getAbsolutePath()));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void deleteFiles(File dir, String prefix) { 
		File[] lst = dir.listFiles(new DelFilenameFilter(prefix));
		for(int i = 0; i < lst.length; i++) { 
			lst[i].delete();
		}
	}
	
	public static class DelFilenameFilter implements FilenameFilter {
		
		public String prefix;
		
		public DelFilenameFilter(String p) { 
			prefix = p;
		}

		public boolean accept(File dir, String name) {
			return name.startsWith(prefix);
		} 
		
	}

	private WorkflowProperties props;
	private String key; 
	private RegionKey region;
	private ArrayList<ProbeLine> lines;
	
	public DataExtraction(WorkflowProperties p, String k, RegionKey r) throws IOException {
		props = p;
		key = k;
		region = r;
		lines = new ArrayList<ProbeLine>();
		loadLines();
	}
	
	private void loadLines() throws IOException {
		lines.clear();
		String strandKey = region.strand.equals("+") ? "plus" : "negative";
		String filename = String.format("%s_%s", key, strandKey);
		WorkflowDataLoader loader = new WorkflowDataLoader(props, filename);
		
		while(loader.hasNext()) { 
			ProbeLine line = loader.next();
			if(line.chrom.equals(region.chrom)) { 
				if(region.start <= line.offset && region.end >= line.offset) { 
					lines.add(line);
				}
			}
		}
		
		loader.close();
		
		System.out.println(String.format("Loaded %d probes.", lines.size()));
	}
	
	public void save(File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		save(ps);
		ps.close();
	}
	
	public Iterator<ProbeLine> probes() {
		return lines.iterator();
	}
	
	public void save(PrintStream ps) {
		Iterator<ProbeLine> prbs = probes();
		while(prbs.hasNext()) { 
			ProbeLine pb = prbs.next();
			ps.println(pb.toString());
		}
	}
	
	
}
