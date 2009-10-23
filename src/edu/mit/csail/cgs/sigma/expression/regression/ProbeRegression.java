/*
 * Author: tdanford
 * Date: Aug 29, 2008
 */
package edu.mit.csail.cgs.sigma.expression.regression;

import java.util.*;
import java.io.*;

import Jama.Matrix;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.models.*;
import edu.mit.csail.cgs.utils.Predicate;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.models.data.DataFrame;
import edu.mit.csail.cgs.utils.models.data.DataRegression;

public class ProbeRegression {

	public static void main(String[] args) { 
		File f = new File(args[0]);
		try {
			DataFrame<ExprProbeModel> frame = new DataFrame<ExprProbeModel>(ExprProbeModel.class, f);
			frame = frame.filter(new Predicate<ExprProbeModel>() {
				public boolean accepts(ExprProbeModel v) {
					return !v.gene.equals("NA");
				} 
			});
			
			String model = "intensity ~ gene - 1";
			
			DataRegression<ExprProbeModel> reg = new DataRegression<ExprProbeModel>(frame, model);

			Matrix m1= reg.getPredictorMatrix();
			//DataRegression.printMatrix(m1, System.out, 2);
			
			Map<String,Double> coeffs = reg.calculateRegression();
			Map<String,Double[]> bounds = reg.calculateBounds();
			
			TreeSet<String> nameOrder = new TreeSet<String>(coeffs.keySet());
			
			for(String name : nameOrder) { 
				Double coeff = coeffs.get(name);
				Double[] b = bounds.get(name);
				System.out.println(String.format("%s \t%.3f\t(%.2f, %.2f)", name, coeff, b[0], b[1]));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	private DataFrame<PRModel> frame;
	private DataRegression<PRModel> regression;
	private int offsetStart;
	private Double intercept, slope;
	
	public ProbeRegression(int base, Iterator<ExpressionProbe> itr) {
		offsetStart = base;
		LinkedList<PRModel> models = new LinkedList<PRModel>();
		while(itr.hasNext()) { 
			models.add(new PRModel(itr.next(), base));
		}
		frame = new DataFrame<PRModel>(PRModel.class, models);
		regression = new DataRegression<PRModel>(frame, "value ~ offset");
		Map<String,Double> coeffs = regression.calculateRegression();
		intercept = coeffs.get("(Intercept)");
		slope = coeffs.get("offset");
	}
	
	public ProbeRegression(int[] offsets, double[] values) {
		offsetStart = 0;
		if(offsets.length != values.length) { 
			throw new IllegalArgumentException();
		}
		LinkedList<PRModel> models = new LinkedList<PRModel>();
		for(int i = 0; i < offsets.length; i++) { 
			models.add(new PRModel(offsets[i], values[i]));
		}
		frame = new DataFrame<PRModel>(PRModel.class, models);
		regression = new DataRegression<PRModel>(frame, "value ~ offset");
		Map<String,Double> coeffs = regression.calculateRegression();
		intercept = coeffs.get("(Intercept)");
		slope = coeffs.get("offset");		
	}
	
	public ProbeRegression(int base, ExpressionProbe[] ps, int i1, int i2) {
		offsetStart = base;
		LinkedList<PRModel> models = new LinkedList<PRModel>();
		for(int i = i1; i < i2; i++) { 
			models.add(new PRModel(ps[i], base));
		}
		frame = new DataFrame<PRModel>(PRModel.class, models);
		regression = new DataRegression<PRModel>(frame, "value ~ offset");
		Map<String,Double> coeffs = regression.calculateRegression();
		intercept = coeffs.get("(Intercept)");
		slope = coeffs.get("offset");		
	}
	
	public Double getIntercept() { return intercept; }
	public Double getSlope() { return slope; }
	
	public Double predict(int offset) { 
		int dx = offset-offsetStart;
		double dy = slope*dx;
		return intercept + dy;
	}

	public class PRModel extends Model {  
		public Integer offset;
		public Double value;
		
		public PRModel(ExpressionProbe p, int base) { 
			offset = Math.abs(p.getLocation()-base);
			value = p.meanlog();
		}
		
		public PRModel(int off, double val) { 
			offset = off; value = val;
		}
	}
}

