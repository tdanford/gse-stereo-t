/*
 * Author: tdanford
 * Date: Apr 28, 2009
 */
package edu.mit.csail.cgs.sigma.expression.rma;

import java.util.Arrays;

public class MedianPolish {

	private Double[][] matrix;
	
	private int rows, cols;
	private Double[] r, c, rmeds, cmeds;
	private Double m;
	
	private Double[] rowTemp, colTemp;
	
	public MedianPolish(Double[][] data) {
		rows = cols = -1;
		rows = data.length;
		matrix = new Double[data.length][];
		for(int i = 0; i < data.length; i++) { 
			matrix[i] = data[i].clone();
			if(cols == -1) { 
				cols = data[i].length; 
			} else { 
				if(data[i].length != cols) { 
					throw new IllegalArgumentException(
							String.format("%d != %d", data[i].length, cols));
				}
			}
		}
		
		rowTemp = new Double[cols];
		colTemp = new Double[rows];
		
		r = new Double[rows];
		c = new Double[cols];
		rmeds = new Double[rows];
		cmeds = new Double[cols];
		m = 0.0;
	}
	
	public double rowMedian(int i) { 
		for(int j = 0; j < cols; j++) { 
			rowTemp[j] = matrix[i][j];
		}
		Arrays.sort(rowTemp);
		int idx = rowTemp.length/2;
		if(rowTemp.length % 2 == 0) { 
			return (rowTemp[idx] + rowTemp[idx+1]) / 2.0;
		} else { 
			return rowTemp[idx];
		}
	}
	
	public double colMedian(int j) { 
		for(int i = 0; i < cols; i++) { 
			colTemp[i] = matrix[i][j];
		}
		Arrays.sort(colTemp);
		int idx = colTemp.length/2;
		if(colTemp.length % 2 == 0) { 
			return (colTemp[idx] + colTemp[idx+1]) / 2.0;
		} else { 
			return colTemp[idx];
		}
	}
	
	public void polish(double tol) { 
		for(int i = 0; i < rows; i++) { r[i] = 0.0; }
		for(int i = 0; i < cols; i++) { c[i] = 0.0; }
		m = 0.0;
		while(!polishRows(tol) || !polishCols(tol)) { 
			// do nothing.
		}
	}
	
	private boolean polishRows(double tol) {
		boolean withinTol = true;
		for(int i = 0; i < rows; i++) { 
			rmeds[i] = rowMedian(i);
			r[i] += rmeds[i];
			withinTol = withinTol && Math.abs(rmeds[i]) <= tol;
			for(int j = 0; j < cols; j++) {
				matrix[i][j] -= rmeds[i];
			}
		}
		double madj = median(rmeds);
		for(int i = 0; i < rows; i++) { 
			r[i] -= madj;
		}
		m += madj;
		return withinTol;
	}
	
	private boolean polishCols(double tol) {
		boolean withinTol = true;
		for(int j = 0; j < cols; j++) { 
			cmeds[j] = colMedian(j);
			c[j] += cmeds[j];
			withinTol = withinTol && Math.abs(cmeds[j]) <= tol;
			for(int i = 0; i < rows; i++) {
				matrix[i][j] -= cmeds[j];
			}
		}
		double madj = median(cmeds);
		for(int j = 0; j < cols; j++) { 
			c[j] -= madj;
		}
		m += madj;
		return withinTol;
	}
	
	private static Double median(Double[] ac) { 
		Arrays.sort(ac);
		int i = ac.length/2;
		if(ac.length % 2 == 0) { 
			return (ac[i] + ac[i+1]) / 2.0;
		} else { 
			return ac[i];
		}
	}
	
	public Double[] getRowFactors() { return r; }
	public Double[] getColFactors() { return c; }
	public Double getGrandMean() { return m; }
}
