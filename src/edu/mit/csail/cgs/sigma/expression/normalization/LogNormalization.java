/*
 * Author: tdanford
 * Date: Apr 15, 2009
 */
package edu.mit.csail.cgs.sigma.expression.normalization;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.utils.Closeable;

/**
 * Takes one argument: the 'key' name.
 * 
 * There's a second optional argument: if the second argument reads 'all', then 
 * the normalization *ignores* the experimental structure, and simply normalizes
 * all channels in a single "block" (that is, all experiments get normalized 
 * together).  
 * 
 * @author tdanford
 *
 */
public class LogNormalization {
	
	public static void main(String[] args) {
		LogNormalization norm = new LogNormalization();
		String key = args[0];
		WorkflowProperties props = new WorkflowProperties();
		File fplus = new File(props.getDirectory(), String.format("%s_plus.norm", key)); 
		File fminus = new File(props.getDirectory(), String.format("%s_negative.norm", key));
		
		try {
			System.out.println(String.format("Loading: %s", fplus.getName()));
			norm.load(new WorkflowDataLoader(fplus));
			System.out.println(String.format("Loading: %s", fminus.getName()));
			norm.load(new WorkflowDataLoader(fminus));
			
			WorkflowIndexing indexing = props.getIndexing(key);

			Set<String> exptNames = indexing.exptNames();
			Integer[][] blocks = new Integer[exptNames.size()][];
			int bidx = 0; 

			if(args.length < 2 || !args[1].equals("all")) { 
				for(String exptName : exptNames) { 
					blocks[bidx] = indexing.findChannels(null, exptName); 
					System.out.print(String.format("%d: [ ", bidx)); 
					for(Integer j : blocks[bidx]) { 
						System.out.print(j + " "); 
					}
					System.out.println("]"); 
					bidx++;
				} 
			} else { 
				blocks = new Integer[1][];
				blocks[0] = new Integer[indexing.getNumChannels()];
				for(int i = 0; i < indexing.getNumChannels(); i++) { 
					blocks[0][i] = i;
				}
			}
			
			System.out.println(String.format("Normalizing %d blocks.", blocks.length)); 
			norm.normalize(blocks);
			
			Filter<ProbeLine,ProbeLine> plus = new Filter<ProbeLine,ProbeLine>() { 
				public ProbeLine execute(ProbeLine p) { 
					return p.strand.equals("+") ? p : null;
				}
			};
			Filter<ProbeLine,ProbeLine> minus = new Filter<ProbeLine,ProbeLine>() { 
				public ProbeLine execute(ProbeLine p) { 
					return p.strand.equals("-") ? p : null;
				}
			};

			System.out.println(String.format("Outputting normalized results..."));
			outputProbes(new FilterIterator<ProbeLine,ProbeLine>(plus, norm.iterator()), 
					new File(props.getDirectory(), String.format("%s_plus.data", key)));
			outputProbes(new FilterIterator<ProbeLine,ProbeLine>(minus, norm.iterator()), 
					new File(props.getDirectory(), String.format("%s_negative.data", key)));
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void outputProbes(Iterator<ProbeLine> lines, File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		while(lines.hasNext()) { 
			ProbeLine line = lines.next();
			ps.println(line.toString());
		}
		ps.close();
	}

	private ArrayList<ProbeLine> lines;
	
	public LogNormalization() { 
		lines = new ArrayList<ProbeLine>();
	}
	
	public void load(Iterator<ProbeLine> ps) {
		while(ps.hasNext()) { 
			lines.add(ps.next());
		}

		if(ps instanceof Closeable) { 
			Closeable c = (Closeable)ps;
			c.close();
		}
	}
	
	public void normalize(Integer[]... sets) { 
		if(lines.isEmpty()) { return; }
		for(Integer[] set : sets) { 
			logValues(set);
		}
	}
	
	private void logValues(Integer[] set) { 
		for(ProbeLine line : lines) { 
			for(int i = 0; i < line.values.length; i++) { 
				line.values[i] = logValue(line.values[i]);			
			}
		}
	}

	private double logValue(Double v) { 
		if(v == null || Double.isInfinite(v) || Double.isNaN(v) || v <= 1.0) { 
			return 0.0;
		} else { 
			return Math.log(v);
		}
	}

	public Iterator<ProbeLine> iterator() { return lines.iterator(); }

}


