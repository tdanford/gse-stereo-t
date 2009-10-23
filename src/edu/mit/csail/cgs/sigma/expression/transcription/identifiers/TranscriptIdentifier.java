package edu.mit.csail.cgs.sigma.expression.transcription.identifiers;

import java.util.Collection;

import edu.mit.csail.cgs.sigma.expression.transcription.Cluster;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.TAFit;

public interface TranscriptIdentifier {
	public TAFit identify(Cluster c);
}
