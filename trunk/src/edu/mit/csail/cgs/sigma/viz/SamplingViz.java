/*
 * Author: tdanford
 * Date: Jun 1, 2009
 */
package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.io.*;
import java.awt.*;

import edu.mit.csail.cgs.cgstools.slicer.SliceSampler;
import edu.mit.csail.cgs.cgstools.slicer.UnnormalizedGaussian;
import edu.mit.csail.cgs.viz.paintable.*;

public class SamplingViz extends AbstractPaintable {
	
	public static void main(String[] args) { 
		double k = 2.0;
		SamplingViz v = new SamplingViz(-1, 0.01);
		v.setFixedValue(0.0);
		
		PaintableFrame pf = new PaintableFrame("sampling", v);
		
		UnnormalizedGaussian g = new UnnormalizedGaussian(0.0, k);
		SliceSampler sampler = new SliceSampler(g, 1.0, 0.0);
		Random rand = new Random();
		
		for(int i = 0; i < 10000; i++) { 
			//double x = sampler.nextX();
			double x = rand.nextGaussian();
			v.addSample(x);
			pf.repaint();
			System.out.println(String.format("%d: %f", i, x));
		}
		
		pf.repaint();
	}
	
	private LinkedList<Double> samples;
	private Double fixed;
	private int maxSize;
	private double kernel;
	
	public SamplingViz(int max, double k) { 
		maxSize = max;
		kernel = k;
		fixed = null;
		samples = new LinkedList<Double>();
	}
	
	public SamplingViz(double k) { 
		this(-1, k);
	}
	
	public void setFixedValue(Double v) { 
		fixed = v;
	}
	
	public Double max() { 
		Double m = fixed;
		for(Double v : samples) {
			 m = m == null ? v : Math.max(m, v);
		}
		return m;
	}
	
	public Double min() { 
		Double m = fixed;
		for(Double v : samples) {
			 m = m == null ? v : Math.min(m, v);
		}
		return m;
	}
	
	public Double mean() { 
		double mean = 0.0;
		for(Double v : samples) { 
			mean += v;
		}
		mean /= (double)Math.max(1, samples.size());
		return mean;
	}
	
	public Double smoothedValue(double v) { 
		double sum = 0.0;
		for(double s : samples) { 
			sum += normal(v, s, kernel);
		}
		return sum;
	}
	
	public static double normal(double x, double mean, double var) { 
		double c = 1.0 / Math.sqrt(2.0 * Math.PI * var);
		double diff = x - mean;
		double expt = -(diff * diff) / (2.0 * var);
		return c * Math.exp(expt);
	}

	public void addSample(double v) {
		synchronized(samples) { 
			samples.addLast(v);
			if(maxSize != -1) { 
				while(samples.size() > maxSize) { 
					samples.removeFirst();
				}
			}
		}
		dispatchChangedEvent();
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2-x1, h = y2-y1;
		
		g.setColor(Color.white);
		g.fillRect(x1, y1, w, h);

		synchronized(samples) { 
			double min = min() - kernel, max = max() + kernel;
			
			double[] fs = new double[w];
			for(int i = 0; i < fs.length; i++) { 
				fs[i] = 0.0;
			}

			double pwidth = (double)(max-min) / (double)(w+1);
			double fmin = 0.0, fmax = 1.e-6;

			for(int i = 0; i < fs.length; i++) { 
				double v = min + (double)(i+1) * pwidth;
				fs[i] = smoothedValue(v);
				fmax = Math.max(fmax, fs[i]);
			}

			PaintableScale xScale = new PaintableScale(min, max);
			PaintableScale yScale = new PaintableScale(fmin, fmax);

			g.setColor(Color.red);

			int px = -1, py = -1;
			if(samples.size() > 0) { 
				for(int i = 0; i < fs.length; i++) { 
					int x = x1 + i + 1; 
					double yf = yScale.fractionalOffset(fs[i]);
					int y = y2 - (int)Math.round(yf * (double)h);
					if(px != -1) { 
						g.drawLine(px, py, x, y);
					}
					px = x; py = y;
				}
			}

			int radius = 5;
			int diam = radius *2;

			for(Double sample : samples) { 
				double xf = xScale.fractionalOffset(sample);
				int x = x1 + (int)Math.round(xf * (double)w);
				int y = y2 - radius;
				g.drawOval(x-radius, y-radius, diam, diam);
			}
			
			double mean = mean();
			double meanf = xScale.fractionalOffset(mean);
			int mx = x1 + (int)Math.round(meanf * (double)w);
			g.drawLine(mx, y1, mx, y2);

			if(fixed != null) { 
				double fixf = xScale.fractionalOffset(fixed);
				int fx = x1 + (int)Math.round(fixf * (double)w);
				g.setColor(Color.black);
				g.drawLine(fx, y1, fx, y2);
			}
		}
	}
}
