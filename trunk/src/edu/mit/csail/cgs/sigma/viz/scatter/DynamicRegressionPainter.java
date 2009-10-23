package edu.mit.csail.cgs.sigma.viz.scatter;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import Jama.Matrix;

import edu.mit.csail.cgs.viz.paintable.*;
import edu.mit.csail.cgs.cgstools.oldregression.Datapoints;
import edu.mit.csail.cgs.cgstools.oldregression.MappedValuation;
import edu.mit.csail.cgs.cgstools.oldregression.PredictorSet;
import edu.mit.csail.cgs.cgstools.oldregression.Regression;
import edu.mit.csail.cgs.sigma.expression.regression.*;
import edu.mit.csail.cgs.utils.Pair;

public class DynamicRegressionPainter extends AbstractPaintable {
	
	public static void main(String[] args) { 
		new DynamicRegressionFrame();
	}
	
	public static class DynamicRegressionFrame extends JFrame {
		
		private DynamicRegressionPanel panel;
		
		public DynamicRegressionPainter getPainter() { return panel.getPainter(); } 
		
		public DynamicRegressionFrame(String title) { 
			super(title);
			Container c = (Container)getContentPane();
			c.setLayout(new BorderLayout());
			
			c.add(panel = new DynamicRegressionPanel(), BorderLayout.CENTER);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setVisible(true);
			pack();
		}

		public DynamicRegressionFrame() { 
			super("Dynamic Regression");
			Container c = (Container)getContentPane();
			c.setLayout(new BorderLayout());
			
			c.add(panel = new DynamicRegressionPanel(), BorderLayout.CENTER);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setVisible(true);
			pack();
		}
	}
	
	public static class DynamicRegressionPanel extends JPanel {
		
		private DynamicRegressionPainter painter;
		
		public DynamicRegressionPainter getPainter() { return painter; }
		
		public DynamicRegressionPanel() { 
			this(new DynamicRegressionPainter());
		}
		
		public DynamicRegressionPanel(DynamicRegressionPainter drp) { 
			super();
			painter = drp;
			setPreferredSize(new Dimension(300, 300));
			
			addMouseListener(new MouseAdapter() { 
				public void mousePressed(MouseEvent e) { 
					int w = getWidth(), h = getHeight();
					if(e.getButton() == MouseEvent.BUTTON1 && w > 0 && h > 0) { 
						double xf = (double)e.getX() / (double)w;
						double yf = (double)e.getY() / (double)h;
						double x = painter.xbounds[0] + xf*painter.xrange;
						double y = painter.xbounds[1] - yf*painter.xrange;
						painter.addDatapoint(x, y);
					}
				}
                
                public void mouseClicked(MouseEvent e) { 
                    if(e.getButton() == MouseEvent.BUTTON3) { 
                        painter.clear();
                    }
                }
			});
			
			painter.addPaintableChangedListener(new PaintableChangedListener() {
				public void paintableChanged(PaintableChangedEvent pce) {
					repaint();
				} 
			});
		}
		
		protected void paintComponent(Graphics g) { 
			super.paintComponent(g);
			int w = getWidth(), h = getHeight();
			painter.paintItem(g, 0, 0, w, h);
		}
	}
	
	private Vector<Pair<Double,Double>> pts;
	private Datapoints datapoints;
	private MappedValuation<Double> xVal, yVal;
	private boolean regressing;
	
	private double[] line;
	private double[] xbounds, ybounds;
	private double xrange, yrange;
	
	public DynamicRegressionPainter() { 
		pts = new Vector<Pair<Double,Double>>();
		line = new double[2];
		line[0] = 0.0; line[1] = 1.0;
		regressing = true;
		
		xbounds = new double[2];
		xbounds[0] = 0.0;
		xbounds[1] = 1.0;
		
		ybounds = new double[2];
		ybounds[0] = 0.0;
		ybounds[1] = 1.0;
		
		xrange = xbounds[1] - xbounds[0];
		yrange = ybounds[1] - ybounds[0];
		
		datapoints = new Datapoints();
		xVal = new MappedValuation<Double>("x");
		yVal = new MappedValuation<Double>("y");
	}
	
	public void enableRegressions() { 
		regressing=true;
		updateRegression();
		dispatchChangedEvent();
	}
	
	public void matchBounds() { 
		double lower = Math.min(xbounds[0], ybounds[0]);
		double upper = Math.max(xbounds[1], ybounds[1]); 
		xbounds[0] = ybounds[0] = lower;
		xbounds[1] = ybounds[1] = upper;
		dispatchChangedEvent();
	}
    
