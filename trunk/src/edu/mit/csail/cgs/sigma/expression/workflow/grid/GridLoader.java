/*
 * Author: tdanford
 * Date: May 20, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.grid;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.GenomeExpander;
import edu.mit.csail.cgs.ewok.verbs.RefGeneGenerator;
import edu.mit.csail.cgs.sigma.SerialExpander;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;

public class GridLoader {
	
	private WorkflowProperties props;
	private String key, strain;
	
	private RowLoader rows;
	private ColumnLoader cols;
	
	private Set<String> totalItems;
	private Set<Gene> totalGenes;

	public GridLoader(WorkflowProperties ps, String k) throws IOException {
		key = k;
		props = ps;
		strain = props.parseStrainFromKey(key);
        System.out.println(String.format("GridLoader: %s in %s", key, strain));
		
		rows = new RowLoader(props);
		cols = new ColumnLoader(props);
		
		loadTotalItems();
	}
	
	public Grid loadGrid() {
		Collection<GridColumn> columns = cols.loadColumns();

		System.out.println(String.format(
				"Building Grid: %d columns, %d items", 
                columns.size(), totalGenes.size()));

		Grid g = new Grid(columns, totalGenes);
		ArrayList<GridRow> rs = rows.loadRows();
		
		for(GridRow r : rs) {
			System.out.println(String.format("\tAdding: %s", r.name));
			g.addRow(r);
		}
		
		return g;
	}
	
	private void loadTotalItems() { 
		totalItems = new TreeSet<String>();
		totalGenes = new TreeSet<Gene>();
		
		Genome genome = props.getSigmaProperties().getGenome(strain);
		Expander<Region,Gene> geneGen = props.getSigmaProperties().getGeneGenerator(strain);
		if(strain.equals("sigma")) { 
			geneGen = new SerialExpander<Region,Gene>(geneGen,
					new RefGeneGenerator(genome, "sgdGene"));
		}

		GenomeExpander<Gene> expander = new GenomeExpander<Gene>(geneGen);
		Iterator<Gene> genes = expander.execute(genome);
		
		while(genes.hasNext()) {
			Gene g = genes.next();
			String id = g.getID();
			if(!totalItems.contains(id)) { 
				totalItems.add(id);
				totalGenes.add(g);
			}
		}

		System.out.println(String.format("Loaded Total Genes: %d", totalGenes.size()));
	}
}
