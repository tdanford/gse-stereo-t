/*
 * Author: tdanford
 * Date: Nov 19, 2008
 */
package edu.mit.csail.cgs.sigma.expression.segmentation;

import edu.mit.csail.cgs.sigma.validation.GridBounds;
import edu.mit.csail.cgs.sigma.validation.ParameterGrid;
import edu.mit.csail.cgs.sigma.validation.ParameterGridState;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.viz.eye.ModelPrefs;

import java.util.*;
import java.lang.reflect.*;

public class SegmentationParameters extends Model {
	
	public static void main(String[] args) { 
		SegmentationParameters params = new SegmentationParameters();
		ModelPrefs<SegmentationParameters> prefs = new ModelPrefs<SegmentationParameters>(params);
		params = prefs.displayAndWait();
		System.out.println(params.asJSON().toString());
	}
	
	public static void grid_example() { 
		MyGrid g = new MyGrid();
		SegmentationParameters p = new SegmentationParameters();
		ParameterGridState stateItr = g.createGridState();
		int i = 0;
		while(stateItr.hasNext()) { 
			stateItr.next();
			stateItr.setParameters(p);
			System.out.println(String.format("%d; %s", i, p.toString()));
			i++;
		}
	}

	public Double probSplit;
	public Double probShare;
	public Double probLine;
	public Double lineVarPenalty;
	public Double flatVarPenalty;
	public Double flatIntensityPenalty;
	public Integer minSegmentLength;
	public Boolean doHRMA;
	
	public SegmentationParameters() { 
		this(new SegmentationProperties());
	}
	
	public SegmentationParameters(SegmentationProperties props) { 
		probSplit = props.getProbSplit();
		probShare = props.getProbShare();
		minSegmentLength = props.getMinSegmentLength();
		probLine = props.getProbLine();
		lineVarPenalty = props.getLineVarPenalty();
		flatVarPenalty = props.getFlatVarPenalty();
		flatIntensityPenalty = props.getFlatIntensityPenalty();
		doHRMA = true;
	}
	
	public SegmentationParameters(Model m) { 
		this();
		setFromModel(m);
	}

	public Double scoreSegmentWidth(int width) {
		return Math.log(1.0-probSplit) * (double)width;
	}
}

class MyGrid extends ParameterGrid { 
	public Double[] probSplit = new Double[] { 0.1, 0.2, 0.3 };
	public Double[] probShare = new Double[] { 0.5, 0.6, 0.7 };
	public GridBounds flatIntensityPenalty = new GridBounds(0.0, 1.0, 0.1);
}




