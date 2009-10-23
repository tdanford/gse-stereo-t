package edu.mit.csail.cgs.sigma.expression.workflow.assessment;

import java.util.*;
import java.awt.Color;
import java.io.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.*;
import edu.mit.csail.cgs.sigma.IteratorCacher;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.sigma.genes.GeneNameAssociation;
import edu.mit.csail.cgs.sigma.motifs.MotifProperties;
import edu.mit.csail.cgs.sigma.motifs.TFList;
import edu.mit.csail.cgs.utils.SetTools;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.utils.models.data.*;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.eye.ModelPaintableWrapper;
import edu.mit.csail.cgs.viz.eye.ModelScatter;
import edu.mit.csail.cgs.viz.eye.OverlayModelPaintable;
import edu.mit.csail.cgs.viz.paintable.HorizontalScalePainter;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;
import edu.mit.csail.cgs.viz.paintable.VerticalScalePainter;
import edu.mit.csail.cgs.sigma.expression.NewExpressionProperties;

/**
 * Calculates a total-probes R^2 (correlation) for probes from a single array, 
 * or from a set of arrays. 
 * 
 * @author tdanford
 */
public class ProbeR2 {

	public static void main(String[] args) { 
		WorkflowProperties ps = new WorkflowProperties();
		try {
			WholeGenome genome = WholeGenome.loadWholeGenome(ps, "s288c");
			genome.loadIterators();
			Iterator<DataSegment> segs = 
				new SerialIterator<DataSegment>(
						genome.getWatsonSegments(), genome.getCrickSegments());
			Iterator<DPoint> pts = 
				new ExpanderIterator<DataSegment,DPoint>(
						new DPointExpander(
							new Integer[] { 2, 3 }, 
							new Integer[] { 4, 5 }), 
						segs);
			
			DataFrame<DPoint> frame = new DataFrame<DPoint>(DPoint.class, pts);
			DataRegression<DPoint> reg = new DataRegression<DPoint>(frame, "y ~ x - 1");
			
			reg.calculate();
			double r2 = reg.getR2();
			System.out.println(String.format("R2: %.3f", r2));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static class DPoint extends Model { 
		public Double y, x;
		
		public DPoint() {}
		
		public DPoint(double xx, double yy) { 
			x = xx; y = yy;
		}
	}
	
	public static class DPointExpander implements Expander<DataSegment,DPoint> {
		
		private Integer[] xchs, ychs;; 
		
		public DPointExpander(Integer[]... chs) {  
			xchs = new Integer[chs.length];
			ychs = new Integer[chs.length];
			for(int i = 0; i < chs.length; i++) { 
				xchs[i] = chs[i][0];
				ychs[i] = chs[i][1];
			}
		}
		
		public Iterator<DPoint> execute(DataSegment s) { 
			LinkedList<DPoint> pts = new LinkedList<DPoint>();
			for(int i = 0; i < s.dataLocations.length; i++) {
				for(int k = 0; k < xchs.length; k++) { 
					double x = s.dataValues[xchs[k]][i];
					double y = s.dataValues[ychs[k]][i];
					pts.add(new DPoint(x, y));
				}
			}
			return pts.iterator();
		}
	}
}
