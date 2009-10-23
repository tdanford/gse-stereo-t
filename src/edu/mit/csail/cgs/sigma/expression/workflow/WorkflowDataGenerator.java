/*
 * Author: tdanford
 * Date: Dec 15, 2008
 */
package edu.mit.csail.cgs.sigma.expression.workflow;

import edu.mit.csail.cgs.datasets.general.NamedRegion;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.ChromRegionIterator;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.ewok.verbs.GenomeExpander;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.ewok.StandardTwoChannelProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionTwoChannelProbe;

import java.util.*;
import java.io.*;

/**
 * Loads, and outputs, a file for a single chromosome's worth of data from 
 * one or more experiments.  This does all the necessary data cleaning, such as: 
 * (1) making sure that the locations available from each experiment "match up"
 * (2) filtering by strand information
 * (3) ordering the probes output
 * (4) grabbing both channels of probe data, where available. 
 * 
 * @author tdanford
 * 
 * Arguments: 
 * 0: key (an arbitrary string).
 * 1: properties file (either 'new_sudeep' or 'new_stacie')
 * 2: strand (either '+' or '-')
 * 3+: key names (from the properties file, above) for the experiments.
 *
 */
public class WorkflowDataGenerator {
	
	public static void main(String[] args) {
		String key = args[0];
		BaseExpressionProperties ps = 
			//new SudeepExpressionProperties();
			new NewExpressionProperties(args[1]);
		
		char strand = args[2].charAt(0);
		String strandKey = strand == '+' ? "plus" : "negative";
		
		ArrayList<String> keylist = new ArrayList<String>();
		for(int i = 3; i < args.length; i++) { 
			keylist.add(args[i]);
		}
		String[] keys = keylist.toArray(new String[0]);
		
		String strain = ps.parseStrainFromExptKey(keys[0]);
		Genome genome = ps.getGenome(strain);
		
		File outf = new File(String.format("%s_%s.raw", key, strandKey));
		try { 
			PrintStream outps = new PrintStream(new FileOutputStream(outf));

			Iterator<NamedRegion> chromItr = new ChromRegionIterator(genome);
			while(chromItr.hasNext()) { 
				NamedRegion r = chromItr.next();
				String chrom = r.getChrom();
				WorkflowDataGenerator builder = 
					new WorkflowDataGenerator(ps, chrom, strand);
				builder.loadData(keys);
				builder.outputData(outps);
				System.err.println(String.format("%s", chrom));
			}

			outps.close();
		} catch(IOException e) { 
			e.printStackTrace(System.err);
		}
	}

	private SigmaProperties sigmaProps;
	private BaseExpressionProperties exprProps;
	private String[] exptKeys;
	private RawInput[] inputs;
	private char strand;
	private String chrom;
	
	public WorkflowDataGenerator(BaseExpressionProperties ps, String chr, char str) { 
		exprProps = ps;
		sigmaProps = exprProps.getSigmaProperties();
		exptKeys = null;
		inputs = null;
		chrom = chr;
		strand = str;
	}
	
	public void loadData(String[] exptKeys) {
		this.exptKeys = exptKeys.clone();
		if(exptKeys.length == 0) { throw new IllegalArgumentException("Zero-length exptKeys."); }
		
		String strain = exprProps.parseStrainFromExptKey(exptKeys[0]);
		Genome genome = exprProps.getGenome(strain);
		
		for(int i = 0; i < exptKeys.length; i++) { 
			if(!strain.equals(exprProps.parseStrainFromExptKey(exptKeys[i]))) { 
				throw new IllegalArgumentException(String.format("%s doesn't match " +
						"strain %s", exptKeys[i], strain));
			}
		}
		
		inputs = new RawInput[exptKeys.length];
		for(int i = 0; i < exptKeys.length; i++) { 
			inputs[i] = new RawInput(genome, exptKeys[i]);
		}
	}
	
	public void outputData(PrintStream ps) { 
		for(int i = 0; i < inputs[0].size(); i++) { 
			int loc = inputs[0].locations[i];
			ps.print(String.format("%s\t%c\t%d", chrom, strand, loc));
			for(int j = 0; j < inputs.length; j++) { 
				if(inputs[j].locations[i] != loc) { 
					throw new IllegalArgumentException(); 
				}
				inputs[j].printLocation(ps, i);
			}
			ps.println();
		}
	}
	
	private class RawInput { 

		public Genome genome; 
		public String exptKey;
		public Integer[] locations;
		public Double[][] values;
		
		public void printLocation(PrintStream ps, int idx) { 
			for(int i = 0; i < values.length; i++) { 
				ps.print(String.format("\t%.4f", values[i][idx]));
			}
		}

		public int size() { return locations.length; }
		
		public RawInput(Genome g, String exptKey) { 
			genome = g;
			this.exptKey = exptKey;
			
			StandardTwoChannelProbeGenerator gen = 
				new StandardTwoChannelProbeGenerator(exprProps, exptKey);
			
			Region chromRegion = new Region(genome, chrom, 0, genome.getChromLength(chrom)-1);
			
			Iterator<ExpressionTwoChannelProbe> pitr = gen.execute(chromRegion);
			pitr = 
				new FilterIterator<ExpressionTwoChannelProbe,ExpressionTwoChannelProbe>(
						new StrandFilter<ExpressionTwoChannelProbe>(strand),
						pitr);
			
			ArrayList<ExpressionTwoChannelProbe> probes = new ArrayList<ExpressionTwoChannelProbe>(); 
			while(pitr.hasNext()) { 
				probes.add(pitr.next());
			}
			ExpressionTwoChannelProbe[] array = probes.toArray(new ExpressionTwoChannelProbe[0]);
			probes = null;
			Arrays.sort(array);

			ArrayList<Integer> locs = new ArrayList<Integer>();
			ArrayList<Double[]> vals = new ArrayList<Double[]>();

			for(int i = 0; i < array.length; i++) { 
				ExpressionTwoChannelProbe p = array[i];
				int loc = p.getLocation();
				//Double v1 = value(p.meanlog(true)), v2 = value(p.meanlog(false));
				Double v1 = value(p.mean(true)), v2 = value(p.mean(false));
				locs.add(loc);
				vals.add(new Double[] { v1, v2 });
			}
			
			locations = locs.toArray(new Integer[0]);
			values = new Double[2][locations.length];
			
			int k = 0; 
			for(Double[] v : vals) { 
				values[0][k] = v[0]; 
				values[1][k] = v[1];
				k++;
			}
		}
	}

	public static Double value(Double v) { 
		return v == null || Double.isNaN(v) || Double.isInfinite(v) ? null : v;
	}
}


