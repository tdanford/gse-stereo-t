/*
 * Author: tdanford
 * Date: Aug 27, 2008
 */
package edu.mit.csail.cgs.sigma.expression.models;

import java.util.*;

import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.utils.models.Model;

public class DiffGeneModel extends Model {

	public String geneID;
	public Double score;
	public Double variance;
	public Double confidence;
	public String genome;
	public String chrom;
	public Integer start, end;
	public String strand;
	
	public DiffGeneModel() { 
	}
	
	public DiffGeneModel(Gene g, double s, double v, double c) { 
		geneID = g.getID();
		score = s;
		variance = v;
		confidence = c;
		genome = g.getGenome().getVersion();
		chrom = g.getChrom();
		start = g.getStart();
		end = g.getEnd();
		strand = String.valueOf(g.getStrand());
	}
	
	public DiffGeneModel(Gene g, String name, double s, double v, double c) { 
		geneID = name;
		score = s;
		variance = v;
		confidence = c;
		genome = g.getGenome().getVersion();
		chrom = g.getChrom();
		start = g.getStart();
		end = g.getEnd();
		strand = String.valueOf(g.getStrand());
	}
	
	public int hashCode() { 
		return 37 * (37 * (17 + geneID.hashCode()) + genome.hashCode());  
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof DiffGeneModel)) { return false; }
		DiffGeneModel m = (DiffGeneModel)o;
		if(!geneID.equals(m.geneID)) { return false; }
		if(!genome.equals(m.genome)) { return false; }
		return true;
	}
}
