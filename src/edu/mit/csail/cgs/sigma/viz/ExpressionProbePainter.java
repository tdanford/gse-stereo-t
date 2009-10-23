package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.util.logging.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import edu.mit.csail.cgs.datasets.chipchip.ChipChipData;
import edu.mit.csail.cgs.datasets.chipchip.SQLData;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.general.StrandedRegion;
import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.OverlappingRegionFinder;
import edu.mit.csail.cgs.sigma.Parser;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.ewok.ExpressionProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.sigma.expression.models.Transcript;
import edu.mit.csail.cgs.sigma.expression.noise.NoiseModel;
import edu.mit.csail.cgs.sigma.expression.noise.PValueNoiseModel;
import edu.mit.csail.cgs.sigma.expression.noise.ThresholdNoiseModel;
import edu.mit.csail.cgs.utils.NotFoundException;
import edu.mit.csail.cgs.utils.Pair;
import edu.mit.csail.cgs.utils.probability.ExponentialDistribution;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;

public class ExpressionProbePainter extends RegionPainter implements Labeled {
	
	private String label;
	
	private Vector<Pair<Integer,Double>> watsonProbes, crickProbes;
	private Vector<ExprPoint> points;
	private Map<Point,ExprPoint> selections;
	private ExpressionProbeGenerator prober;
	private PaintableScale scale;
		
    private boolean displayOppositeChannel;
    private Color baseColor;
	
	public ExpressionProbePainter(ExpressionProbeGenerator gen, String lbl) { 
		scale = new PaintableScale(0.0, 1.0);
		label = lbl;
		prober = gen;
		
        baseColor = displayOppositeChannel ? Color.green: Color.red;
		
		watsonProbes = new Vector<Pair<Integer,Double>>();
		crickProbes = new Vector<Pair<Integer,Double>>();
		points = new Vector<ExprPoint>();
		
		selections = new HashMap<Point,ExprPoint>();
	}
	
	public String getLabel() { 
		return label; 
	}
	
	public void setLabel(String lbl) { 
		label = lbl;
		dispatchChangedEvent();
	}
    
	public void setRegion(Region r) { 
		super.setRegion(r);

		watsonProbes.clear();
		crickProbes.clear();
		points = null;
		scale.setScale(0.0, 1.0);
		
		Iterator<ExpressionProbe> probes = prober.execute(r);
		while(probes.hasNext()) { 
			ExpressionProbe probe = probes.next();
			int bp = probe.getLocation();
			double value = probe.mean();
			
			if(!Double.isNaN(value)) { 
				if(probe.getStrand() == '+') { 
					watsonProbes.add(new Pair<Integer,Double>(bp, value));
				} else { 
					crickProbes.add(new Pair<Integer,Double>(bp, value));				
				}
				
				scale.updateScale(value);
			}
		}

	}
    
	public void doLayout() {
        selections.clear();
		if(points == null) { points = new Vector<ExprPoint>(); }
		points.clear();
		
		for(Pair<Integer,Double> p : watsonProbes) { 
			ExprPoint ep = new ExprPoint(p.getFirst(), p.getLast(), '+');
			points.add(ep);
		}
		
		for(Pair<Integer,Double> p : crickProbes) { 
			ExprPoint ep = new ExprPoint(p.getFirst(), p.getLast(), '-');
			points.add(ep);
		}
		
	}

	public boolean isReadyToPaint() {
		return points != null;
	}

	public void paintRegion(Graphics2D g, int x1, int y1, int w, int h) {
		int h2 = h / 2;
		int rad = 3;
		int diam = rad * 2;

		Color lg = Color.cyan;
		Color dg = Color.orange;
		
		lg = new Color(lg.getRed(), lg.getGreen(), lg.getBlue(), 75);
		dg = new Color(dg.getRed(), dg.getGreen(), dg.getBlue(), 75);
		
		/*
		 * Draw the Baseline
		 */
		g.setColor(Color.black);
		g.drawLine(x1, y1+h, x1+w, y1+h);

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke((float)2.0));

