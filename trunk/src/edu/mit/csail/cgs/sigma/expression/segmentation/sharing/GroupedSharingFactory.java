/*
 * Author: tdanford
 * Date: Feb 14, 2009
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.sharing;

import java.util.ArrayList;
import java.util.Collection;

import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationProperties;

public class GroupedSharingFactory implements ParameterSharingFactory {
	
	private Double separateWeight; 
	private int groupSize;
	
	public GroupedSharingFactory(SegmentationProperties props, int groups) { 
		SegmentationParameters params = new SegmentationParameters(props);
		double pshar = params.probShare; 
		double pnoshar = 1.0 - pshar;
		separateWeight = Math.log(pshar / (1.0-pnoshar));
		groupSize = groups;
	}
	
	public GroupedSharingFactory(int groups) {
		this(new SegmentationProperties(), groups);
	}
	
	public GroupedSharingFactory(Double sepWeight) { 
		separateWeight = sepWeight;
	}

	public Collection<ParameterSharing> loadSharing(int channels) {
		ArrayList<ParameterSharing> sharing = new ArrayList<ParameterSharing>();
		sharing.add(new ParameterSharing(separateWeight, channels, false));
	
		Integer[] groups = new Integer[channels];
		int group = 0;
		for(int i = 0; i < channels; i++) {
			if(i % groupSize == 0) { group += 1; }
			groups[i] = group;
		}
		sharing.add(new ParameterSharing(0.0, groups));
		
		return sharing;
	} 
}