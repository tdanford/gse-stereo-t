/*
 * Author: tdanford
 * Date: May 20, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.grid;

import edu.mit.csail.cgs.datasets.general.StrandedRegion;

public interface GridColumn {
	public String name();
	public boolean containsRegion(StrandedRegion region);
}