		/*
		 * Draw the datapoints
		 */
		for(ExprPoint ep : points) {
			if(ep.strand == '+') { 
				g.setColor(lg);
			} else { 
				g.setColor(dg);
			}
			
			g.drawOval(x1+ep.x-rad, y1+ep.y-rad, diam, diam);
		}
		
		g.setStroke(oldStroke);
		
		/*
		 * Paint the hash marks...
		 */
		if(!displayOppositeChannel) { 
		    g.setColor(Color.black);
		    boolean flipper = true;
		    for(int value = 100; value <= scale.getMax(); value *= (flipper ? 5 : 2), flipper = !flipper) { 
		        int yoff = getYOffset((double)value);
		        String line = String.format("%d", value);
		        int uy = y1+h2 - yoff, ly = y1+h2 + yoff;

		        g.drawLine(x1, uy, x1+10, uy);
		        g.drawString(line, x1+12, uy+5);

		        g.drawLine(x1, ly, x1+10, ly);
		        g.drawString(line, x1+12, ly+5);
		    }
		}
		
		/*
		 * Draw any selections.
		 */
		
		g.setColor(Color.black);
		for(Point p : selections.keySet()) {
			ExprPoint ep = selections.get(p);
			g.drawLine(p.x, p.y, ep.x, ep.y);
			g.drawString(ep.getLabel(), p.x, p.y);
		}
		
		/*
		 * Draw the label in the upper-right hand corner.
		 */
		g.setColor(Color.black);
		Font oldFont = g.getFont();
		Font newFont = new Font("Arial", Font.BOLD, 24);
		g.setFont(newFont);
		FontMetrics fm = g.getFontMetrics();
		int lblHeight = fm.getAscent() + fm.getDescent();
		int lblWidth = fm.charsWidth(label.toCharArray(), 0, label.length());
		
		int padding = 5;
		int lblx = x1 + w - lblWidth - padding;
		int lbly = y1 + lblHeight + padding;
		
		g.drawString(label, lblx, lbly);
		
		g.setFont(oldFont);
		
	}
	
	public void clearSelections() { 
		selections.clear();
	}
	
	public void addSelection(Point p) { 
		ExprPoint ep = null;
		int minDist = -1;
		
		for(ExprPoint ex : points) {
			int sd = ex.screenDist(p);
			if(ep == null || sd < minDist) { 
				ep = ex;
				minDist = sd;
			}
		}
		
		if(ep != null) { 
			Set<Point> remove = new HashSet<Point>();
			for(Point rp : selections.keySet()) { 
				if(selections.get(rp).equals(ep)) { 
					remove.add(rp);
				}
			}
			
			selections.put(p, ep);
			
			for(Point rp : remove) { 
				selections.remove(rp);
			}
		}
	}
	
	public int getYOffset(double value) {
        double frac = scale.fractionalOffset(value);
		int pix = (int)Math.round(frac * (double)height);
        
        return pix;
	}
	
	private class ExprPoint { 
		public double value;
		public int bp;
		public char strand;
		public int x, y;
		
		public ExprPoint(int loc, double val, char str) { 
			bp = loc;
			value = val;
			strand = str;
			x = getXOffset(bp);
			y = height - getYOffset(value);
		}
		
		public int screenDist(Point ep) { 
			int dx = (x-ep.x);
			int dy = (y-ep.y);
			return (int)Math.sqrt((double)(dx*dx + dy*dy));
		}
		
		public String getLabel() { 
			return String.format("%.1f", value);
		}
		
		public int hashCode() { 
			int code = 17;
			code += bp; code *= 37;
			code += strand; code *= 37;
			code += y; code *= 37;
			return code;
		}
		
		public boolean equals(Object o) { 
			if(!(o instanceof ExprPoint)) { return false; }
			ExprPoint ep = (ExprPoint)o;
			return ep.bp == bp && ep.strand==strand && ep.y == y;
		}
	}
}
