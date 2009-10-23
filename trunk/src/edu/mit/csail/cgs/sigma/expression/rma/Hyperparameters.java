/*
 * Author: tdanford
 * Date: May 12, 2009
 */
package edu.mit.csail.cgs.sigma.expression.rma;

import edu.mit.csail.cgs.cgstools.singlevarcalculus.FunctionModel;
import edu.mit.csail.cgs.utils.models.Model;

public interface Hyperparameters<T extends Model> {

	public String[] names();
	public Double[] values();
	public int size();
	
	public void estimate(T values);
	public FunctionModel likelihood();
}
