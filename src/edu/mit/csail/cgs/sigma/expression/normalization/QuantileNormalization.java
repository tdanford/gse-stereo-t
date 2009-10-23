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
public class QuantileNormalization {
	
	public static void main(String[] args) {
		//BackgroundEstimation estimation = new BolstadBackgroundEstimation();
		//QuantileNormalization norm = new QuantileNormalization(estimation);

		QuantileNormalization norm = new QuantileNormalization();
		String key = args[0];

		WorkflowProperties props = new WorkflowProperties();
		File fplus = new File(props.getDirectory(), String.format("%s_plus.raw", key)); 
		File fminus = new File(props.getDirectory(), String.format("%s_negative.raw", key));
		
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
					new File(props.getDirectory(), String.format("%s_plus.norm", key)));
			outputProbes(new FilterIterator<ProbeLine,ProbeLine>(minus, norm.iterator()), 
					new File(props.getDirectory(), String.format("%s_negative.norm", key)));
			
			
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

	private BackgroundEstimation bg;
	private AdditiveCorrection corrected;
	private ArrayList<IndexedValue[]> sorted; 
	
	public QuantileNormalization() { 
		this(new NoBackgroundEstimation());
	}
	
	public QuantileNormalization(BackgroundEstimation est) { 
		bg = est;
		corrected = new AdditiveCorrection(bg);
		sorted = null;
	}
	
	public void load(Iterator<ProbeLine> ps) {
		corrected.load(ps);

		if(ps instanceof Closeable) { 
			Closeable c = (Closeable)ps;
			c.close();
		}
		
		corrected.correct();
	}
	
	public void normalize(Integer[]... sets) { 
		if(corrected.isEmpty()) { 
			System.err.println("corrected.isEmpty() == true ; // returning.");
			return; 
		}
		sort();
		for(Integer[] set : sets) { 
			average(set);
		}
		unsort();
	}
	
	private void sort() {
		sorted = new ArrayList<IndexedValue[]>();
		int channels = corrected.getNumChannels();
		int size = corrected.size();
		
		for(int j = 0; j < channels; j++) { 
			IndexedValue[] col = new IndexedValue[size];
			for(int i = 0; i < size; i++) { 
				Double value = corrected.get(i).values[j]; 
				col[i] = new IndexedValue(value, i);
			}
			Arrays.sort(col);
			sorted.add(col);
		}		
	}

	private void unsort() { 
		for(int j = 0; j < sorted.size(); j++) {
			IndexedValue[] col = sorted.get(j);
			for(int i = 0; i < col.length; i++) {
				corrected.get(col[i].idx).values[j] = col[i].value; 
				//corrected.get(col[i].idx).values[j] = col[i].logValue(); 
			}
		}
	}

	private void average(Integer[] set) { 
		System.out.println(String.format("Averaging %d entries (%d expts)", corrected.size(), set.length));
		for(int i = 0; i < corrected.size(); i++) { 
			double sum = 0.0;
			int n = 0; 

			for(int k = 0; k < set.length; k++) { 
				int j = set[k];
				Double value = sorted.get(j)[i].value;
				
				if(!isBad(value) && value > 0.0) { 
					sum += Math.log(value);
					n += 1; 
				}
			}
			
			sum /= (double)Math.max(1, n);  

			for(int k = 0; k < set.length; k++) { 
				int j = set[k];
				sorted.get(j)[i].value = sum; 
			}
		}
	}

	public boolean isBad(Double v) { 
		return v == null || Double.isNaN(v) || Double.isInfinite(v);
	}

	public Iterator<ProbeLine> iterator() { return corrected.iterator(); }

	private class IndexedValue implements Comparable<IndexedValue> {
		
		public Integer idx; 
		public Double value; 
		
		public IndexedValue(Double v, int i) { 
			value = v; idx = i; 
		}
		
		public int hashCode() { return idx.hashCode(); }
		
		public boolean equals(Object o) { 
			if(!(o instanceof IndexedValue)) { return false; }
			IndexedValue iv = (IndexedValue)o;
			return iv.idx.equals(idx);
		}

		public Double logValue() { 
			if(value > 1.0) { 
				return Math.log(value);
			} else { 
				return 0.0;
			}
		}
		
		public boolean error() { 
			return value == null || Double.isInfinite(value) || Double.isNaN(value); 
		}

		public int compareTo(IndexedValue iv) {
			if(error() || iv.error()) { 
				if(error() && iv.error()) { 
					if(idx < iv.idx) { return -1; }
					if(idx > iv.idx) { return 1; }
					return 0; 
				} else { 
					if(error()) { 
						return -1; 
					} else { 
						return 1; 
					}
				}
			}
			
			if(value < iv.value) { return -1; }
			if(value > iv.value) { return 1; }
			if(idx < iv.idx) { return -1; }
			if(idx > iv.idx) { return 1; }
			return 0; 
		}
	}
}


