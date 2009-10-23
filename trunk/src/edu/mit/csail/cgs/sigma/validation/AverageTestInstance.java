/*
 * Author: tdanford
 * Date: May 6, 2009
 */
package edu.mit.csail.cgs.sigma.validation;

import java.util.ArrayList;

import edu.mit.csail.cgs.utils.models.Model;

public class AverageTestInstance<Params extends Model> implements TestInstance<Params> {

	private ArrayList<TestInstance<Params>> tests;
	
	public AverageTestInstance() {
		tests = new ArrayList<TestInstance<Params>>();
	}
	
	public void addTestInstance(TestInstance<Params> t) { 
		tests.add(t);
	}

	public double evaluate(Params p) {
		double sum = 0.0;
		for(TestInstance<Params> ti : tests) { 
			sum += ti.evaluate(p);
		}
		sum /= (double)Math.max(tests.size(), 1);
		return sum;
	}
}
