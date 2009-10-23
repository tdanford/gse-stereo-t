/*
 * Author: tdanford
 * Date: May 6, 2009
 */
package edu.mit.csail.cgs.sigma.validation;

import edu.mit.csail.cgs.utils.models.Model;

public interface TestInstance<Params extends Model> {
	public double evaluate(Params p);
}
