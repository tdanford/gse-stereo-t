/*
 * Author: tdanford
 * Date: Aug 28, 2008
 */
package edu.mit.csail.cgs.sigma.expression.models;

import java.util.*;

import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.utils.models.Model;

public class ExprProbeModel extends Model {

	public Double intensity;
	public Double background;
	public String chrom;
	public Integer location;
	public String strand;
	
	public String gene;
	public String geneStrand;
	public Integer geneOffset;
	
	public ExprProbeModel() { 
	}
	
	public ExprProbeModel(ExpressionProbe p) { 
		intensity = p.meanlog();
		chrom = p.getChrom();
		location = p.getLocation();
		strand = String.valueOf(p.getStrand());
		background = 1.0;
		
		gene = "NA";
		geneStrand = "?";
		geneOffset = 0;
	}
	
	public ExprProbeModel(ExpressionTwoChannelProbe p) { 
		intensity = p.meanlog(true);
		chrom = p.getChrom();
		location = p.getLocation();
		strand = String.valueOf(p.getStrand());
		background = p.meanlog(false);
		
		gene = "NA";
		geneStrand = "?";
		geneOffset = 0;
	}
	
	public void setGene(Gene g) { 
		gene = g.getID();
		geneStrand = String.valueOf(g.getStrand());
		
		if(strand.charAt(0) == '+') { 
			geneOffset = g.getEnd()-location;
		} else { 
			geneOffset = location - g.getStart();
		}
	}
	
	public int hashCode() { 
		int code = 17;
		code += chrom.hashCode(); code *= 37;
		code += location; code *= 37;
		return code; 
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof ExprProbeModel)) { 
			return false; 
		}
		ExprProbeModel m = (ExprProbeModel)o;
		if(!chrom.equals(m.chrom)) { return false; }
		if(location != m.location) { return false; }
		if(intensity != m.intensity) { return false; }
		if(!m.strand.equals(strand)) { return false; }
		return true;
	}
}
