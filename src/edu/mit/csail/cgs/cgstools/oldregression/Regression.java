package edu.mit.csail.cgs.cgstools.oldregression;

import Jama.*;
import java.util.*;
import java.io.*;

public class Regression {
	
	public static void main(String[] args) { 
		File f = new File("C:\\Users\\tdanford\\Desktop\\regression.txt");
		try {
			System.out.println("Loading: " + f.getName());
			Regression r = loadRegression(f);

			/*
			for(String n : r.predictors.getPredictorNames()) { 
				r.predictors.recenter(n, r.data);
			}
			*/
			
			r.predictors.addConstantPredictor();
			
			printMatrix(r.getX(), System.out);
			
			Matrix betaHat = r.calculateBetaHat();
			double s2 = r.calculateS2(betaHat);
			
			System.out.println(String.format("s2: %.5f", s2));
			System.out.println(String.format("RMS: %.5f", Math.sqrt(s2 / (double)r.data.size())));
			//printMatrix(betaHat, System.out);
			
			Vector<String> predNames = r.predictors.getPredictorNames();
			for(int i = 0; i < predNames.size(); i++) { 
				System.out.println(String.format("%s:\t%.3f", predNames.get(i), betaHat.get(i, 0)));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void printMatrix(Matrix m, PrintStream ps) { 
		ps.println(String.format("Dimension: %d x %d", m.getRowDimension(), m.getColumnDimension()));
		for(int r = 0; r < m.getRowDimension(); r++) { 
			for(int c = 0; c < m.getColumnDimension(); c++) { 
				ps.print(String.format("%.2f\t", m.get(r, c)));
			}
			ps.println();
		}
	}
	
	public static Regression loadRegression(File inf) throws IOException {
		PredictorSet ps = new PredictorSet();
		
		Vector<String> names = new Vector<String>();
		Vector<PredictorType> types = new Vector<PredictorType>();
		Vector<Vector<String>> values = new Vector<Vector<String>>();
		int ndps = 0;
		
		BufferedReader br = new BufferedReader(new FileReader(inf));
		
		String line = br.readLine();
		String[] array = line.split("\t");
		for(int i = 0; i < array.length; i++) {
			String n = array[i].trim();
			if(n.length() > 0) { 
				names.add(n);

				String[] na = n.split(":");
				PredictorType type = PredictorType.QUANT;
				if(na.length > 1) { 
					PredictorType pt = PredictorType.valueOf(na[1]);
					if(pt != null) { type = pt; }
				}

				types.add(type);
				values.add(new Vector<String>());
			}
		}

		while((line = br.readLine()) != null) {
			line = line.trim();
			if(line.length() > 0) {
				array = line.split("\t");
				if(array.length == names.size()) { 
					for(int i = 0; i < names.size(); i++) { 
						values.get(i).add(array[i]);
					}
					ndps++;
				}
			}
		}
		
		br.close();
		
		for(int i = 1; i < names.size(); i++) { 
			Valuation vp = parseValuation(types.get(i), names.get(i), values.get(i));
			switch(types.get(i)) { 
			case QUANT:
				ps.addQuantitativePredictor((Valuation<Double>)vp);
				break;
			case IND:
				ps.addIndicatorPredictor((Valuation<Boolean>)vp);
				break;
			case CAT:
				ps.addCategoricalPredictor((Valuation<Integer>)vp);
				break;
			case CATIND:
				ps.addCategoricalIndicatorPredictors((CategoricalArrayValuation)vp);
				break;
			}
		}
		
		Valuation<Double> vals = 
			(Valuation<Double>)parseValuation(PredictorType.QUANT, names.get(0), values.get(0));
		
		Datapoints dps = new Datapoints(ndps);
		
		return new Regression(vals, ps, dps);
	}
	
	public static Valuation parseValuation(PredictorType type, String n, Vector<String> vals) { 
		switch(type) { 
		case QUANT:
			Double[] dv = new Double[vals.size()];
			for(int i = 0; i < dv.length; i++) { 
				dv[i] = Double.parseDouble(vals.get(i));
			}
			Valuation<Double> vd = new ArrayValuation<Double>(n, dv);
			return vd;
		case IND:
			Boolean[] bv = new Boolean[vals.size()];
			for(int i = 0; i < bv.length; i++) { 
				if(vals.get(i).equals("1")) { 
					bv[i] = true;
				} else { 
					bv[i] = false;
				}
			}
			Valuation<Boolean> vb = new ArrayValuation<Boolean>(n, bv);
			return vb;
		case CAT:
		case CATIND:
			Integer[] iv = new Integer[vals.size()];
			for(int i = 0; i < iv.length; i++) { 
				iv[i] = Integer.parseInt(vals.get(i));
			}
			CategoricalArrayValuation vi = new CategoricalArrayValuation(n, iv);
			return vi;
		}
		return null;
	}
	
	public static enum PredictorType { QUANT, CAT, IND, CATIND };

	private PredictorSet predictors;
	private Valuation<Double> values;
	private Datapoints data;
	
	public Regression(Valuation<Double> vs, PredictorSet pds, Datapoints dps) { 
		predictors = pds;
		values = vs;
		data = dps;
	}
	
	public double calculateS2(Matrix betaHat) { 
		// pg. 356 of Gelman

		// n : number of datapoints
		// k : number of predictors
		double n = (double)data.size();
		double k = (double)predictors.size();
		double coeff = 1.0 / (n - k);
		
		Matrix y = getY();
		Matrix X = predictors.createMatrix(data);
		if(betaHat == null) { betaHat = calculateBetaHat(); }
		
		Matrix half = y.minus(X.times(betaHat));
		
		Matrix product = half.transpose().times(half);
		
		double ret = coeff * product.get(0, 0);
		return ret;
	}
	
	public Matrix calculateBetaHat() {
		// pg. 356 of Gelman
		
		// n : number of datapoints
		// k : number of predictors
		
		// Xtrans : k x n
		Matrix Xtrans = predictors.createMatrix(data).transpose();
		
		// Vbeta : k x k 
		Matrix Vbeta = Xtrans.times(Xtrans.transpose());
		Vbeta = Vbeta.inverse();
		
		// ytransf : k x 1 
		Matrix ytransf = Xtrans.times(getY());
		
		// res : k x 1
		Matrix res = Vbeta.times(ytransf);
		
		return res;
	}
	
	public int getSize() { return data.size(); }
	
	public Matrix getX() { return predictors.createMatrix(data); }
	
	public Matrix getY() { 
		Matrix m = new Matrix(data.size(), 1);
		Double[] array = getValueArray(data, values);
		for(int i = 0; i < data.size(); i++) { 
			m.set(i, 0, array[i]);
		}
		return m;
	}
	
	public static Vector<Double> getColumn(Matrix m, int c) { 
		Vector<Double> vals = new Vector<Double>();
		for(int i = 0; i < m.getRowDimension(); i++) { 
			vals.add(m.get(i, c));
		}
		return vals;
	}
	
	public static Vector<Double> getRow(Matrix m, int r) { 
		Vector<Double> vals = new Vector<Double>();
		for(int i = 0; i < m.getColumnDimension(); i++) { 
			vals.add(m.get(r, i));
		}
		return vals;
	}
	
	public static Double[] getValueArray(Datapoints dp, Valuation<Double> p) { 
		Double[] array = new Double[dp.size()];
		for(int i = 0; i < dp.size(); i++) { 
			array[i] = p.getValue(dp, i);
		}
		return array;
	}
	
	public static Integer[] getValueArray(Datapoints dp, Valuation<Integer> p) { 
		Integer[] array = new Integer[dp.size()];
		for(int i = 0; i < dp.size(); i++) { 
			array[i] = p.getValue(dp, i);
		}
		return array;
	}
	
	public static Boolean[] getValueArray(Datapoints dp, Valuation<Boolean> p) { 
		Boolean[] array = new Boolean[dp.size()];
		for(int i = 0; i < dp.size(); i++) { 
			array[i] = p.getValue(dp, i);
		}
		return array;
	}
	
}
