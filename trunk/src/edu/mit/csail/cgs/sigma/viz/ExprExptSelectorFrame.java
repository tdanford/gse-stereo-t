/*
 * Author: tdanford
 * Date: May 23, 2008
 */
package edu.mit.csail.cgs.sigma.viz;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.motifs.WeightMatrix;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.sigma.motifs.Motifs;

public class ExprExptSelectorFrame extends JFrame {
	
	public static void main(String[] args) { 
		SigmaProperties props = new SigmaProperties();
		new ExprExptSelectorFrame(props, "sigma");
		new ExprExptSelectorFrame(props, "s288c");
	}
	
	private ExprExptSelectorPanel selectorPanel;
	private JButton startViewerButton;
	
	private SigmaProperties sprops;
	private GeneAnnotationProperties gaProps;
	private String strain;

	public ExprExptSelectorFrame(SigmaProperties sps, String str) { 
		super(String.format("%s Expression", str));
		
		sprops = sps;
		strain = str;
		gaProps = new GeneAnnotationProperties(sprops, "default");
		
		Container c = (Container)getContentPane();
		c.setLayout(new BorderLayout());
		
		JPanel p1  = new JPanel(); 
		p1.setLayout(new BorderLayout());
		
		JPanel p2 = new JPanel();
		p2.setLayout(new FlowLayout());
		
		p1.add(selectorPanel = new ExprExptSelectorPanel(strain), BorderLayout.CENTER);
		p2.add(startViewerButton = new JButton("Start Viewer!"));
		
		c.add(p1, BorderLayout.CENTER);
		c.add(p2, BorderLayout.SOUTH);
		
		selectorPanel.addExpts(new SudeepExpressionProperties());
		selectorPanel.addExpts(new StacieExpressionProperties());
		
		startViewerButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				startViewer();
			}
		});

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		pack();
	}
	
	public RegionPainterFrame startViewer() { 
		SigmaRegionPainter painter = new SigmaRegionPainter(sprops, strain);
		Collection<ExprExptSelector> selectors = selectorPanel.getSelected();

		for(ExprExptSelector sel : selectors) { 
			painter.addExprExpt(sel);
		}

		Motifs motifs = painter.getMotifs();
		MotifRegionPainter mp = painter.getMotifPainter();

		Collection<WeightMatrix> matrices = motifs.getMatrices();
		TreeMap<String,WeightMatrix> matmap = new TreeMap<String,WeightMatrix>();
		for(WeightMatrix m : matrices) { 
			matmap.put(m.name, m);
			System.out.println(m.name);
			mp.addWeightMatrix(m);
		}

		Region region = new Region(sprops.getGenome(strain), "3", 10000, 20000); 

		RegionPainterPanel panel = new RegionPainterPanel(painter, region);
		RegionPainterFrame frame = new RegionPainterFrame(panel);
		
		return frame;
	}
}

class ExprExptSelectorPanel extends JPanel {
	
	private String strain;
	private Vector<JCheckBox[]> boxes;
	private Vector<ExprExptSelector[]> selectors;
	
	public ExprExptSelectorPanel(String str) { 
		super();
		
		strain = str;
		boxes = new Vector<JCheckBox[]>();
		selectors = new Vector<ExprExptSelector[]>();
		
		setLayout(new FlowLayout());
	}
	
	public Collection<ExprExptSelector> getSelected() { 
		LinkedList<ExprExptSelector> sels = new LinkedList<ExprExptSelector>();
		for(int i = 0; i < selectors.size(); i++) {
			JCheckBox[] barray = boxes.get(i);
			ExprExptSelector[] sarray = selectors.get(i);
			for(int j = 0; j < barray.length; j++) { 
				if(barray[j].isSelected()) { 
					sels.add(sarray[j]);
					System.out.println(String.format("=> \"%s\"", sarray[j]));
				}
			}
		}
		return sels;
	}
	
	public void addExpts(BaseExpressionProperties props) { 
		Collection<ExprExptSelector> sels = ExprExptSelector.createSelectors(props, strain);
		
		if(!sels.isEmpty()) { 
			ExprExptSelector[] array = sels.toArray(new ExprExptSelector[sels.size()]);
			JCheckBox[] barray = new JCheckBox[array.length];
			JPanel panel = new JPanel();
			panel.setLayout(new GridLayout(array.length, 1));

			for(int i = 0; i < array.length; i++) { 
				barray[i] = new JCheckBox(array[i].toString());
				panel.add(barray[i]);
			}

			boxes.add(barray);
			selectors.add(array);

			add(panel);
			invalidate();
		}
	}
}
