/*
 * Author: tdanford
 * Date: May 26, 2009
 */
package edu.mit.csail.cgs.sigma.expression.normalization;

import cern.jet.random.Normal;
import cern.jet.random.engine.DRand;

public class UniformModel implements SignalModel {
	
	public Double baseline;
	
	public UniformModel() { 
		this(0.0);
	}
	
	public UniformModel(double b) {
		baseline = b;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.normalization.SignalModel#params()
	 */
	public Double[] params() { return new Double[] { baseline }; }
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.normalization.SignalModel#numParams()
	 */
	public int numParams() { return 1; }
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.normalization.SignalModel#setParams(double[])
	 */
	public void setParams(double[] p) { 
		baseline = p[0];
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.normalization.SignalModel#expectedX(java.lang.Double)
	 */
	public Double expectedX(Double s) { 
		return baseline;
	}
}
