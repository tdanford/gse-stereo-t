/*
 * Author: tdanford
 * Date: Mar 19, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.assessment.differential;

import edu.mit.csail.cgs.cgstools.slicer.StudentsTTest;
import edu.mit.csail.cgs.sigma.expression.workflow.models.DataSegment;

public class PairwiseDifferential extends DifferentialTest { 
	
	public Integer channel1, channel2;
	public Double pvalue;
	
	public PairwiseDifferential(int c1, int c2, double p) { 
		channel1 = c1; 
		channel2 = c2;
		pvalue = p;
	}

	public boolean isDifferent(DataSegment dseg) {
		if(dseg.dataLocations.length >= 5) {
			Double[] v1 = new Double[dseg.dataLocations.length];
			
			int c1 = channel1, c2 = channel2;
			
			for(int i = 0; i < v1.length; i++) { 
				double value1 = dseg.dataValues[c1][i];
				double value2 = dseg.dataValues[c2][i];
				v1[i] = value1 - value2;
			}
			
			//int degs = 2 * v1.length - 2;
			int degs = v1.length-1;
			
			StudentsTTest ttest = new StudentsTTest(degs);
			Double t = StudentsTTest.calculateT(v1, 0.0);
			Double p = ttest.pvalue(t);
			
			return p <= pvalue;
		} 
		return false;
	}
}

