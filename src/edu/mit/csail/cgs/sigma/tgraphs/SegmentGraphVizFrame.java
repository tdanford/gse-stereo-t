/*
 * Author: tdanford
 * Date: Apr 14, 2009
 */
package edu.mit.csail.cgs.sigma.tgraphs;

import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.utils.SetTools;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.graphs.*;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;

import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.transcription.viz.GenericRegionFrame;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;

import java.awt.*;

import edu.mit.csail.cgs.viz.paintable.*;
import javax.swing.*;

public class SegmentGraphVizFrame extends GenericRegionFrame {
	
	private SegmentGraph graph;
	
	public SegmentGraphVizFrame(SegmentGraph g, Region region) {
		super(new SegmentGraphViz(g, region));
		graph = g;
	}	
}
