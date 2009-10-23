package edu.mit.csail.cgs.sigma.expression.segmentation.viz;

import java.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.TAFit;
import edu.mit.csail.cgs.sigma.expression.transcription.identifiers.ExhaustiveIdentifier;
import edu.mit.csail.cgs.sigma.expression.transcription.identifiers.TranscriptIdentifier;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;

public class SegmentVizFrame extends JFrame {
	
	private SegmentViz[] vizPanel;
	private JCheckBoxMenuItem showProbes, showTranscripts;
	private Runnable runner;
	private RegionController controller;
	
	public SegmentVizFrame(Runnable r, RegionController cnt, SegmentViz... sv) { 
		super("Segmentation Visualization"); 
		
		runner = r;
		vizPanel = sv.clone();
		controller = cnt;
		
		JPanel vizPane = new JPanel();
		vizPane.setLayout(new GridLayout(vizPanel.length,1));
		
		Container c = (Container)getContentPane();
		c.setLayout(new BorderLayout());
		c.add(vizPane, BorderLayout.CENTER);
		
		for(int i = 0; i < vizPanel.length; i++) { 
			vizPane.add(vizPanel[i]);
			vizPanel[i].setPreferredSize(new Dimension(400, 100));
			if(i > 0) { 
				vizPanel[0].synchronizeScales(vizPanel[i]);
			}
		}
		
		if(controller != null) { 
			c.add(new ControllerPanel(), BorderLayout.SOUTH);
		}
		
		setJMenuBar(createMenu());
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		showMe();
	}
	
	private class ControllerPanel extends JPanel {
		
		private JButton left, right, in, out;
		private JTextField navField;
		
		public ControllerPanel() { 
			setLayout(new FlowLayout());
			
			add(left = new JButton("<-"));
			add(out = new JButton("--"));
			//add(navField = new JTextField());
			add(in = new JButton("++"));
			add(right = new JButton("->"));
			
			left.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					controller.moveLeft();
					runner.run();
				}
			});
			right.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					controller.moveRight();
					runner.run();
				}
			});
			in.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					controller.zoomIn();
					runner.run();
				}
			});
			out.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					controller.zoomOut();
					runner.run();
				}
			});
		}
	}
	
	private JMenuBar createMenu() { 
		JMenuBar bar = new JMenuBar();
		
		JMenu menu = null;
		JMenuItem item = null;
		
		bar.add(menu = new JMenu("File"));
		if(runner != null) { 
			menu.add(item = new JMenuItem("New"));
			item.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					runner.run();
				}
			});
			menu.add(new JSeparator());
		}
		menu.add(item = new JMenuItem("Exit"));
		item.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				System.exit(0);
			}
		});
		
		/*
		bar.add(menu = new JMenu("Image"));
		menu.add(item = new JMenuItem(vizPanel.createSnapshotAction()));
		*/
		
		bar.add(menu = new JMenu("Interactive"));
		menu.add(showProbes = new JCheckBoxMenuItem("Show Probes"));
		menu.add(showTranscripts = new JCheckBoxMenuItem("Show Transcripts"));
		menu.add(new JMenuItem(createFitAction()));
		
		showTranscripts.setSelected(false);
		showProbes.setSelected(false);
		
		showTranscripts.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				updateTranscriptMarking();
			}
		});
		showProbes.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				updateProbeMarking();
			}
		});
		
		return bar;
	}
	
	public Action createFitAction() {  
		return new AbstractAction("Find Best Fit...") { 
			public void actionPerformed(ActionEvent e) {
				ExhaustiveIdentifier ident = new ExhaustiveIdentifier();
				for(int i = 0; i < vizPanel.length; i++) { 
					vizPanel[i].findBestFit(ident);
					
					System.out.println("--- Ranked Arrangements ---");
					TAFit first = null, prev = null;
					double diff = Math.log(2.0);
					
					for(TAFit fit : ident.getRankedFits()) {
						String overlapStr = fit.arrangement.containsOverlap() ? "*" : " ";
						double score = fit.getScore();
						if(first != null && (first.getScore() - score) >= diff && 
								(first.getScore() - prev.getScore()) < diff) { 
							System.out.println("=== 50% Line ===");
						}
						
						System.out.println(String.format("%s %.3f\t%s", 
								overlapStr, score, fit.arrangement.toString()));
						prev = fit;
						if(first == null) { first = fit; }
					}
					System.out.println();
				}
			}
		};
	}
	
	private void updateTranscriptMarking() { 
		boolean value = showTranscripts.isSelected();
		for(int i = 0; i < vizPanel.length; i++) { 
			vizPanel[i].setTranscriptMarking(value);
		}
	}
	
	private void updateProbeMarking() { 
		boolean value = showProbes.isSelected();
		for(int i = 0; i < vizPanel.length; i++) { 
			vizPanel[i].setProbeMarking(value);
		}
	}
	
	private void showMe() { 
		SwingUtilities.invokeLater(new Runnable() { 
			public void run() { 
				setVisible(true);
				setLocation(100, 100);
				pack();
			}
		});
	}
}
