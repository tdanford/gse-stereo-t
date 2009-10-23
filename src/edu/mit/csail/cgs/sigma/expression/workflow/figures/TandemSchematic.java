/*
 * Author: tdanford
 * Date: May 5, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.figures;

import java.awt.*;
import java.util.*;

import edu.mit.csail.cgs.viz.paintable.*;

public class TandemSchematic extends TwoArrowSchematic {
	
	public static void main(String[] args) { 
		Paintable p = new TandemSchematic(0.5, 0.5, 0.25);
		PaintableFrame pf = new PaintableFrame("Schematic", p);
	}
	
	public TandemSchematic(double d, double e, double f) {
		super(d, e, f, true, true);
	}
	
}
