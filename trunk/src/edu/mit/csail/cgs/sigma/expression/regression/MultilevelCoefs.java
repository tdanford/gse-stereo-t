/*
 * Author: tdanford
 * Date: Oct 24, 2008
 */
package edu.mit.csail.cgs.sigma.expression.regression;

import java.util.*;
import java.awt.Color;
import java.io.*;
import java.lang.reflect.*;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.GenomeExpander;
import edu.mit.csail.cgs.ewok.verbs.RefGeneGenerator;
import edu.mit.csail.cgs.sigma.GeneGenerator;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.sigma.genes.GeneNameAssociation;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.models.data.ATransformation;
import edu.mit.csail.cgs.utils.models.data.DataFrame;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.eye.*;
import edu.mit.csail.cgs.viz.paintable.*;

public class MultilevelCoefs {
	
	public static void main(String[] argarray) {
		CoefArgs args = CoefArgs.parse(argarray);
		File[] array = args.coefs();
		
		try { 
			
			MultilevelCoefs coefs1 = new MultilevelCoefs(array[0]);
			MultilevelCoefs coefs2 = new MultilevelCoefs(array[1]);
			
			DataFrame<JoinedGenes> frame = 
				coefs1.coefs.join(JoinedGenes.class, coefs2.coefs, "gene", "left", "right");
			DataFrame<XYPoint> points = 
				frame.transform(new ATransformation<JoinedGenes,XYPoint>(JoinedGenes.class, XYPoint.class) { 
					public XYPoint transform(JoinedGenes g) {
						return new XYPoint(g, "intensity");
					}
				});

			//scatter.setProperty(ModelScatter.xScaleKey, scatter.getPropertyValue(ModelScatter.yScaleKey));
			ModelScatterHistograms scatter = new ModelScatterHistograms("x", "y");
			
			scatter.rebin();
			
			//new ModelScatter.InteractiveFrame(scatter, args.compare);
			new PaintableFrame(args.compare, 
					new DoubleBufferedPaintable(
							new OverlayModelPaintable(
									new ModelPaintableWrapper(new BackgroundPaintable(Color.white)),
									scatter)));
			
		} catch(IOException e) { 
			e.printStackTrace(System.err);
		}
	}

	public SigmaProperties props;
	public GeneAnnotationProperties geneProps;
	public DataFrame<Coefs> coefs;
	
	public MultilevelCoefs(File f) throws IOException {
		props = new SigmaProperties();
		geneProps = new GeneAnnotationProperties();
		coefs = new DataFrame<Coefs>(Coefs.class, f, "gene", "intercept", "offset", "fg");
	}
	
	public Coefs[] values() { 
		Coefs[] carray = new Coefs[coefs.size()];
		for(int i = 0; i < carray.length; i++) { 
			carray[i] = coefs.object(i);
		}
		Arrays.sort(carray);
		return carray;
	}
	
	public Set<String> genes() { 
		return coefs.fieldValues("gene");
	}
	
	public Map<String,Double> geneValues(String valueName) { 
		try {
			Field f = Coefs.class.getField(valueName);
			Map<String,Double> map = new TreeMap<String,Double>();
			
			for(int i = 0; i < coefs.size(); i++) { 
				Coefs c = coefs.object(i);
				Double value = (Double)f.get(c);
				map.put(c.gene, value);
			}
			
			return map;
			
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public DataFrame<GeneLengthModel> loadGeneLengths(String strain) {
		
		DataFrame<GeneLengthModel> frame = new DataFrame<GeneLengthModel>(GeneLengthModel.class);
		GeneGenerator gen = props.getGeneGenerator(strain);
		GenomeExpander<Gene> exp = new GenomeExpander<Gene>(gen);
		Genome g = props.getGenome(strain);
		Iterator<Gene> genes = exp.execute(g);
		GeneNameAssociation assoc = geneProps.getGeneNameAssociation(strain);
		
		while(genes.hasNext()) { 
			Gene gene = genes.next();
			String id = gene.getID();
			int length = gene.getWidth();
			String name = assoc.getName(id);
			if(name == null) { name = id; }
			GeneLengthModel m = new GeneLengthModel(name, length);
			frame.addObject(m);
		}
		return frame;
	}

	public static class Coefs extends Model implements Comparable<Coefs> { 
		
		public static Boolean quote_gene = true;
		
		public String gene;
		public Double intercept;
		public Double fg;
		public Double offset;
		
		public int hashCode() { return gene.hashCode(); }
		
		public boolean equals(Object o) { 
			if(!(o instanceof Coefs)) { return false; }
			Coefs c = (Coefs)o;
			return c.gene.equals(gene);
		}
		
		public int compareTo(Coefs c) { 
			if(fg > c.fg) { return -1; }
			if(fg < c.fg) { return 1; }
			if(intercept > c.intercept) { return -1; }
			if(intercept < c.intercept) { return 1; }
			return gene.compareTo(c.gene);
		}
		
		public String toString() { 
			return String.format("%.3f\t%.3f\t%s", fg, intercept, gene);
		}
	}
	
	public static class JoinedGenes extends Model {
		
		public static ModelFieldAnalysis<JoinedGenes> analysis = 
			new ModelFieldAnalysis<JoinedGenes>(JoinedGenes.class); 
		
		public String gene;
		public Coefs left, right;
	}
	
	public static class GeneLengthModel extends Model { 
		
		public String gene;
		public Integer length;
		
		public GeneLengthModel(String g, Integer l) { 
			gene = g;
			length = l;
		}
	}
	
	public static class XYPoint extends Model {
		
		public Double x, y;
		public String gene;

		public XYPoint(double _x, double _y, String g) { x = _x; y = _y; gene = g;}
		
		public XYPoint(JoinedGenes g, String field) { 
			gene = g.gene;
			x = (Double)JoinedGenes.analysis.get(String.format("left.%s", field), g); 
			y = (Double)JoinedGenes.analysis.get(String.format("right.%s", field), g); 
		}
	}
}


