package edu.mit.csail.cgs.sigma.viz;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.motifs.WeightMatrix;
import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.motifs.Motifs;

public class StartupFrame extends JFrame {
	
	public static void main(String[] args) { 
		new StartupFrame(new StacieExpressionProperties());
		new StartupFrame(new SudeepExpressionProperties());
	}
	
	private JComboBox exptBox;
	private DefaultComboBoxModel exptModel;
	private JButton startButton;
	
	private BaseExpressionProperties props;

	public StartupFrame(BaseExpressionProperties ps) { 
		super("Tiled Expression Visualizer");
		props = ps;
		
		Container c = (Container)getContentPane();
		c.setLayout(new GridLayout(2, 1));
		
		exptModel = new DefaultComboBoxModel();
		exptBox = new JComboBox(exptModel);
		startButton = new JButton("Start!");
		
		c.add(exptBox);
		c.add(startButton);
		
		startButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				startVisualizer();
			}
		});
		
		initStartupFrame();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		setVisible(true);
		pack();
	}
	
	private void initStartupFrame() { 
		Collection<String> expts = props.getExptKeys();
		String currentExpt = null;
		for(String key : expts) { 
			exptModel.addElement(key);
			currentExpt = key;
		}
		
		exptBox.setSelectedItem(currentExpt);
	}
	
	public void closeFrame() { 
		setVisible(false);
		dispose();
	}
	
	public RegionPainterFrame startVisualizer() { 
		String exptKey = (String)exptBox.getSelectedItem();
		SigmaProperties sp = props.getSigmaProperties();
		String strain = props.parseStrainFromExptKey(exptKey);
		
		SigmaRegionPainter painter = 
			new SigmaRegionPainter(sp, strain);
		painter.addExprExpt(new ExprExptSelector(props, exptKey));
		
		Motifs motifs = painter.getMotifs();
		MotifRegionPainter mp = painter.getMotifPainter();
		
		Collection<WeightMatrix> matrices = motifs.getMatrices();
		TreeMap<String,WeightMatrix> matmap = new TreeMap<String,WeightMatrix>();
		for(WeightMatrix m : matrices) { 
			matmap.put(m.name, m);
			System.out.println(m.name);
			mp.addWeightMatrix(m);
		}
		
		Region region = new Region(sp.getGenome(strain), "3", 10000, 20000); 

		RegionPainterPanel panel = new RegionPainterPanel(painter, region);
		RegionPainterFrame frame = new RegionPainterFrame(panel);
		
		SwingUtilities.invokeLater(new Runnable() { 
			public void run() { 
				//closeFrame();
			}
		});
		
		return frame;
	}
	
}
