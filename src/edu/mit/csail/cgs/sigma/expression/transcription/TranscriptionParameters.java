/*
 * Author: tdanford
 * Date: Nov 19, 2008
 */
package edu.mit.csail.cgs.sigma.expression.transcription;

import edu.mit.csail.cgs.sigma.validation.GridBounds;
import edu.mit.csail.cgs.sigma.validation.ParameterGrid;
import edu.mit.csail.cgs.sigma.validation.ParameterGridState;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.viz.eye.ModelPrefs;

import java.util.*;
import java.lang.reflect.*;

public class TranscriptionParameters extends Model {

	public Boolean filterRedundancies, filterBreaks;
	public Integer maxCallsPerCluster, maxOverlap;
	public Double extraTranscripts, overlapPenalty, meanIntensity;
	
	public TranscriptionParameters() { 
		this(new TranscriptionProperties());
	}
	
	public TranscriptionParameters(TranscriptionProperties props) {
		filterRedundancies = true;
		filterBreaks = props.isFilteringBreaks();
		maxCallsPerCluster = props.getMaxTranscripts();
		extraTranscripts = props.getExtraTranscripts();
		overlapPenalty = props.getOverlapPenalty();
		meanIntensity = props.getAverageIntensity();
		maxOverlap = props.getMaxOverlap();
	}
}

