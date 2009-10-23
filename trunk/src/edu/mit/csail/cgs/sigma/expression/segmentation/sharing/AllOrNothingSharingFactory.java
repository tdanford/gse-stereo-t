/*
 * Author: tdanford
 * Date: Feb 14, 2009
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.sharing;

import java.util.ArrayList;
import java.util.Collection;

import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationProperties;

public class AllOrNothingSharingFactory implements ParameterSharingFactory {
	
	private Double separateWeight;
	
	public AllOrNothingSharingFactory() { 
		this(new SegmentationProperties());
	}
	
	public AllOrNothingSharingFactory(SegmentationProperties props) { 
		SegmentationParameters params = new SegmentationParameters(props);
		double pshar = params.probShare; 
		double pnoshar = 1.0 - pshar;
		separateWeight = Math.log(pshar / (1.0-pnoshar));
	}
	
	public AllOrNothingSharingFactory(Double sepWeight) { 
		separateWeight = sepWeight;
	}

	public Collection<ParameterSharing> loadSharing(int channels) {
		ArrayList<ParameterSharing> sharing = new ArrayList<ParameterSharing>();
		sharing.add(new ParameterSharing(channels, true));
		sharing.add(new ParameterSharing(separateWeight, channels, false));
		return sharing;
	} 
	
}