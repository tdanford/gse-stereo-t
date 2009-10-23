package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;

import javax.swing.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Filter;
import edu.mit.csail.cgs.ewok.verbs.FilterIterator;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos.MultiChannelSegmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos.SharingMultiChannelSegmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.FlatFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.LineFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.Priors;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.InputGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.RandomInputGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.AllOrNothingSharingFactory;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.ParameterSharingFactory;
import edu.mit.csail.cgs.sigma.expression.segmentation.viz.RegionController;
import edu.mit.csail.cgs.sigma.expression.transcription.*;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.TAFit;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowInputGenerator;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.FileInputData;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.sigma.viz.NavigationListener;
import edu.mit.csail.cgs.sigma.viz.RegionListNavigator;
import edu.mit.csail.cgs.utils.database.DatabaseException;
import edu.mit.csail.cgs.utils.models.ModelListener;
import edu.mit.csail.cgs.viz.eye.ModelPrefs;
import edu.mit.csail.cgs.viz.utils.FileChooser;

public class TranscriptionVizFrame extends JFrame implements NavigationListener {
	
	public static void main(String[] args) {
		SigmaProperties sigmaProps = new SigmaProperties();
		String strain = "s288c";
		String chrom = "5";
		int start = 500000, end = 600000;
		String strand = "+";
		RegionKey region = new RegionKey(chrom, start, end, strand);
		Integer[] channels = new Integer[] { 0 };

		try {
			RandomInputGenerator 
				//gen1 = new WorkflowInputGenerator(new WorkflowProperties(), "5plus");
				gen1 = new WorkflowInputGenerator(new WorkflowProperties(), "s288c_mata_1_plus"),
				gen2 = new WorkflowInputGenerator(new WorkflowProperties(), "s288c_mata_1_negative");

			TranscriptionVizFrame frame = new TranscriptionVizFrame(strain, region);
			
			frame.addDataTrack(gen1, 0, true);
			frame.addDataTrack(gen2, 0, false);

			frame.addDataTrack(gen1, 1, true);
			frame.addDataTrack(gen2, 1, false);
			
			Genome genome = sigmaProps.getGenome(strain);
			//String exptName = "Sc TBP:Sigma:YPD vs WCE:Sigma:YPD";
			String exptName = "Sc TFIIB:Sc:YPD vs WCE:Sc:YPD";
			String exptVersion = "median linefit";
			ChipChipLocator loc = new ChipChipLocator(genome, exptName, exptVersion);

			//frame.addChipData(loc, Color.blue);
			//frame.addChipData(new ChipChipLocator(genome, "Sc TBP:Sc:YPD vs WCE:Sc:YPD", exptVersion), Color.orange);
			
			frame.showMe();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String strain;
	private Genome genome;
	
	private Vector<Integer> channels;
	private Vector<InputGenerator> generators;
	private Vector<Boolean> generatorStrands;
	
	private Vector<TranscriptionViz> vizPanels;
	private SequenceViz seqViz;
	private DisplayPanel posPane, negPane;
	
	private RegionKey region;
	
	private JCheckBoxMenuItem showProbes, showTranscripts;
	private RegionController controller;
	private TranscriptionParameters callParams;
	
	private SegmentationParameters segParams;
	private Segmenter segmenter;
	
	public TranscriptionVizFrame(
			String str, RegionKey k) {
		
		super("Transcription Visualization"); 
		
		strain = str;
		SigmaProperties sps = new SigmaProperties();
		
		WorkflowProperties wprops = new WorkflowProperties();
		callParams = wprops.getDefaultTranscriptionParameters();
		segParams = wprops.getDefaultSegmentationParameters();
		
		SegmentationParameters p = segParams;
		double probFlat = 1.0-p.probSplit-p.probLine;

		ParameterSharingFactory sharingFactory = new AllOrNothingSharingFactory();
		int numChannels = 2;
		
		segmenter = new SharingMultiChannelSegmenter(p, sharingFactory.loadSharing(numChannels),
					new FlatFitter(Math.log(probFlat), p.flatVarPenalty, p.flatIntensityPenalty), 
					new LineFitter(Math.log(p.probLine), 
							Priors.zero, Priors.zero,
							Priors.expPrior(p.lineVarPenalty)));

		/*
		segmenter = new MultiChannelSegmenter(p.minSegmentLength, p.probSplit, p.probShare, 
				new FlatFitter(Math.log(probFlat), p.flatVarPenalty, p.flatIntensityPenalty), 
				new LineFitter(Math.log(p.probLine), p.lineVarPenalty));
		*/
		
		try { 
			genome = sps.getGenome(strain);
		} catch(DatabaseException e) { 
			genome = null;
		}
		
		region = k;
		controller = new KeyController();
		seqViz = new SequenceViz(strain);
		
		generators = new Vector<InputGenerator>();
		generatorStrands = new Vector<Boolean>();
		channels = new Vector<Integer>();
		vizPanels = new Vector<TranscriptionViz>();

		JPanel vizPane = new JPanel();
		vizPane.setLayout(new BoxLayout(vizPane, BoxLayout.Y_AXIS));
		vizPane.add(posPane = new DisplayPanel());
		vizPane.add(seqViz);
		vizPane.add(negPane = new DisplayPanel());
		
		Container c = (Container)getContentPane();
		c.setLayout(new BorderLayout());
		c.add(vizPane, BorderLayout.CENTER);
		
		if(controller != null) { 
			c.add(new ControllerPanel(), BorderLayout.SOUTH);
		}
		
		setJMenuBar(createMenu());
		
		updateVizData();
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	private static class DisplayPanel extends JPanel { 
		
		private Vector<TranscriptionViz> tviz; 
		
		public DisplayPanel() { 
			super();
			tviz = new Vector<TranscriptionViz>();
		}
		
		public void addViz(TranscriptionViz v) {
			tviz.add(v);
			setLayout(new GridLayout(tviz.size(), 1));
			add(v);
		}
	}
	
	public void addChipData(ChipChipLocator loc, Color c) {
		seqViz.addChipChipValues(loc, c);
	}
	
	public void addDataTrack(InputGenerator gen, int channel, boolean strand) {
		TranscriptionViz panel = new TranscriptionViz(strain, channel, strand);
		vizPanels.add(panel);
		
		if(strand) { 
			posPane.addViz(panel); 
		} else { 
			negPane.addViz(panel); 
		}
		
		generators.add(gen);
		generatorStrands.add(strand);
		channels.add(channel);
		
		panel.setPreferredSize(new Dimension(400, 100));
		
		if(vizPanels.size() > 1) { 
			vizPanels.get(0).synchronizeScales(panel);
		}
	}
	
	private void updateVizData() {
		for(int i = 0; i < vizPanels.size(); i++) {
			System.out.println("Updating panel " + i + "...");
			TranscriptionViz panel = vizPanels.get(i);
			InputGenerator generator = generators.get(i);
			String strand = generatorStrands.get(i) ? "+" : "-";
			
			generator.generate(region.chrom, region.start, region.end, strand);
			InputData data = generator.inputData();
		
			try { 
				panel.setData(data, region.start, region.end);

				if(generator instanceof RandomInputGenerator) { 
					RandomInputGenerator rig = (RandomInputGenerator)generator;
					panel.setSegments(rig.segments(channels.get(i)).iterator());

					if(rig instanceof WorkflowInputGenerator) { 
						WorkflowInputGenerator wig = (WorkflowInputGenerator)rig;
						//panel.setCalls(wig.calls());
					}
				}
			} catch(ArrayIndexOutOfBoundsException e) { 
				System.err.println(e.getMessage());
			}
		}
		
		seqViz.setRegion(region);
	}

	public void navigateTo(String chrom, int start, int end) {
		System.out.println(String.format("navigateTo(%s, %d, %d)", chrom, start, end));
		controller.jumpTo(String.format("%s:%d-%d", chrom, start, end));
		updateVizData();
	}

	private class KeyController implements RegionController {
		
		private Pattern p = Pattern.compile("([^:]+):(\\d+)-(\\d+)");

		public void jumpTo(String loc) {
			System.out.println(String.format("Jump To: %s", loc));
			Matcher m = p.matcher(loc);
			if(m.matches()) { 
				String chrom = m.group(1);
				int start = Integer.parseInt(m.group(2));
				int end = Integer.parseInt(m.group(3));
				region = new RegionKey(chrom, start, end, region.strand);
			} else { 
				Region r = seqViz.lookupRegion(loc);
				if(r == null) { 
					System.err.println(String.format("No Match: \"%s\"", loc));
				} else { 
					jumpTo(String.format("%s:%d-%d", r.getChrom(), r.getStart(), r.getEnd()));
				}
			}
		}

		public void moveLeft() {
			int w = region.end-region.start+1;
			int w4 = w/4;
			int ns = region.start-w4;
			int ne = region.end-w4;
			region = new RegionKey(region.chrom, ns, ne, region.strand);
		}

		public void moveRight() {
			int w = region.end-region.start+1;
			int w4 = w/4;
			int ns = region.start+w4;
			int ne = region.end+w4;
			region = new RegionKey(region.chrom, ns, ne, region.strand);
		}

		public Region region() {
			return new Region(genome, region.chrom, region.start, region.end);
		}

		public void zoomIn() {
			int w = region.end-region.start+1;
			int w4 = w/4;
			int ns = region.start+w4;
			int ne = region.end-w4;
			region = new RegionKey(region.chrom, ns, ne, region.strand);
		}

		public void zoomOut() {
			int w = region.end-region.start+1;
			int w4 = w/2;
			int ns = region.start-w4;
			int ne = region.end+w4;
			region = new RegionKey(region.chrom, ns, ne, region.strand);
		} 
	}
	
	private class ControllerPanel extends JPanel {
		
		private JButton left, right, in, out;
		private JTextField navField;
		
		public ControllerPanel() { 
			setLayout(new FlowLayout());
			
			add(left = new JButton("<-"));
			add(out = new JButton("--"));
			add(navField = new JTextField());
			navField.setColumns(15);
			add(in = new JButton("++"));
			add(right = new JButton("->"));
			
			navField.addKeyListener(new KeyListener() {
				public void keyPressed(KeyEvent arg0) {
				}

				public void keyReleased(KeyEvent arg0) {
				}

				public void keyTyped(KeyEvent arg) {
					System.out.println("Key: " + arg.getKeyChar() + "(" + ((int)arg.getKeyChar()) + ") " + KeyEvent.VK_ENTER);
					if(arg.getKeyChar() == KeyEvent.VK_ENTER) { 
						String value = navField.getText().trim();
						controller.jumpTo(value);
						navField.setText(String.format("%s:%d-%d", region.chrom, region.start, region.end));
						updateVizData();
					}
				} 
			});
			
			left.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					controller.moveLeft();
					navField.setText(String.format("%s:%d-%d", region.chrom, region.start, region.end));
					updateVizData();
				}
			});
			right.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					controller.moveRight();
					navField.setText(String.format("%s:%d-%d", region.chrom, region.start, region.end));
					updateVizData();
				}
			});
			in.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					controller.zoomIn();
					navField.setText(String.format("%s:%d-%d", region.chrom, region.start, region.end));
					updateVizData();
				}
			});
			out.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					controller.zoomOut();
					navField.setText(String.format("%s:%d-%d", region.chrom, region.start, region.end));
					updateVizData();
				}
			});
		}
	}
	
	private JMenuBar createMenu() { 
		JMenuBar bar = new JMenuBar();
		
		JMenu menu = null;
		JMenuItem item = null;
		
		bar.add(menu = new JMenu("File"));
		menu.add(item = new JMenuItem(createNavigationFrameAction()));
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
		menu.add(new JSeparator());
		menu.add(new JMenuItem(createSegmentParamsAction()));
		menu.add(new JMenuItem(createSegmentAction()));
		menu.add(new JSeparator());
		menu.add(new JMenuItem(createFitParamsAction()));
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
	
	public Action createNavigationFrameAction() { 
		return new AbstractAction("Load Navigation Regions...") { 
			public void actionPerformed(ActionEvent e) { 
				FileChooser chooser = new FileChooser(TranscriptionVizFrame.this);
				File f = chooser.choose();
				if(f != null) { 
					try {
						RegionListNavigator frame = new RegionListNavigator(f);
						frame.addNavigationListener(TranscriptionVizFrame.this);
						frame.addWindowListener(new WindowAdapter() {
							public void windowClosed(WindowEvent e) {
								System.out.println("Closing Region Navigation List...");
								RegionListNavigator nav = (RegionListNavigator)e.getSource();
								nav.removeNavigationListener(TranscriptionVizFrame.this);
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
	
	public Action createFitParamsAction() {  
		return new AbstractAction("Modify Fit Params...") { 
			public void actionPerformed(ActionEvent e) {
				ModelPrefs<TranscriptionParameters> prefs = new ModelPrefs<TranscriptionParameters>(callParams);
				prefs.addModelListener(new PrefsListener());
				prefs.display();
			}
		};
	}
	
	public Action createSegmentParamsAction() {  
		return new AbstractAction("Modify Segmentation Params...") { 
			public void actionPerformed(ActionEvent e) {
				ModelPrefs<SegmentationParameters> prefs = 
					new ModelPrefs<SegmentationParameters>(segParams);
				prefs.addModelListener(new SegPrefsListener());
				prefs.display();
			}
		};
	}
	
	private class PrefsListener implements ModelListener<TranscriptionParameters> { 
		public void modelChanged(TranscriptionParameters cps) { 
			for(int i = 0; i < vizPanels.size(); i++) { 
				vizPanels.get(i).setCallingParameters(cps);
				vizPanels.get(i).setFits(new ArrayList<TAFit>());
			}			
		}
	}

	private class SegPrefsListener implements ModelListener<SegmentationParameters> { 
		public void modelChanged(SegmentationParameters cps) {
			segParams = cps;
			SegmentationParameters p = segParams;
			double probFlat = 1.0-p.probSplit-p.probLine;

			ParameterSharingFactory sharingFactory = new AllOrNothingSharingFactory();
			int channels = 2;
			
			segmenter = new SharingMultiChannelSegmenter(p, sharingFactory.loadSharing(channels),
						new FlatFitter(Math.log(probFlat), p.flatVarPenalty, p.flatIntensityPenalty), 
						new LineFitter(Math.log(p.probLine), 
								Priors.zero, Priors.zero,
								Priors.expPrior(p.lineVarPenalty)));

			/*
			segmenter = new MultiChannelSegmenter(p.minSegmentLength, p.probSplit, p.probShare, 
					new FlatFitter(Math.log(probFlat), p.flatVarPenalty, p.flatIntensityPenalty), 
					new LineFitter(Math.log(p.probLine), p.lineVarPenalty));
			*/
			
			for(int i = 0; i < vizPanels.size(); i++) { 
				vizPanels.get(i).setSegments(new ArrayList<Segment>().iterator());
			}
		}
	}

	public Action createFitAction() {  
		return new AbstractAction("Find Best Fit") { 
			public void actionPerformed(ActionEvent e) {
				for(int i = 0; i < vizPanels.size(); i++) { 
					vizPanels.get(i).findBestFit();
				}
			}
		};
	}
	
	public Action createSegmentAction() {  
		return new AbstractAction("Find Segmentation") { 
			public void actionPerformed(ActionEvent e) {
				InputData data = createInputData(true);
				if(data != null) { 
					setSegments(segmenter.segment(data), true);
				}
				data = createInputData(false);
				if(data != null) { 
					setSegments(segmenter.segment(data), false);
				}
			}
		};
	}
	
	private void setSegments(Collection<Segment> segs, boolean str) { 
		for(int i = 0; i < vizPanels.size(); i++) { 
			if(generatorStrands.get(i) == str) { 
				TranscriptionViz viz = vizPanels.get(i);
				int ch = channels.get(i);
				viz.setSegments(new FilterIterator<Segment,Segment>(
						new SegmentChannelFilter(ch),
						segs.iterator()));
			}
		}
	}
	
	private class SegmentChannelFilter implements Filter<Segment,Segment> {
		private Integer ch;
		public SegmentChannelFilter(int c) { ch = c; }
		public Segment execute(Segment s) { 
			return s.channel.equals(ch) ? s : null;
		}
	}

	private InputData createInputData(boolean str) { 
		FileInputData data = null;
		for(int i = 0; i < generators.size(); i++) { 
			InputGenerator gen = generators.get(i);
			if(generatorStrands.get(i) == str) { 
				InputData gData = gen.inputData();
				if(data == null) { 
					data = new FileInputData(gData);
				} else { 
					data.stack(gData);
				}
			}
		}

		return data;
	}
	
	private void updateTranscriptMarking() { 
		boolean value = showTranscripts.isSelected();
		for(int i = 0; i < vizPanels.size(); i++) { 
			vizPanels.get(i).setTranscriptMarking(value);
		}
	}
	
	private void updateProbeMarking() { 
		boolean value = showProbes.isSelected();
		for(int i = 0; i < vizPanels.size(); i++) { 
			vizPanels.get(i).setProbeMarking(value);
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
