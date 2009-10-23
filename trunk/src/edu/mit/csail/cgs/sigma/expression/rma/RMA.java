/*
 * Author: tdanford
 * Date: May 7, 2009
 */
package edu.mit.csail.cgs.sigma.expression.rma;

public class RMA {

	/**
	 * Quick utility function -- computes the log of an (unnormalized) gaussian.
	 * Useful for computing the unnormalized posteriors that we need for slice-sampling
	 * the conditionals.    
	 * 
	 * @param x
	 * @param mu
	 * @param var
	 * @return
	 */
	public static double loguNormal(double x, double mu, double var) { 
		double diff = x - mu; 
		return -(diff * diff) / (2.0 * var);
	}

}
