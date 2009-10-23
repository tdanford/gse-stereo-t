/*
 * Author: tdanford
 * Date: Sep 8, 2008
 */
package edu.mit.csail.cgs.sigma.expression.ewok;

import java.io.File;
import java.io.IOException;
import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.utils.Closeable;
import edu.mit.csail.cgs.utils.models.data.DataFrame;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.models.*;
import edu.mit.csail.cgs.sigma.expression.ewok.*;

public class ExprProbeModelExpander implements Expander<Region,ExprProbeModel>, Closeable {

	/**
	 * By default, loads all the probe models for a given experiment, and saves them to a 
	 * table.  
	 * 
	 * @param args
	 */
	public static void main(String[] args) { 
		BaseExpressionProperties props = new SudeepExpressionProperties();
		
		ArrayList<String> keys = new ArrayList<String>();
		if(args.length > 0) { 
			for(int i = 0; i < args.length; i++) { 
				keys.add(args[i]);
			}
		} else { 
			keys.addAll(props.getExptKeys());
		}

		for(String exptKey : keys) { 
			File outputFile = new File(props.getDir(), String.format("%s_probes.txt", exptKey));

			ExprProbeModelExpander mexp = new ExprProbeModelExpander(props, exptKey);

			GenomeExpander<ExprProbeModel> gexp = new GenomeExpander<ExprProbeModel>(mexp);
			String strain = props.parseStrainFromExptKey(exptKey);
			Genome genome = props.getGenome(strain);
			Iterator<ExprProbeModel> models = gexp.execute(genome);

			models = new FilterIterator<ExprProbeModel,ExprProbeModel>(
					new Filter<ExprProbeModel,ExprProbeModel>() { 
						public ExprProbeModel execute(ExprProbeModel m) { 
							if(Double.isNaN(m.intensity) || Double.isNaN(m.background)) { 
								m = null;
							}
							return m;
						}
					}, models);
		models = new FilterIterator<ExprProbeModel,ExprProbeModel>(new Filter<ExprProbeModel,ExprProbeModel>() { 
				public ExprProbeModel execute(ExprProbeModel m) { 
					return !Double.isNaN(m.intensity) ? m : null;
				}
			}, models);

			DataFrame<ExprProbeModel> frame = new DataFrame<ExprProbeModel>(ExprProbeModel.class, models);
			try {
				frame.save(outputFile);
			} catch (IOException e) {
				e.printStackTrace();
			}

			mexp.close();
		}
	}
	
	private Expander<Region,Gene> gener;
	private Expander<Region,ExpressionTwoChannelProbe> prober;
	
	public ExprProbeModelExpander(BaseExpressionProperties bps, String exptKey) {
		String strain = bps.parseStrainFromExptKey(exptKey);
		gener = bps.getGeneGenerator(strain);
		prober = new StandardTwoChannelProbeGenerator(bps, exptKey);
	}

	public Iterator<ExprProbeModel> execute(Region a) {
		OverlappingRegionFinder<Gene> geneFinder = new OverlappingRegionFinder<Gene>(gener.execute(a));
		LinkedList<ExprProbeModel> models = new LinkedList<ExprProbeModel>();
		
		Iterator<ExpressionTwoChannelProbe> probes = prober.execute(a);
		while(probes.hasNext()) { 
			ExpressionTwoChannelProbe p = probes.next();
			ExprProbeModel m = new ExprProbeModel(p);
			Collection<Gene> genes = geneFinder.findOverlapping(p);
			if(!genes.isEmpty()) { 
				LinkedList<Gene> gs = new LinkedList<Gene>(genes);
				m.setGene(gs.getFirst());
			}
			models.add(m);
		}
		
		return models.iterator();
	}

	public void close() {
		if(gener instanceof Closeable) { 
			((Closeable)gener).close();
		}
		if(prober instanceof Closeable) { 
			((Closeable)prober).close();
		}
		gener = null; prober = null;
	}

	public boolean isClosed() {
		return gener == null || prober == null;
	}

}
