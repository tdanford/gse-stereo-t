/*
 * Author: tdanford
 * Date: May 26, 2009
 */
package edu.mit.csail.cgs.sigma.expression.normalization;

public interface SignalModel {

	public Double[] params();
	public int numParams();
	public void setParams(double[] p);

	public Double expectedX(Double s);
}