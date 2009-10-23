/*
 * Author: tdanford
 * Date: May 26, 2009
 */
package edu.mit.csail.cgs.sigma.expression.normalization;

import cern.jet.random.Normal;
import cern.jet.random.engine.DRand;

public class BolstadModel implements SignalModel {
	
	public Double alpha, mu, sigma;
	private Normal normal;
	
	public BolstadModel() { 
		this(1.0, 0.0, 1.0);
	}
	
	public BolstadModel(double a, double m, double s) {
		setYParams(a);
		setXParams(m, s);
	}
	
	public void setYParams(double a) { 
		alpha = a;
	}
	
	public void setXParams(double m, double s) { 
		mu = m;
		sigma = s;
		normal = new Normal(mu, sigma, new DRand());		
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.normalization.SignalModel#params()
	 */
	public Double[] params() { return new Double[] { mu, sigma, alpha }; }
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.normalization.SignalModel#numParams()
	 */
	public int numParams() { return 3; }
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.normalization.SignalModel#setParams(double[])
	 */
	public void setParams(double[] p) { 
		setXParams(p[0], p[1]); 
		setYParams(p[2]);
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.normalization.SignalModel#expectedX(java.lang.Double)
	 */
	public Double expectedX(Double s) { 
		double a = s - mu - (sigma*sigma*alpha);
		double b = sigma;
		
		double ab = a/b;
		double sab = (s-a)/b;
		//double numer = normal.pdf(ab) - normal.pdf(sab);
		//double denom = normal.cdf(ab) + normal.cdf(sab) - 1;
		
		double numer = normal.pdf(ab);
		double denom = normal.cdf(ab);
		
		double ratio = numer / denom;
		return a + b * ratio;
	}
}
