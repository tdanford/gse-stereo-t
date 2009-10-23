/*
 * Author: tdanford
 * Date: Feb 14, 2009
 */
package edu.mit.csail.cgs.sigma.expression.segmentation.sharing;

import java.util.*;
import java.io.*;

public interface ParameterSharingFactory {
	public Collection<ParameterSharing> loadSharing(int channels);
}

