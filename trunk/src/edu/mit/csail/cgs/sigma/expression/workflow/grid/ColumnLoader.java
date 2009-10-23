/*
 * Author: tdanford
 * Date: May 20, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.grid;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.workflow.WholeGenome;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.grid.columns.*;

public class ColumnLoader {
	
	private WorkflowProperties props;

	public ColumnLoader(WorkflowProperties ps) { 
		props = ps;
	}
	
	public Collection<GridColumn> loadColumns() {
		ArrayList<GridColumn> cols = new ArrayList<GridColumn>();
		try {
			String key = "txns288c";
			String expt = "matalpha";
			WholeGenome genome = WholeGenome.loadWholeGenome(props, "txns288c");
			genome.loadIterators();
			
			//cols.add(new SenseGridColumn(genome, "matalpha"));
			cols.add(new SenseGridColumn(props, key, expt));
			cols.add(new AntisenseGridColumn(genome, "matalpha"));
			
			cols.add(new DiffSenseGridColumn(genome, "matalpha"));
			cols.add(new DiffAntisenseGridColumn(genome, "matalpha"));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return cols;
	}
}
