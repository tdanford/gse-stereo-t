/*
 * Author: tdanford
 * Date: Aug 30, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import edu.mit.csail.cgs.utils.models.Model;

public class NewVizPaintableProperties extends Model {

	public Boolean paintSegmentParams = true;
	public Boolean grayBackgroundSegments = true;
	public Boolean drawTranscripts = true;
	public Boolean showDifferentialSegments = false;
	public Boolean drawSegmentBoundaries = false;
	public Boolean vizSteinmetz = false;
	public Boolean vizSnyder = false;
	
	public NewVizPaintableProperties() {
	}
}
