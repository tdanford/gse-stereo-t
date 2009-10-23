/*
 * Author: tdanford
 * Date: May 28, 2009
 */
package edu.mit.csail.cgs.sigma.expression.normalization;

public class IdentityModel implements SignalModel {
	
	public IdentityModel() { 
	}

	public Double expectedX(Double s) {
		return s;
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
