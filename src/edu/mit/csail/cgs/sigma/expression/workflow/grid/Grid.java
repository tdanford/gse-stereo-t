/*
 * Author: tdanford
 * Date: May 20, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.grid;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.utils.ArrayUtils;
import edu.mit.csail.cgs.utils.Enrichment;

public class Grid {

	private GridColumn[] cols;
	private Map<String,StrandedRegion> totalItems;
	private Map<Integer,Set<String>> colSubsets;
	private ArrayList<GridRow> rows;
	
	public Grid(Collection<Gene> genes) { 
		cols = new GridColumn[0];
		totalItems = new TreeMap<String,StrandedRegion>();
		colSubsets = new TreeMap<Integer,Set<String>>();
		rows = new ArrayList<GridRow>();
		
		for(Gene g : genes) { 
			totalItems.put(g.getID(), g);
		}
	}
	
	public Grid(Collection<GridColumn> cs, Collection<Gene> genes) { 
		this(genes);
		cols = cs.toArray(new GridColumn[0]);
		buildTable();
	}
	
	public int numItems() { return totalItems.size(); }
	public int numRows() { return rows.size(); }
	public int numCols() { return cols.length; }
	public String colName(int i) { return cols[i].name(); }
	public String rowName(int i) { return rows.get(i).name; }
	public GridRow getRow(int i) { return rows.get(i); }
	
	public Boolean[][] enrichedGrid(double pvalue) { 
		Boolean[][] array = new Boolean[rows.size()][cols.length];
		for(int i = 0; i < rows.size(); i++) { 
            System.out.print(String.format("Row %d: %s -> ", i, rows.get(i).name));
			for(int j = 0; j < cols.length; j++) {
				array[i][j] = false;
				if(rows.get(i).isEnriched(cols[j].name(), numItems(), pvalue)) { 
					array[i][j] = true;
				}
			}
            System.out.println();
		}
		return array;
	}

	public Set<String> enrichedCols(int rowIdx, double pvalue) { 
		Set<Enrichment> enr = rows.get(rowIdx).enrichedSubsets(numItems(), pvalue);
		Set<String> enrichedTags = new TreeSet<String>();
		for(Enrichment e : enr) {
			enrichedTags.add(e.getCategory());
		}
		return enrichedTags;
	}
	
	public Set<String> enrichedRows(int colIdx, double pvalue) { 
		String tag = cols[colIdx].name();
		Set<String> enriched = new TreeSet<String>();
		for(int i = 0; i < rows.size(); i++) { 
			if(rows.get(i).isEnriched(tag, numItems(), pvalue)) { 
				enriched.add(rows.get(i).name);
			}
		}
		return enriched;
	}

	public void addColumn(GridColumn c) { 
		cols = ArrayUtils.append(cols, c);
		buildColumn(cols.length-1);
		Set<String> subset = colSubsets.get(cols.length-1);
		for(GridRow r : rows) { 
			r.addSubset(c.name(), subset);
		}
	}
	
	public void buildTable() { 
		for(int i = 0; i < cols.length; i++) { 
			buildColumn(i);
		}
	}
	
	private void buildColumn(int i) {
		TreeSet<String> subset = new TreeSet<String>();
		for(String id : totalItems.keySet()) {
			StrandedRegion reg = totalItems.get(id);
			if(cols[i].containsRegion(reg)) { 
				subset.add(id);
			}
		}
		colSubsets.put(i, subset);
	}
	
	public void addRow(GridRow r) { 
        r.subsetItems(totalItems.keySet());
		for(int i = 0; i < cols.length; i++) { 
			Set<String> subset = colSubsets.get(i);
			r.addSubset(cols[i].name(), subset);
		}
		rows.add(r);
	}
}

class ColumnNameComparator implements Comparator<GridColumn> { 
	public int compare(GridColumn c1, GridColumn c2) { 
		return c1.name().compareTo(c2.name());
	}
}