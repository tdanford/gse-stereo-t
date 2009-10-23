/*
 * Author: tdanford
 * Date: Feb 24, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import edu.mit.csail.cgs.datasets.general.NamedRegion;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.ewok.verbs.CastingMapper;
import edu.mit.csail.cgs.ewok.verbs.ChromRegionIterator;
import edu.mit.csail.cgs.ewok.verbs.ExpanderIterator;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.ewok.verbs.MapperIterator;
import edu.mit.csail.cgs.ewok.verbs.RefGeneGenerator;
import edu.mit.csail.cgs.sigma.GeneGenerator;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotation;
import edu.mit.csail.cgs.utils.models.ModelInput;
import edu.mit.csail.cgs.utils.models.ModelInputIterator;

public class WorkflowGeneGenerator implements Iterator<GeneAnnotation> {
	
	public static void main(String[] args) { 
		String strain = args.length > 0 ? args[0] : "sigma";
		WorkflowProperties props = new WorkflowProperties();
		WorkflowGeneGenerator ggen = new WorkflowGeneGenerator(props, strain);
		File f = new File(props.getDirectory(), String.format("%s_genes.txt", strain));
		try { 
			PrintStream ps = new PrintStream(new FileOutputStream(f));
			int i = 0;
			while(ggen.hasNext()) { 
				GeneAnnotation annote = ggen.next();
				ps.println(annote.asJSON().toString());
				i += 1;
				if(i % 100 == 0) { System.out.print("."); System.out.flush(); }
				if(i % 1000 == 0) { System.out.println("*"); }
			}
			System.out.println();
			ps.close();
		} catch(IOException e) { 
			e.printStackTrace(System.err);
		}
	}
	
	private Iterator<GeneAnnotation> genes;
	
	public WorkflowGeneGenerator(WorkflowProperties ps, String strain) { 
		SigmaProperties sprops = ps.getSigmaProperties();
		GeneGenerator gener = sprops.getGeneGenerator(strain);
		Iterator<Gene> gs = 
			new ExpanderIterator<Region,Gene>(gener,
				new MapperIterator<NamedRegion,Region>(new CastingMapper<NamedRegion,Region>(),
						new ChromRegionIterator(sprops.getGenome(strain))));
		genes = new MapperIterator<Gene,GeneAnnotation>(new GeneToAnnotationMapper(), gs);
	}
	
	public WorkflowGeneGenerator(File f) throws IOException { 
		genes = new ModelInputIterator<GeneAnnotation>(
				new ModelInput.LineReader(GeneAnnotation.class, new FileReader(f)));
	}

	public boolean hasNext() {
		return genes.hasNext();
	}

	public GeneAnnotation next() {
		return genes.next();
	}

	public void remove() {
		genes.remove();
	}
	
	private static class GeneToAnnotationMapper implements Mapper<Gene,GeneAnnotation> {
		public GeneAnnotation execute(Gene a) {
			String id = a.getID();
			String c = a.getChrom();
			int start = a.getStart(), end = a.getEnd();
			String strand = String.valueOf(a.getStrand());
			return new GeneAnnotation(c, start, end, strand, id);
		} 
	}
}
