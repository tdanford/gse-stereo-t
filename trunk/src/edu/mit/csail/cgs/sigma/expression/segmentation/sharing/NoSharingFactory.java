/*
 * Author: tdanford
 * Date: Feb 14, 2009
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.sharing;

import java.util.ArrayList;
import java.util.Collection;

import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationProperties;

public class NoSharingFactory implements ParameterSharingFactory {
	
	private Double separateWeight;
	
	public NoSharingFactory() { 
		this(new SegmentationProperties());
	}
	
	public NoSharingFactory(SegmentationProperties props) { 
		SegmentationParameters params = new SegmentationParameters(props);
		separateWeight = 0.0;
	}
	
	public NoSharingFactory(Double sepWeight) { 
		separateWeight = sepWeight;
	}

	public Collection<ParameterSharing> loadSharing(int channels) {
		ArrayList<ParameterSharing> sharing = new ArrayList<ParameterSharing>();
		sharing.add(new ParameterSharing(separateWeight, channels, false));
		return sharing;
	} 
	
}