	public void disableRegressions() { 
		regressing=false;
		dispatchChangedEvent();
	}
    
    public void clear() { 
        datapoints.clear();
        xVal.clear();
        yVal.clear();
        pts.clear();
        line[0] = 0.0; line[1] = 1.0;
        xbounds[0] = 0.0;
        xbounds[1] = 100.0;
        ybounds[0] = 0.0;
        ybounds[1] = 100.0;
        xrange = xbounds[1] - xbounds[0];
        yrange = ybounds[1] - ybounds[0];
        
        dispatchChangedEvent();
    }
    
    public double getIntercept() { return line[0]; }
    public double getSlope() { return line[1]; }
	
	public void addDatapoint(double x, double y) { 
		int idx = pts.size();
		String ptName = String.format("#%d", idx);
		datapoints.addDatapoint(ptName);
		xVal.addValue(ptName, x);
		yVal.addValue(ptName, y);
		Pair<Double,Double> pt = new Pair<Double,Double>(x, y);
		
		synchronized(pts) { 
			pts.add(pt);
			
			if(pts.size()==2) { 
				Pair<Double,Double> p1 = pts.get(0), p2 = pts.get(1);
				double x1 = p1.getFirst(), y1 = p1.getLast();
				double x2 = p1.getFirst(), y2 = p1.getLast();
				xbounds[0] = ybounds[0] = Math.min(Math.min(x1, x2), Math.min(y1, y2));
				xbounds[1] = ybounds[1] = Math.max(Math.max(x1, x2), Math.max(y1, y2));				
			} else { 
				xbounds[0] = ybounds[0] = Math.min(Math.min(x, y),Math.min(xbounds[0], ybounds[0]));
				xbounds[1] = ybounds[1] = Math.max(Math.max(x, y),Math.max(xbounds[1], ybounds[1]));

				xrange = xbounds[1] - xbounds[0];
				yrange = ybounds[1] - ybounds[0];
			}
		}

		updateRegression();
		dispatchChangedEvent();
	}
	
	public void updateRegression() { 
		if(pts.size() >= 2 && regressing) {
			System.out.println("Building regression...");
			Regression reg = createRegression();

			Matrix betaHat = reg.calculateBetaHat();
	        //double s2 = reg.calculateS2(betaHat);
	        //double rms = s2 / (double)reg.getSize();
	        
	        double intercept = betaHat.get(0,0);
	        double slope = betaHat.get(1, 0);
	        
	        line[0] = intercept;
	        line[1] = slope;
	        System.out.println(String.format("\tb=%.3f, m=%.3f", intercept, slope));
		}
	}
	
    public Regression createRegression() {
    	PredictorSet preds = new PredictorSet();

    	preds.addConstantPredictor();
		preds.addQuantitativePredictor(xVal);
    	
		return new Regression(yVal, preds, datapoints);
    }
	
	public int valueToXPix(double val, int size) { 
		double frac = (val-xbounds[0]) / xrange;
		return (int)Math.round(frac * (double)size);
	}

	public int valueToYPix(double val, int size) { 
		double frac = (val-ybounds[0]) / yrange;
		return (int)Math.round(frac * (double)size);
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2 - x1, h = y2 - y1;
		int pointRadius = 3;
		int pointDiam = pointRadius*2;
		
		//Color redColor = new Color(255, 0, 0, 50);
		Color redColor = Color.red;
		
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.setColor(Color.white);
		g.fillRect(x1, y1, w, h);
		
		g.setColor(Color.black);
		g.drawRect(x1, y1, w, h);
		
		Stroke oldStroke = g2.getStroke();
		g2.setStroke(new BasicStroke((float)3.0));

		synchronized(pts) { 
			g.setColor(redColor);
			for(Pair<Double,Double> pt : pts) { 
				double x = pt.getFirst(), y = pt.getLast();
				int px = valueToXPix(x, w), py = valueToYPix(y, h);

				g.drawOval(x1+px-pointRadius, y2-py-pointRadius, pointDiam, pointDiam);
			}
		}
		
		if(regressing) { 
			double b = line[0], m = line[1];
			double ly1 = m*xbounds[0] + b, ly2 = m*xbounds[1]+b;
			int lpy1 = y2- valueToYPix(ly1, h), lpy2 = y2 - valueToYPix(ly2, h);

			g.setColor(Color.blue);
			//g2.setStroke(oldStroke);
			g.drawLine(x1, lpy1, x2, lpy2);
		}
		
		g2.setStroke(oldStroke);
	}

}
