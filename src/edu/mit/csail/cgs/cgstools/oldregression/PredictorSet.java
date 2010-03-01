package edu.mit.csail.cgs.cgstools.oldregression;

import java.util.*;
import java.io.*;
import Jama.*;

/**
 * @author tdanford
 * 
 * Contains a complete set of Valuations, which are to be used as the 'predictors' in regression,
 * separated by name and and by 'type' (quantitative, indicator, or categorical).  Maintains the
 * guarantee that only one predictor of a given name is present.
 */
public class PredictorSet {

	private Vector<String> totalNames;
	private Map<String,Valuation<Double>> quantPredict;
	private Map<String,Valuation<Integer>> categPredict;
	private Map<String,Valuation<Boolean>> indicatePredict;
	
	public PredictorSet() { 
		totalNames = new Vector<String>();
		quantPredict = new TreeMap<String,Valuation<Double>>();
		categPredict = new TreeMap<String,Valuation<Integer>>();
		indicatePredict = new TreeMap<String,Valuation<Boolean>>();
	}
	
	public Vector<String> getPredictorNames() { 
		Vector<String> ns = new Vector<String>();
		ns.addAll(quantPredict.keySet());
		ns.addAll(categPredict.keySet());
		ns.addAll(indicatePredict.keySet());
		return ns;
	}
	
	public int size() { return totalNames.size(); }
	
	public String toString() { 
		return String.format(
				"%d predictors (%d Quant, %d Categ, %d Indicator)",
				totalNames.size(), quantPredict.size(),
				categPredict.size(), indicatePredict.size());
	}

	public Valuation getPredictor(String n) { 
		if(totalNames.contains(n)) { 
			if(quantPredict.containsKey(n)) { return quantPredict.get(n); }
			if(categPredict.containsKey(n)) { return categPredict.get(n); }
			if(indicatePredict.containsKey(n)) { return indicatePredict.get(n); }
		}
		return null;
	}
	
	public double getMeanPredictor(String n, Datapoints dp) { 
		if(quantPredict.containsKey(n)) { 
			double sum = 0.0;
			int c = 0;
			for(int i = 0; i < dp.size(); i++) { 
				c += 1;
				sum += quantPredict.get(n).getValue(dp, i);
			}
			
			sum /= (double)c;
			return sum;
		}
		return 0.0;
	}
	
	public void recenter(String n, Datapoints dp) { 
		if(quantPredict.containsKey(n)) { 
			double mean = getMeanPredictor(n, dp);
			Transformation t = new Transformation.ShiftTransformation(-mean);
			applyTransformation(n, t);
		}
	}
	
	public void applyTransformation(String n, Transformation t) { 
		if(quantPredict.containsKey(n)) { 
			quantPredict.put(n, new TransformedValuation(quantPredict.get(n), t));
		}
	}
	
	public Double[] getQuantitativeValues(Datapoints dp, String n) { 
		Double[] array = new Double[dp.size()];
		for(int i = 0; i < dp.size(); i++) { 
			array[i] = quantPredict.get(n).getValue(dp, i);
		}
		return array;
	}
	
	public Integer[] getCategoricalValues(Datapoints dp, String n) { 
		Integer[] array = new Integer[dp.size()];
		for(int i = 0; i < dp.size(); i++) { 
			array[i] = categPredict.get(n).getValue(dp, i);
		}
		return array;
	}
	
	public Boolean[] getIndicatorValues(Datapoints dp, String n) { 
		Boolean[] array = new Boolean[dp.size()];
		for(int i = 0; i < dp.size(); i++) { 
			array[i] = indicatePredict.get(n).getValue(dp, i);
		}
		return array;
	}
	
	public void writeToFile(Datapoints dp, File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		
		ps.print("Index:");
		for(String n : quantPredict.keySet()) { 
			ps.print("\t" + n + ":");
		}
		for(String n : categPredict.keySet()) { 
			ps.print("\t" + n + ":");
		}
		for(String n : indicatePredict.keySet()) { 
			ps.print("\t" + n + ":");
		}
		ps.println();
		
		for(int i = 0; i < dp.size(); i++) { 
			ps.print(String.format("%d", i));
			for(String n : quantPredict.keySet()) {
				ps.print("\t" + quantPredict.get(n).getValue(dp, i));
			}
			for(String n : categPredict.keySet()) { 
				ps.print("\t" + categPredict.get(n).getValue(dp, i));
			}
			for(String n : indicatePredict.keySet()) { 
				ps.print("\t" + indicatePredict.get(n).getValue(dp, i));
			}
			ps.println();
		}
		
		ps.close();
	}
	
	public Matrix createMatrix(Datapoints dp) { 
		Matrix m = new Matrix(dp.size(), totalNames.size());
		int c = 0;
		
		for(String n : quantPredict.keySet()) { 
			Double[] array = getQuantitativeValues(dp, n);
			for(int r = 0; r < dp.size(); r++) { 
				m.set(r, c, array[r]);
			}
			c++;
		}
		
		for(String n : categPredict.keySet()) { 
			Integer[] array = getCategoricalValues(dp, n);
			for(int r = 0; r < dp.size(); r++) { 
				m.set(r, c, (double)array[r]);
			}
			c++;
		}
		
		for(String n : indicatePredict.keySet()) { 
			Boolean[] array = getIndicatorValues(dp, n);
			for(int r = 0; r < dp.size(); r++) { 
				m.set(r, c, array[r] ? 1.0 : 0.0);
			}
			c++;
		}
		
		return m;
	}

	public boolean containsPredictor(String n) { return totalNames.contains(n); }
	
	public static String offsetPredictorName = "@offset";
	
	public void addConstantPredictor() {
		ConstantValuation<Double> cval = new ConstantValuation<Double>(offsetPredictorName, 1.0);
		addQuantitativePredictor(cval);
	}
	
	public void addQuantitativePredictor(Valuation<Double> p) {
		String n = p.getName();
		if(!containsPredictor(n)) { 
			quantPredict.put(n, p);
			totalNames.add(n);
		}
	}
	
	public void addCategoricalPredictor(Valuation<Integer> p) { 
		String n = p.getName();
		if(!containsPredictor(n)) { 
			categPredict.put(n, p);
			totalNames.add(n);
		}
	}

	public void addCategoricalIndicatorPredictors(CategoricalValuation p) {
		int[] values = p.getValues();
		for(int i = 0; i < values.length; i++) { 
			IndicatorValuation cip = new IndicatorValuation(p, values[i]);
			addIndicatorPredictor(cip);
		}
	}

	public void addIndicatorPredictor(Valuation<Boolean> p) { 
		String n = p.getName();
		if(!containsPredictor(n)) { 
			indicatePredict.put(n, p);
			totalNames.add(n);
		}
	}
}
