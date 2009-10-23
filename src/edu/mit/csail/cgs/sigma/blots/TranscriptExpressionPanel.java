/*
 * Author: tdanford
 * Date: Jul 17, 2008
 */
package edu.mit.csail.cgs.sigma.blots;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.ewok.StandardProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;

public class TranscriptExpressionPanel extends JPanel implements ActionListener {
	
	public static void main(String[] args) { 
		BaseExpressionProperties props = new SudeepExpressionProperties();
		String key = "sigma_mat_alpha";
		Genome genome = props.getGenome(key);
		
		StandardProbeGenerator gen = new StandardProbeGenerator(props, key);
		
		edu.mit.csail.cgs.datasets.general.Point probe = 
			new edu.mit.csail.cgs.datasets.general.Point(genome, "1", 100000);

		int windowWidth = 1000;
		int[] transcripts = new int[] { 500, 250, 700 };
		
		TranscriptExpressionPanel.Frame f = setupFrame(gen, probe, windowWidth, transcripts);
		SwingUtilities.invokeLater(f);
	}
	
	public static TranscriptExpressionPanel.Frame 
		setupFrame(StandardProbeGenerator gen,
					edu.mit.csail.cgs.datasets.general.Point probe, 
					int windowWidth, 
					int[] transcripts) {
		
		int hw = windowWidth/2;
		Region window = new Region(probe.getGenome(), probe.getChrom(), probe.getLocation()-hw, probe.getLocation()+hw);
		
		TranscriptSliderPanel sliders = 
			new TranscriptSliderPanel(windowWidth, hw, transcripts);
		
		LinkedList<ExpressionProbe> probes = new LinkedList<ExpressionProbe>();
		Iterator<ExpressionProbe> itr = gen.execute(window);
		while(itr.hasNext()) { 
			probes.addLast(itr.next());
		}
		
		TranscriptExpressionPanel expr = new TranscriptExpressionPanel(window, probes, sliders);
		return new TranscriptExpressionPanel.Frame(expr, sliders);
	}
	
	public static class Frame extends JFrame implements Runnable {
		
		private TranscriptExpressionPanel expr; 
		private TranscriptSliderPanel sliders;
		
		public Frame(TranscriptExpressionPanel tep, TranscriptSliderPanel tsp) {
			super("Transcript expression");
			
			expr = tep;
			sliders = tsp;
			
			Container c = (Container)getContentPane();
			c.setLayout(new BorderLayout());
			
			c.add(expr, BorderLayout.CENTER);
			c.add(sliders, BorderLayout.SOUTH);
			
			expr.setPreferredSize(new Dimension(600, 300));
			sliders.setPreferredSize(new Dimension(600, 100));
			
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		
		public void run() { 
			setVisible(true);
			pack();
		}
	}
	
	private Region window;
	private ExpressionProbe[] probes;
	private double max;
	
	private TranscriptSliderPanel sliders;

	public TranscriptExpressionPanel(Region w, Collection<ExpressionProbe> ps, TranscriptSliderPanel tsp) { 
		window = w;
		probes = ps.toArray(new ExpressionProbe[ps.size()]);
		Arrays.sort(probes);
		
		double factor = 1.1;
		max = 1.0 / factor;
		for(int i = 0; i < probes.length; i++) { 
			double v = Math.log(probes[i].mean());
			max = Math.max(max, v);
		}
		max *= factor;
		
		sliders = tsp;
		sliders.addActionListener(this);
	}
	
	private int bpToX(int bp) { 
		int w = Math.max(getWidth(), 1);
		double f = (double)(bp-window.getStart()) / (double)window.getWidth();
		return (int)Math.round(f * (double)w);
	}
	
	private int offsetToX(int offset) { 
		int w = Math.max(getWidth(), 1);
		double f = (double)offset / (double)window.getWidth();
		return (int)Math.round(f * (double)w);
	}
	
	private int xToOffset(int x) { 
		int w = Math.max(getWidth(), 1);
		double f = (double)x / (double)w;
		return (int)Math.round(f * (double)window.getWidth());		
	}
	
	private int xToBp(int x) { 
		return window.getStart()+xToOffset(x);
	}
	
	private int intensityToY(double intensity) { 
		double f = (intensity) / max;
		return (int)Math.round(f * (double)Math.max(1, getHeight()));
	}
	
	protected void paintComponent(Graphics g) { 
		super.paintComponent(g);
		
		int w = getWidth(), h = getHeight();
		Graphics2D g2 = (Graphics2D)g;
		
		g2.setColor(Color.white);
		g2.fillRect(0, 0, w, h);
		
		g2.setColor(Color.red);
		Stroke oldStroke = g2.getStroke();
		g2.setStroke(new BasicStroke((float)2.0));
		
		int radius = 2;
		int diam = radius*2;
	
		for(int i = 0; i < probes.length; i++) { 
			int x = bpToX(probes[i].getLocation());
			int y = h - intensityToY(Math.log(probes[i].mean()));
			g.drawOval(x-radius, y-radius, diam, diam);
		}
		
		g2.setStroke(oldStroke);
		
		for(int i = 0; i < sliders.getNumBlocks(); i++) { 
			int leftOffset = sliders.getBlockOffset(i);
			int rightOffset = leftOffset + sliders.getBlockLength(i);
			int x1 = offsetToX(leftOffset);
			int x2 = offsetToX(rightOffset);
			
			g2.setColor(new Color(0, 0, 250, 150));
			g2.fillRect(x1, 0, (x2-x1), h);
		}
	}

	public void actionPerformed(ActionEvent e) {
		TranscriptAdjustedEvent evt = (TranscriptAdjustedEvent)e;
		repaint();
	}
}
