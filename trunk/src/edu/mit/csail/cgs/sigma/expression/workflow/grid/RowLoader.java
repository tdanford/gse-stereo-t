/*
 * Author: tdanford
 * Date: May 20, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.grid;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.GenomeExpander;
import edu.mit.csail.cgs.ewok.verbs.RefGeneGenerator;
import edu.mit.csail.cgs.sigma.SerialExpander;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.utils.SetTools;

public class RowLoader {

	private static SetTools<String> tools = new SetTools<String>();

	private WorkflowProperties props;

	public RowLoader(WorkflowProperties ps) throws IOException {
		props = ps;
	}
	
	public GridRow findRow(String name) { 
		File dir = props.getDirectory();
		String filename = String.format("%s.rows.txt", name);
		File f = new File(dir, filename);
		if(f.exists()) { 
			try {
				return loadFileRow(f);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else { 
			return null;
		}
	}
	
	public ArrayList<GridRow> loadRows() { 
		File dir = new File(props.getDirectory(), "grid");
		ArrayList<GridRow> rs = new ArrayList<GridRow>();
		try { 
			if(dir.exists()) { 
				rs.addAll(loadRows(dir));
			}
		} catch(IOException e) { 
			e.printStackTrace(System.err);
		}
		return rs;
	}
	
	public ArrayList<GridRow> loadRows(File dir) throws IOException { 
		ArrayList<GridRow> rs = new ArrayList<GridRow>();
		System.out.println(String.format("Searching %s...", dir.getName()));
		
		File[] lst = dir.listFiles();
		for(int i = 0; i < lst.length; i++) { 
			if(lst[i].isDirectory()) { 
				ArrayList<GridRow> subrows = loadRows(lst[i]);
				String prefix = lst[i].getName();
				for(GridRow r : subrows) { 
					r.name = String.format("%s/%s", prefix, r.name);
				}
				rs.addAll(subrows);
			} else if(lst[i].getName().endsWith(".rows.txt")) { 
				GridRow row = loadFileRow(lst[i]);
				System.out.println(String.format(
						"Loaded %s -> %d entries", 
						row.name, row.items.length));
				rs.add(row);
			}
		}
		
		return rs;
	}
	
	private static Pattern rowFileNamePattern = Pattern.compile(
			"^(.*)\\.rows\\.txt$");

	public GridRow loadFileRow(File f) throws IOException { 
		String line;
		Matcher m = rowFileNamePattern.matcher(f.getName());
		if(!m.matches()) { throw new IllegalArgumentException(f.getName()); }
		String name = m.group(1);
		
		BufferedReader br = new BufferedReader(new FileReader(f));
		Set<String> items = new TreeSet<String>();
		while((line = br.readLine()) != null) { 
			line = line.trim();
			if(line.length() > 0) { 
				items.add(line);
			}
		}
		br.close();
		return new GridRow(name, items.iterator());
	}
}
