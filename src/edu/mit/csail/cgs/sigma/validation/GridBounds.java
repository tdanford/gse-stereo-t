/*
 * Author: tdanford
 * Date: May 5, 2009
 */
package edu.mit.csail.cgs.sigma.validation;

public class GridBounds { 
	private Double min, max, step;
	
	public GridBounds(double min, double max, double step) { 
		this.min = min; 
		this.max = max;
		this.step = step;
	}
	
	public Double getMin() { return min; }
	public Double getMax() { return max; }
	public Double getStep() { return step; }
	
	public Double[] createArray() { 
		double range = max-min;
		int steps = (int)Math.floor(range/step) + 1;
		Double[] array = new Double[steps];
		array[0] = min;
		for(int i = 1; i < array.length; i++) { 
			array[i] = min + (double)i * step;
		}
		return array;
	}
}
