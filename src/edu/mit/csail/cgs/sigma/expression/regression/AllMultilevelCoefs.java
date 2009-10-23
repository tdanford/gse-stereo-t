/*
 * Author: tdanford
 * Date: Nov 11, 2008
 */
package edu.mit.csail.cgs.sigma.expression.regression;

import java.util.*;
import java.awt.Color;
import java.io.*;
import java.lang.reflect.*;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.models.data.DataFrame;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.eye.ModelScatter;

/**
 * Loads a set of MultilevelCoefs objects, and performs matrix operations on them.  
 * 
 * @author tdanford
 */
public class AllMultilevelCoefs {
	
	public static void main(String[] argarray) { 
		CoefArgs args = CoefArgs.parse(argarray);
		try {
			AllMultilevelCoefs coefs = new AllMultilevelCoefs(args);
			
			Matrix matrix = coefs.matrix(true);
			Matrix covar = matrix.transpose().times(matrix);
			SingularValueDecomposition svd = covar.svd();
			
			Matrix U = svd.getU();
			
			double[] d1 = normalize(column(U, 1));
			double[] d2 = normalize(column(U, 2));
			
			String[] genes = coefs.genes();
			
			DataFrame<MultilevelCoefs.XYPoint> points = 
				new DataFrame<MultilevelCoefs.XYPoint>(MultilevelCoefs.XYPoint.class); 
			
			for(int i = 0; i < genes.length; i++) { 
				double[] gr = row(matrix, i);
				double x = inner(gr, d1), y = inner(gr, d2);
				MultilevelCoefs.XYPoint point = 
					new MultilevelCoefs.XYPoint(x, y, genes[i]);
				points.addObject(point);
				System.out.println(point.toString());
			}

			System.out.println("U: ");
			println(U, System.out);

			ModelScatter scatter = new ModelScatter();
			scatter.addModels(points.iterator());
			
			scatter.setProperty(ModelScatter.colorKey, Coloring.clearer(Color.red));
			
			coefs.save(new File(String.format(
					"C:\\Documents and Settings\\tdanford\\Desktop\\sigma_%s.txt",
					args.compare)));
			
			new ModelScatter.InteractiveFrame(scatter, "PCA");
			
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void println(double[] p, PrintStream ps) { 
		for(int i = 0; i < p.length; i++) {
			ps.print(i > 0 ? " " : "");
			ps.print(String.format("%.2f", p[i]));
		}
		ps.println();
	}
	
	public static void println(Matrix m, PrintStream ps) { 
		for(int i = 0; i < m.getRowDimension(); i++) { 
			println(row(m, i), ps);
		}
	}
	
	public static double[] row(Matrix m, int i) { 
		double[] array = new double[m.getColumnDimension()];
		for(int k = 0; k < array.length; k++) { 
			array[k] = m.get(i, k);
		}
		return array;
	}
	
	public static double[] column(Matrix m, int i) {  
		double[] array = new double[m.getRowDimension()];
		for(int k = 0; k < array.length; k++) { 
			array[k] = m.get(k, i);
		}
		return array;
	}
	
	public static double[] normalize(double[] v) { 
		double sum = 0.0;
		for(int i = 0; i < v.length; i++) { 
			sum += v[i]*v[i];
		}
		sum = Math.sqrt(sum);
		double[] nv = v.clone();
		for(int i = 0;i < nv.length; i++) { 
			nv[i] /= sum;
		}
		return nv;
	}
	
	public static double inner(double[] v1, double[] v2) { 
		if(v1.length != v2.length) { 
			throw new IllegalArgumentException(String.format("%d != %d", v1.length, v2.length));
		}
		
		double sum = 0.0;
		for(int i = 0; i < v1.length; i++) { 
			sum += v1[i] * v2[i];
		}
		return sum;
	}

	private CoefArgs args;
	private Vector<String> keys;
	private Vector<File> files;
	private Vector<MultilevelCoefs> coefs;

	public AllMultilevelCoefs(CoefArgs args) throws IOException {
		this.args = args;
		files = new Vector<File>();
		keys = new Vector<String>();
		coefs = new Vector<MultilevelCoefs>();
		
		File dir = args.dir();
		for(int i = 0; i < args.keys.length; i++) { 
			keys.add(args.keys[i]);
			File f = CoefArgs.coefFile(dir, args.keys[i]);
			files.add(f);
			
			coefs.add(new MultilevelCoefs(f));
			System.out.println(String.format("Loaded: %s", args.keys[i]));
		}
	}
	
	public void save(File f) throws IOException {
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		String[] genes = genes();
		
		Vector<Map<String,Double>> maps = new Vector<Map<String,Double>>();
		for(int j = 0; j < coefs.size(); j++) { 
			maps.add(coefs.get(j).geneValues(args.compare));
		}
		
		ps.print("gene");
		for(int i = 0; i < coefs.size(); i++) { 
			ps.print(" " + keys.get(i));
		}
		ps.println();
		
		for(int i = 0; i < genes.length; i++) { 
			ps.print(String.format("%s", genes[i]));
			
			for(int j = 0; j < coefs.size(); j++) {
				Map<String,Double> map = maps.get(j);
				Double value = map.containsKey(genes[i]) ? map.get(genes[i]) : null;
				
				ps.print(String.format(" %s", value != null ? String.format("%.2f", value) : "N/A"));
			}
			
			ps.println();
		}
		
		ps.println();
		ps.close();
	}
	
	private String[] keys() { return keys.toArray(new String[0]); }
	
	public String[] genes() { 
		String[] genes = coefs.get(0).genes().toArray(new String[0]); 
		return genes;
	}
	
	public Matrix matrix(boolean centered) {
		String[] genes = genes();
		Matrix matrix = new Matrix(genes.length, coefs.size());

		for(int j = 0; j < coefs.size(); j++) { 
			Map<String,Double> values = coefs.get(j).geneValues(args.compare);
			double sum = 0.0;
			
			for(int i = 0; i < genes.length; i++) { 
				Double value = values.get(genes[i]);
				if(value != null) { 
					matrix.set(i, j, value);
					sum += value;
				} else { 
					matrix.set(i, j, 0.0);
				}
			}
			
			if(centered) { 
				sum /= (double)genes.length;
				for(int i = 0; i < genes.length; i++) { 
					matrix.set(i, j, matrix.get(i, j) - sum);
				}
			}
		}

		return matrix;
	}
}
