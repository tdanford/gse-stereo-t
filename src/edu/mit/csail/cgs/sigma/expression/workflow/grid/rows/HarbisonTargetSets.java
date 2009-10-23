/*
 * Author: tdanford
 * Date: May 21, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.grid.rows;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.litdata.*;
import edu.mit.csail.cgs.sigma.litdata.harbison.*;

public class HarbisonTargetSets {
	
	public static void main(String[] args) {
		WorkflowProperties ps = new WorkflowProperties();
		try {
			HarbisonTargetSets sets = new HarbisonTargetSets(ps);
			sets.clearSets();
			sets.createSets();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private WorkflowProperties props;
	private HarbisonData data;
	private File dir;
	
	public HarbisonTargetSets(WorkflowProperties ps) throws IOException { 
		props = ps;
		data = new HarbisonData(new LitDataProperties());
		dir = null;
	}
	
	public void clearSets() throws IOException { 
		File gridDir = new File(props.getDirectory(), "grid");
		if(gridDir.exists()) { 
			dir = new File(gridDir, "harbison");
			if(dir.exists()) {
				File[] lst = dir.listFiles(new RowFilenameFilter());
				for(int i = 0; i < lst.length; i++) { 
					if(!lst[i].delete()) { 
						System.err.println(String.format("Couldn't delete: %s", lst[i].getAbsolutePath()));
					} else { 
						System.out.println(String.format("Deleted: %s", lst[i].getAbsolutePath()));
					}
				}
			}
		}
	}
	
	private static class RowFilenameFilter implements FilenameFilter {
		public boolean accept(File dir, String fname) {
			return fname.endsWith(".rows.txt");
		} 
	}
	
	public void createSets() throws IOException { 
		File gridDir = new File(props.getDirectory(), "grid");
		if(!gridDir.exists() && !gridDir.mkdir()) { 
			throw new IOException(String.format("Can't create: %s", gridDir.getAbsolutePath()));
		}
		dir = new File(gridDir, "harbison");
		if(!dir.exists() && !dir.mkdir()) { 
			throw new IOException(String.format("Can't create: %s", dir.getAbsolutePath()));			
		}
		
		double pvalue_threshold = 0.001;
		String pvalue_key = "001";
		
		for(String expt : data.getExpts()) { 
			Set<String> orfs = data.boundORFs(expt, pvalue_threshold);
			if(orfs.size() > 0) { 
				String filename = String.format("%s_%s.rows.txt", expt, pvalue_key);
				File file = new File(dir, filename);
				PrintStream ps = new PrintStream(new FileOutputStream(file));
				for(String orf : orfs) { 
					ps.println(orf);
				}
				ps.close();
				System.out.println(String.format("Saved: %s (%d ORFs)", file.getName(), orfs.size()));
			} else { 
				System.out.println(String.format("Ignoring: %s", expt));
			}
		}
	}
}
