/*
 * Author: tdanford
 * Date: Mar 10, 2009
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.sharing;

import java.util.Collection;
import java.util.LinkedList;

public class DefaultSharingFactory implements ParameterSharingFactory {
	
	private LinkedList<ParameterSharing> sharingList;
	
	public DefaultSharingFactory() {
		sharingList = new LinkedList<ParameterSharing>();
	}
	
	public void addSharing(ParameterSharing ps) { 
		sharingList.addLast(ps);
	}

	public Collection<ParameterSharing> loadSharing(int channels) {
		return sharingList;
	}
}
