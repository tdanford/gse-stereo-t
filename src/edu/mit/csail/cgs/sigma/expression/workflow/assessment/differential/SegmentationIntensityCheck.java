/*
 * Author: tdanford
 * Date: Sep 1, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.assessment.differential;

import java.io.*;
import java.util.*;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.litdata.*;
import edu.mit.csail.cgs.sigma.litdata.snyder.*;
import edu.mit.csail.cgs.sigma.litdata.steinmetz.*;

public class SegmentationIntensityCheck {

	private WorkflowProperties props; 
	private LitDataProperties litprops;
	private String key;
	
	public SegmentationIntensityCheck(String key) { 
		this.key = key;
		props = new WorkflowProperties();
		litprops = new LitDataProperties();
	}
}
