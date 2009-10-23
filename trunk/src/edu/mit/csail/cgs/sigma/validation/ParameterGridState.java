/*
 * Author: tdanford
 * Date: May 5, 2009
 */
package edu.mit.csail.cgs.sigma.validation;

import edu.mit.csail.cgs.utils.models.Model;

public interface ParameterGridState { 
	public void setParameters(Model m);
	public boolean hasNext();
	public void next();
}