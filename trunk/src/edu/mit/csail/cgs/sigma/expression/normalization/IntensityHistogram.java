/*
 * Author: tdanford
 * Date: May 27, 2009
 */
package edu.mit.csail.cgs.sigma.expression.normalization;

import java.awt.*;
import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.IteratorCacher;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowDataLoader;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowIndexing;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;
import edu.mit.csail.cgs.utils.iterators.SerialIterator;
import edu.mit.csail.cgs.viz.paintable.AbstractPaintable;
import edu.mit.csail.cgs.viz.paintable.HorizontalScalePainter;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;
import edu.mit.csail.cgs.viz.paintable.VerticalScalePainter;

public class IntensityHistogram extends AbstractPaintable {
	
	public static void main(String[] args) { 
		String key = args.length > 0 ? args[0] : "s288c";
		String extension = args.length > 1 ? args[1] : "corrected";
		boolean showBackground = false;
		
		WorkflowProperties props = new WorkflowProperties();
		WorkflowIndexing indexing = props.getIndexing(key);
		
		String[] names = new String[indexing.getNumChannels()];
		for(int i = 0; i < names.length; i++) { 
			names[i] = indexing.descriptor(i);
		}
		
		File plus = new File(props.getDirectory(), String.format("%s_plus.%s", key, extension));
		File minus = new File(props.getDirectory(), String.format("%s_negative.%s", key, extension));
		
		try {
			System.out.println(String.format("Loading:\n\t%s\n\t%s", plus.getName(), minus.getName()));
		
			WorkflowDataLoader plusLoader = new WorkflowDataLoader(plus);
			WorkflowDataLoader minusLoader = new WorkflowDataLoader(plus);
			
			SerialIterator<ProbeLine> itr = new SerialIterator<ProbeLine>(plusLoader, minusLoader);
			IteratorCacher<ProbeLine> probes = new IteratorCacher<ProbeLine>(itr);
			
			BackgroundEstimation estim = new BolstadBackgroundEstimation();

			//IntensityHistogram his = new IntensityHistogram(-15.0, 15.0);
			IntensityHistogram his = new IntensityHistogram();
			
			PaintableFrame pf = new PaintableFrame(key, his);
			
			his.addProbeLines(names, probes.iterator(), true);
			
			for(int i = 0; i < names.length; i ++) {
				if(i % 2 == 0) { 
					String nm = String.format("%s/%s", names[i], names[i+1]);
					//his.addProbeDiffs(nm, probes.iterator(), i, i + 1);
				}
				
				if(showBackground) { 
					SignalModel model = estim.estimateModel(probes.iterator(), i);
					his.addSignalModel(names[i], model);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static Color[] staticColors = new Color[] { 
		Color.red, Color.blue, Color.green, Color.orange, Color.cyan, 
		Color.pink, Color.black, Color.darkGray, Color.gray, Color.magenta,
		Color.yellow, 
	};

	private boolean adaptBounds;
	private Double min, max;
	private ArrayList<Double[]> values;
	private ArrayList<String> names;
	
	private Map<String,SignalModel> models;
	
	public IntensityHistogram() {
		this(null, null);
		adaptBounds = true;
	}
	
	public IntensityHistogram(Double mn, Double mx) { 
		min = mn; max = mx;
		adaptBounds = false;
		values = new ArrayList<Double[]>();
		names = new ArrayList<String>();	
		models = new TreeMap<String,SignalModel>();
	}
	
	public void addSignalModel(String name, SignalModel m) { 
		models.put(name, m);
		dispatchChangedEvent();
	}
	
	public void addProbeLines(String[] ns, Iterator<ProbeLine> ps, boolean correct) { 
		ArrayList<ProbeLine> probes = new ArrayList<ProbeLine>();
		while(ps.hasNext()) { 
			probes.add(ps.next());
		}
		
		System.out.println(String.format("Loaded %d probes.", probes.size()));
		
		ProbeLine fst = probes.get(0);
		int chs = fst.values.length;
		
		for(int i = 0; i < ns.length; i++) { 
			LinkedList<Double> valueList = new LinkedList<Double>();
			int corrected = 0;
			
			for(int j = 0; j < probes.size(); j++) { 
				Double value = probes.get(j).values[i];
				if(correct && value == null && i % 2 == 0 && i < ns.length-1) { 
					value = probes.get(j).values[i+1];
					corrected += 1;
				}
				
				if(value != null) {
					value = Math.exp(value);
					valueList.add(value);
				}
			}

			addValues(ns[i], valueList);
			System.out.println(String.format("\t# Corrected: %d", corrected));
		}
	}
	
	public void addProbeDiffs(String nm, Iterator<ProbeLine> ps, int ch1, int ch2) { 
		ArrayList<Double> vals = new ArrayList<Double>();
		while(ps.hasNext()) { 
			ProbeLine probe = ps.next();
			Double v1 = probe.values[ch1];
			Double v2 = probe.values[ch2];
			
			if(v1 != null && v2 != null) { 
				vals.add(v1 - v2);
			}
		}
		
		addValues(nm, vals);
	}

	public void addValues(String name, Collection<Double> vs) { 
		Double[] array = vs.toArray(new Double[0]);
		Arrays.sort(array);
		
		if(adaptBounds) {
			if(min == null) { 
				min = array[0];
			} else { 
				min = Math.min(min, array[0]);
			}
			
			if(max == null) { 
				max = array[array.length-1]; 
			} else { 
				max = Math.max(max, array[array.length-1]);
			}
		}
		values.add(array);
		names.add(name);
		System.out.println(String.format("Loaded %d values -> %s", array.length, name));
		
		dispatchChangedEvent();
	}
	
	private double[] fractions(int vidx, int bins) {
		double width = (max-min);
		double binWidth = width / (double)bins;
		double[] farray = new double[bins+1];
		for(int i = 0; i < bins; i++) { farray[i] = 0.0; }
		Double[] vals = values.get(vidx);
		
		double fsum = 0.0;
		double maxF = 0.0;
		
		int i = 0;
		while(i < vals.length && vals[i] < min) { i++; }
		
		for(int b = 0; b < bins; b++ ) { 
			int c = 0;
			double binMin = min + ((double)b * binWidth);
			double binMax = binMin + binWidth;
			while(i < vals.length && vals[i] < binMax) { 
				i += 1;
				c += 1;
			}
			farray[b] = (double)c / (double)vals.length;
			fsum += farray[b];
			maxF = Math.max(maxF, farray[b]);
		}
		farray[bins] = maxF;
		
		System.out.println(String.format("%d Fraction sum: %.5f", vidx, fsum));
		
		return farray;
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2-x1, h = y2-y1;
		
		int leftScaleInset = 30;
		int granularity = 8;
		
		int lineAreaWidth = w - leftScaleInset;
		int bins = lineAreaWidth/granularity;
		
		g.setColor(Color.white);
		g.fillRect(x1, y1, w, h);
		
		ArrayList<double[]> flist = new ArrayList<double[]>();
		double maxFrac = 0.0;
		for(int i = 0; i < values.size(); i++) { 
			double[] fs = fractions(i, bins);
			flist.add(fs);
			maxFrac = Math.max(maxFrac, fs[bins]);
		}
		
		PaintableScale scale = new PaintableScale(0.0, maxFrac);
		VerticalScalePainter vsp = new VerticalScalePainter(scale);
		
		PaintableScale xScale = new PaintableScale(min, max);
		HorizontalScalePainter hsp = new HorizontalScalePainter(xScale);
		
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Stroke oldStroke = g2.getStroke();
		Stroke newStroke = new BasicStroke(2.0f);
		g2.setStroke(newStroke);
		
		for(int i = 0; i < flist.size(); i++) { 
			int px = -1, py = -1;
			double[] fractions = flist.get(i);
			
			g.setColor(staticColors[i % staticColors.length]);
			g2.setStroke(newStroke);

			for(int j = 0; j < bins; j++) {
				double f = scale.fractionalOffset(fractions[j]);
				int y = y2 - (int)Math.round(f * h);
				int x = x2 - lineAreaWidth + j*granularity;
				
				//System.out.println(String.format("\t%d: %.3f -> %d", j, fractions[j], y));
				if(px != -1) { 
					g.drawLine(px, py, x, y);
				}
				px = x; py = y;
			}
			
			String nm = names.get(i);
			if(models.containsKey(nm)) { 
				SignalModel model = models.get(nm);
				px = py = -1;
				g2.setStroke(oldStroke);
				
				for(int j = 0; j < lineAreaWidth; j++) {
					int x = x2 - lineAreaWidth + j;
					double f = (double)(j+1) / (double)(lineAreaWidth+1);
					//double signal = min + f * (max-min);
					double signal = Math.exp(min + f * (max-min));

					if(signal > 0.0) { 
						double expSignal = Math.log(model.expectedX(signal));
						
						double expf = xScale.fractionalOffset(expSignal);
						
						int y = y2 - (int)Math.round(expf * (double)h);

						//System.out.println(String.format("Model: %.3f -> %.3f (%%%.2f of [%.3f, %.3f]) / %d,%d", signal, expSignal, expf*100.0, min, max, x, y));

						if(px != -1 && py != -1) { 
							g.drawLine(px, py, x, y);
						}
						px = x; py = y;
					}
				}
			}
		}
		g2.setStroke(oldStroke);
		
		vsp.paintItem(g, x1, y1, x2, y2);
		hsp.paintItem(g, x1, y1, x2, y2);
		
		FontMetrics fm = g2.getFontMetrics();
		int textHeight = fm.getAscent() + fm.getDescent();
		int textY = y1 + textHeight + 2;
		
		for(int i = 0; i < names.size(); i++) { 
			g.setColor(staticColors[i % staticColors.length]);
			String n = names.get(i);
			int nameWidth = fm.charsWidth(n.toCharArray(), 0, n.length());
			int textX = x2 - nameWidth - 2;
			g.drawString(n, textX, textY);
			textY += textHeight + 2;
		}
	}
}
