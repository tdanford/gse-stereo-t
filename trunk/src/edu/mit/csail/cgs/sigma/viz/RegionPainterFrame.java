package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import java.io.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.motifs.WeightMatrix;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.RefGeneGenerator;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.StacieExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.ewok.ExpressionProbeGenerator;
import edu.mit.csail.cgs.sigma.litdata.steinmetz.SteinmetzProbeGenerator;
import edu.mit.csail.cgs.sigma.motifs.*;
import edu.mit.csail.cgs.viz.utils.FileChooser;

public class RegionPainterFrame extends JFrame implements NavigationListener {
	
	private JButton left, right, zoomOut, zoomIn, chromPrev, chromNext, goButton;
	private RegionPainterPanel panel;
	private MatrixComboBox matrixBox;
    private JTextField coordinateField;
	
	public RegionPainterFrame(RegionPainterPanel rpp) {
		super("Region Painter");
		panel = rpp;
		
		setJMenuBar(createJMenuBar());
		
		Container c = (Container)getContentPane();
		c.setLayout(new BorderLayout());
		c.add(panel, BorderLayout.CENTER);
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout());
		buttons.add(matrixBox = new MatrixComboBox());
        buttons.add(chromPrev = new JButton("<-CHR"));
		buttons.add(left = new JButton("<-"));
		buttons.add(zoomOut = new JButton("--"));
		buttons.add(zoomIn = new JButton("++"));
		buttons.add(right = new JButton("->"));
        buttons.add(chromNext = new JButton("CHR->"));
        
        buttons.add(coordinateField = new JTextField(panel.getRegion().getLocationString()));
        buttons.add(goButton = new JButton("Go"));
        
		c.add(buttons, BorderLayout.SOUTH);
		
        goButton.addActionListener(new ActionListener() { 
            public void actionPerformed(ActionEvent e) {
                Region current = panel.getRegion();
                Region r = Region.fromString(current.getGenome(), coordinateField.getText());
                if(r == null) { 
                    String name = coordinateField.getText();
                    Genome g = current.getGenome();
                    RefGeneGenerator gen = new RefGeneGenerator(g, "sgdGene");
                    Iterator<Gene> genes = gen.byName(name);
                    if(genes.hasNext()) { 
                        r = genes.next();
                    }
                }
                
                if(r != null) { 
                    panel.setRegion(r);
                }
                coordinateField.setText(panel.getRegion().getLocationString());
            }
        });
        
        chromPrev.addActionListener(new ActionListener() { 
            public void actionPerformed(ActionEvent e) { 
                panel.prevChrom();
                coordinateField.setText(panel.getRegion().getLocationString());
            }
        });
        
        chromNext.addActionListener(new ActionListener() { 
            public void actionPerformed(ActionEvent e) { 
                panel.nextChrom();
                coordinateField.setText(panel.getRegion().getLocationString());
            }
        });
        
        left.addActionListener(new ActionListener() { 
            public void actionPerformed(ActionEvent e) { 
                panel.shiftLeft();
                coordinateField.setText(panel.getRegion().getLocationString());
            }
        });
        
		zoomOut.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				panel.zoomOut();
                coordinateField.setText(panel.getRegion().getLocationString());
			}
		});
		
		zoomIn.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				panel.zoomIn();
                coordinateField.setText(panel.getRegion().getLocationString());
			}
		});
		
		right.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				panel.shiftRight();
                coordinateField.setText(panel.getRegion().getLocationString());
			}
		});
		
		for(WeightMatrix m : rpp.getSigmaPainter().getMatrices()) { 
			matrixBox.addWeightMatrix(m);
		}
		
		matrixBox.addMatrixSelectionListener(new MatrixComboBox.MatrixSelectionListener() {
			public void matrixSelected(WeightMatrix m) {
				panel.getSigmaPainter().getMotifScorePainter().setMatrix(m);
			} 
		});
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		pack();
	}

	public void navigateTo(String chrom, int start, int end) {
		Region r = panel.getRegion();
		if(r != null) { 
			Genome g = r.getGenome();
			Region nr = new Region(g, chrom, start, end);
			panel.setRegion(nr);
			SwingUtilities.invokeLater(new Runnable() { 
				public void run() { 
		            coordinateField.setText(panel.getRegion().getLocationString());								
				}
			});
		}
	}	

	public void saveImage() { 
		FileChooser chooser = new FileChooser(this);
        File f = chooser.choose();
        panel.saveImage(f);
	}
	
	public Action createExitAction() { 
		return new AbstractAction("Exit") { 
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() { 
					public void run() { 
						setVisible(false);
						dispose();
						System.exit(0);
					}
				});
			}
		};
	}
	
	public void loadSteinmetzData(File f) {
		SteinmetzLoader loader = new SteinmetzLoader(panel.getRegion().getGenome(), f);
		Thread t = new Thread(loader);
		t.start();
	}
	
	private class SteinmetzLoader implements Runnable { 
		private Genome genome;
		private File file;
		
		public SteinmetzLoader(Genome g, File f) { 
			genome = g;
			file = f;
		}
		
		public void run() { 
			ExpressionProbeGenerator gen = new SteinmetzProbeGenerator(genome, file);
			RegionPainter rp = new ExpressionProbePainter(gen, "Steinmetz Data");
			panel.getSigmaPainter().addSingleExprPainter(rp);
		}
	}
	
	public Action createLoadRegionsAction() { 
		return new AbstractAction("Load Navigation Regions...") { 
			public void actionPerformed(ActionEvent e) { 
				FileChooser chooser = new FileChooser(RegionPainterFrame.this);
				File f = chooser.choose();
				if(f != null) { 
					try {
						RegionListNavigator frame = new RegionListNavigator(f);
						frame.addNavigationListener(RegionPainterFrame.this);
						frame.addWindowListener(new WindowAdapter() {
							public void windowClosed(WindowEvent e) {
								System.out.println("Closing Region Navigation List...");
								RegionListNavigator nav = (RegionListNavigator)e.getSource();
								nav.removeNavigationListener(RegionPainterFrame.this);
							}
						});
						frame.display();
					} catch (IOException e1) {
						e1.printStackTrace(System.err);
					}
					
				}
			}
		};
	}
	
	private JMenuBar createJMenuBar() { 
		JMenuBar bar = new JMenuBar();
		JMenu menu = null;
		JMenuItem item = null;
		
		bar.add(menu = new JMenu("File"));
		menu.add(item = new JMenuItem(createLoadRegionsAction()));
		menu.add(item = new JMenuItem("Load Steinmetz..."));
		item.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						FileChooser chooser = new FileChooser(RegionPainterFrame.this, "Open");
						File f = chooser.choose();
						if(f != null) { 
							loadSteinmetzData(f);
						}
					}
				});
				
			}
		});
		
		menu.add(new JSeparator());
		menu.add(new JMenuItem(createExitAction()));
		
		bar.add(menu = new JMenu("Image"));
		menu.add(item = new JMenuItem("Save Image..."));
		
		item.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				saveImage();
			}
		});
		
		return bar;
	}

}
