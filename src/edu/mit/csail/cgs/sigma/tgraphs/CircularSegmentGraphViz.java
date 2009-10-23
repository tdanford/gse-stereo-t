package edu.mit.csail.cgs.sigma.tgraphs;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
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
import edu.mit.csail.cgs.sigma.expression.transcription.viz.AbstractRegionPaintable;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;

import edu.mit.csail.cgs.viz.paintable.*;

public class CircularSegmentGraphViz extends JPanel {
	
	public static void main(String[] args) { 
		WorkflowProperties props = new WorkflowProperties();
		String strain = "s288c";
		String expt = "matalpha";
		Genome genome = props.getSigmaProperties().getGenome(strain);
		String chrom = "1";
		
		try {
			WholeGenome whole = WholeGenome.loadWholeGenome(props, strain);
			SegmentGraph graph = new SegmentGraph(props, strain, expt);
			
			//SegmentGraphViz viz = new SegmentGraphViz(graph);
			//PaintableFrame pf = new PaintableFrame(String.format("%s:%s", strain, chromRegion.regionString()), viz);
			
			CircularSegmentGraphViz viz = new CircularSegmentGraphViz(graph, genome, chrom);
			CircleVizFrame frame = new CircleVizFrame(viz);
			frame.showVizFrame();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static SetTools<String> sets = new SetTools<String>(); 
	
	private SegmentGraph graph;
	private Region chrom;
	
	private RadialLayout radial;
	private CircularLayout segAnnulus, geneAnnulus;
	private Map<String,Point> geneLayout, segLayout;

	public CircularSegmentGraphViz(SegmentGraph g, Genome gnm, String c) { 
		graph = g;
		chrom = new Region(gnm, c, 0, gnm.getChromLength(c));
		geneLayout = new TreeMap<String,Point>();
		segLayout = new TreeMap<String,Point>();
		
		radial = new RadialLayout(chrom.getStart(), chrom.getEnd());
		segAnnulus = geneAnnulus = null;
	}
	
	public void layoutNodes() {
		if(!geneLayout.isEmpty() || !segLayout.isEmpty()) { return; }
		
		int w = getWidth(), h = getHeight();
		int radius = Math.min(w, h) / 2;
		int cx = w < h ? radius : (radius + (w-h)/2);
		int cy = h < w ? radius : (radius + (h-w)/2);
		
		geneLayout.clear(); segLayout.clear();
		
		int outerRadius = (4 * radius) / 5;
		int innerRadius = (3 * radius) / 5;
		
		geneAnnulus = new CircularLayout(radial, innerRadius);
		segAnnulus = new CircularLayout(radial, outerRadius);
		
		for(String geneID : graph.geneNodes(chrom)) {  
			StrandedRegion r = graph.region(geneID);
			int middle = (r.getStart() + r.getEnd()) / 2;
			Point p = geneAnnulus.findPoint(middle);
			geneLayout.put(geneID, new Point(cx + p.x, cy + p.y));
		}

		for(String segID : graph.segmentNodes(chrom)) {  
			StrandedRegion r = graph.region(segID);
			int middle = (r.getStart() + r.getEnd()) / 2;
			Point p = segAnnulus.findPoint(middle);
			segLayout.put(segID, new Point(cx + p.x, cy + p.y));
		}		
	}
	
	protected void paintComponent(Graphics g) { 
		super.paintComponent(g);
		
		int x1 = 0, x2 = getWidth(), y1 = 0, y2 = getHeight();
		
		int radius = 8;
		int diam = radius * 2;
		
		int w = x2-x1, h = y2-y1;
		
		int h5 = h/4;
		int wsegY = y1 + h5;
		int csegY = y1 + h5*2; 
		int wgeneY = y1 + h5*3;
		int cgeneY = wgeneY;
		
		layoutNodes();
		
		Graphics2D g2 = (Graphics2D)g;
		Stroke oldStroke = g2.getStroke();
		//g2.setStroke(new BasicStroke((float)2.0));
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		FontMetrics fm = g2.getFontMetrics();
		int textHeight = fm.getAscent() + fm.getDescent();
		
		for(String id : segLayout.keySet()) {
			Point p1 = segLayout.get(id);

			g.setColor(Color.red);
			Set<String> overlapping = graph.overlapping(id);
			for(String id2 : sets.intersection(overlapping, segLayout.keySet())) {  
				Point p2 = segLayout.get(id2);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}

			g.setColor(Color.blue);
			Set<String> convergent = graph.convergent(id);
			for(String id2 : sets.intersection(convergent, segLayout.keySet())) {  
				Point p2 = segLayout.get(id2);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
			
			g.setColor(Color.green);
			Set<String> divergent = graph.divergent(id);
			for(String id2 : sets.intersection(divergent, segLayout.keySet())) { 
				Point p2 = segLayout.get(id2);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}

			g.setColor(Color.gray);
			Set<String> tandem = graph.tandem(id);
			for(String id2 : sets.intersection(tandem, segLayout.keySet())) { 
				Point p2 = segLayout.get(id2);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}

			g.setColor(Color.lightGray);
			Set<String> sense = graph.sense(id);
			for(String id2 : sets.intersection(sense, geneLayout.keySet())) { 
				Point p2 = geneLayout.get(id2);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
			
			g.setColor(Color.orange);
			Set<String> antisense = graph.antisense(id);
			for(String id2 : sets.intersection(antisense, geneLayout.keySet())) { 
				Point p2 = geneLayout.get(id2);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
		}
		
		g.setColor(Color.black);
		
		for(String id : segLayout.keySet()) { 
			Point p = segLayout.get(id);
			
			int diff = graph.differential(id);
			if(diff != 0) { 
				g.setColor(diff == 1 ? Color.red : Color.green);
				g.fillRect(p.x- radius, p.y-radius, diam, diam);
			}
			
			g.setColor(Color.black);
			g.drawRect(p.x- radius, p.y-radius, diam, diam);
			
			if(graph.isUnique(id)) { 
				g.drawLine(p.x-radius, p.y-radius, p.x+radius, p.y+radius);
				g.drawLine(p.x-radius, p.y+radius, p.x+radius, p.y-radius);
			}
		}
		for(String id : geneLayout.keySet()) { 
			Point p = geneLayout.get(id);
			g.drawOval(p.x- radius, p.y-radius, diam, diam);
			int textWidth = fm.charsWidth(id.toCharArray(), 0, id.length());
			g.drawString(id, p.x - textWidth/2, p.y + radius + fm.getAscent() + 2);
		}
		
		g2.setStroke(oldStroke);
	}

}

class CircularLayout { 
	
	private int radius;
	private RadialLayout layout;
	
	public CircularLayout(RadialLayout r, int rd) { 
		radius = rd;
		layout = r;
	}
	
	public void setRadius(int r) { radius = r; }
	public int getRadius() { return radius; }
	
	public Point findPoint(int value) { 
		double angle = layout.valueToAngle(value);
		return findPoint(angle);
	}
	
	public Point findPoint(double angle) { 
		int y = (int)Math.round((double)radius * Math.sin(angle));
		int x = (int)Math.round((double)radius * Math.cos(angle));
		return new Point(x, y);		
	}
	
	public double findAngle(Point p) { 
		int dx = p.x, dy = p.y;
		double rot = 0.0;
		if(dx == 0 && dy == 0) { 
			rot = 0.0; 
		} else { 
			if(dy < 0) { 
				rot = Math.acos((double)dx / (double)radius); 
			} else { 
				rot = 2.0 * Math.PI - Math.acos((double)dx / (double)radius);
			}
		}
		return rot;
	}
	
	public int findValue(Point p) { 
		return layout.angleToValue(findAngle(p));
	}
}

class CircleVizFrame extends JFrame {
	
	private CircularSegmentGraphViz viz;
	
	public CircleVizFrame(CircularSegmentGraphViz v) {
		super("Circle View");
		viz = v;
		
		Container c = (Container)getContentPane();
		c.setLayout(new BorderLayout());
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		viz.setPreferredSize(new Dimension(400, 400));
		
		c.add(viz, BorderLayout.CENTER);
	}
	
	public void showVizFrame() { 
		SwingUtilities.invokeLater(new Runnable() { 
			public void run() { 
				setVisible(true);
				pack();
			}
		});
	}
}

class RadialLayout { 
	
	private int min, max;
	
	public RadialLayout(int m1, int m2) { 
		min = m1; max = m2;
	}
	
	public double valueToAngle(int v) { 
		double f = (double)(v - min) / (double)(max - min);
		return f * Math.PI * 2.0;
	}
	
	public int angleToValue(double rad) { 
		double af = rad / (Math.PI * 2.0);
		return min + (int)Math.round(af * (double)(max - min));
	}
}
