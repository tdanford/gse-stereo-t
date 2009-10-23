/*
 * Author: tdanford
 * Date: May 28, 2009
 */
package edu.mit.csail.cgs.sigma.expression.normalization;

public class ZeroModel implements SignalModel {
	
	public ZeroModel() { 
	}

	public Double expectedX(Double s) {
		return 0.0;
	}

	public int numParams() {
		return 0;
	}

	public Double[] params() {
		return new Double[] { };
	}

	public void setParams(double[] p) {
	}

}
