/*
 * Author: tdanford
 * Date: Jul 17, 2008
 */
package edu.mit.csail.cgs.sigma.blots;

import java.awt.event.ActionEvent;

public class TranscriptAdjustedEvent extends ActionEvent {
	
	private int index;

	public TranscriptAdjustedEvent(Object source, int tidx) {
		super(source, ActionEvent.ACTION_PERFORMED, "Transcript Adjusted");
		index = tidx;
	}
	
	public int getTranscriptIndex() { return index; }

}
