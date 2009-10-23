/*
 * Author: tdanford
 * Date: Oct 9, 2008
 */
package edu.mit.csail.cgs.sigma.expression.regression;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.datasets.general.*;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.models.*;

import edu.mit.csail.cgs.utils.Accumulator;
import edu.mit.csail.cgs.utils.Predicate;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.models.data.ATransformation;
import edu.mit.csail.cgs.utils.models.data.DataFrame;
import edu.mit.csail.cgs.utils.models.data.Transformation;

/**
 * Outputs the data matrix necessary for the lmer() and BUGS multilevel models in R.  
 * 
 * @author tdanford
 */
public class MultilevelRExporter {

	/**
	 * Requires two arguments: 
	 * (1) the name of the input file, the raw probe model output
	 * (2) the name of the output file.  
	 * @param args
	 */
	public static void main(String[] args) { 
		SigmaProperties props = new SigmaProperties();
		File dir = props.getBaseDir();
		
		Pattern p = Pattern.compile("(.*)_probes.txt");
		
		for(int i = 0; i < args.length; i++) { 
			File input = new File(args[i]);
			String inputName = input.getName();
			Matcher m = p.matcher(inputName);
			if(!m.matches()) { 
				continue;
			}
			
			String exptKey = m.group(1);
			File output = new File(dir, String.format("%s_Rtable.txt", exptKey));
			
			try {
				DataFrame<RDataLine> lines = new DataFrame<RDataLine>(RDataLine.class);

				DataFrame<ExprProbeModel> probes = new DataFrame<ExprProbeModel>(ExprProbeModel.class, input);
				probes = probes.filter(new Predicate<ExprProbeModel>() {
					public boolean accepts(ExprProbeModel v) {
						return !v.gene.equals("NA") && v.strand.equals(v.geneStrand);
					} 
				});

				Transformation<ExprProbeModel,RDataLine> fgLiner = 
					new ATransformation<ExprProbeModel,RDataLine>(ExprProbeModel.class, RDataLine.class) {
					public RDataLine transform(ExprProbeModel v) {
						Double intensity = v.intensity;
						Integer offset = v.geneOffset;
						String gene = v.gene;
						Integer fg = 1;
						return new RDataLine(intensity, offset, gene, fg);
					} 
				};

				Transformation<ExprProbeModel,RDataLine> bgLiner = 
					new ATransformation<ExprProbeModel,RDataLine>(ExprProbeModel.class, RDataLine.class) {
					public RDataLine transform(ExprProbeModel v) {
						Double intensity = v.background;
						Integer offset = v.geneOffset;
						String gene = v.gene;
						Integer fg = 0;
						return new RDataLine(intensity, offset, gene, fg);
					} 
				};

				DataFrame<RDataLine> fgLines = probes.transform(fgLiner);
				DataFrame<RDataLine> bgLines = probes.transform(bgLiner);

				lines = lines.extend(fgLines).extend(bgLines);

				lines.save(output);
				System.out.println("Saved: " + output.getName());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class RDataLine extends Model { 
		public Double intensity;
		public Double offset;
		public String gene;
		public Integer fg;
		
		public RDataLine(Double in, Integer off, String g, Integer f) { 
			intensity = in; 
			offset = (double)off / 100.0;
			gene = g;
			fg = f;
		}
	}
	
	public static abstract class StoredAccumulator<S,T> implements Accumulator<T> { 
		protected S stored;
		public StoredAccumulator(S s) { stored = s; }
	}
}




