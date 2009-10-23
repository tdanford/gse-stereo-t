/*
 * Author: tdanford
 * Date: Mar 19, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.assessment.differential;

import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;

public class AndDifferential extends DifferentialTest { 
	
	public DifferentialTest[] specs; 
	
	public AndDifferential(DifferentialTest... s) { 
		specs = s.clone();
	}

	public boolean isDifferent(DataSegment seg) {
		for(int i = 0; i < specs.length; i++) { 
			if(!specs[i].isDifferent(seg)) { 
				return false;
			}
		}
		return true;
	}
}