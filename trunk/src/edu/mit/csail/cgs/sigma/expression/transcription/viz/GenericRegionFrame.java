package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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
import edu.mit.csail.cgs.ewok.verbs.RefGeneGenerator;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.expression.segmentation.InputData;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.segmentation.SegmentationParameters;
import edu.mit.csail.cgs.sigma.expression.segmentation.Segmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos.MultiChannelSegmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.dpalgos.SharingMultiChannelSegmenter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.FlatFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.fitters.LineFitter;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.InputGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.input.RandomInputGenerator;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.AllOrNothingSharingFactory;
import edu.mit.csail.cgs.sigma.expression.segmentation.sharing.ParameterSharingFactory;
import edu.mit.csail.cgs.sigma.expression.segmentation.viz.RegionController;
import edu.mit.csail.cgs.sigma.expression.transcription.TranscriptArrangement;
import edu.mit.csail.cgs.sigma.expression.transcription.Call;
import edu.mit.csail.cgs.sigma.expression.transcription.fitters.TAFit;
import edu.mit.csail.cgs.sigma.expression.transcription.identifiers.ExhaustiveIdentifier;
import edu.mit.csail.cgs.sigma.expression.transcription.identifiers.TranscriptIdentifier;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowInputGenerator;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;
import edu.mit.csail.cgs.sigma.expression.workflow.models.FileInputData;
import edu.mit.csail.cgs.sigma.expression.workflow.models.RegionKey;
import edu.mit.csail.cgs.sigma.viz.NavigationListener;
import edu.mit.csail.cgs.sigma.viz.RegionListNavigator;
import edu.mit.csail.cgs.utils.database.DatabaseException;
import edu.mit.csail.cgs.utils.models.ModelListener;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.eye.ModelPrefs;
import edu.mit.csail.cgs.viz.paintable.DoubleBufferedPaintable;
import edu.mit.csail.cgs.viz.paintable.Paintable;
import edu.mit.csail.cgs.viz.paintable.PaintableChangedEvent;
import edu.mit.csail.cgs.viz.paintable.PaintableChangedListener;
import edu.mit.csail.cgs.viz.paintable.PaintablePanel;
import edu.mit.csail.cgs.viz.utils.FileChooser;

public class GenericRegionFrame extends JFrame implements NavigationListener, PaintableChangedListener {
	
	private Genome genome;
	private RefGeneGenerator generator;
	
	private RegionController controller;
	private AbstractRegionPaintable regionPaintable;
	private RegionPaintablePanel regionPaintablePanel;
	
	private JMenu actionMenu;
	
	private Double startDrag;
	private RegionKey indicated;
	
	public GenericRegionFrame(AbstractRegionPaintable rp) {
		super("Transcription Visualization"); 
		
		regionPaintable = rp;
		controller = new KeyController();
		
		genome = regionPaintable.getRegion().getGenome();

		Container c = (Container)getContentPane();
		c.setLayout(new BorderLayout());
		
		if(controller != null) { 
			c.add(new ControllerPanel(), BorderLayout.SOUTH);
		}
		
		regionPaintablePanel = new RegionPaintablePanel(new DoubleBufferedPaintable(regionPaintable));
		//regionPaintablePanel = new PaintablePanel(regionPaintable);
		
		c.add(regionPaintablePanel, BorderLayout.CENTER);
		setJMenuBar(createMenu());
		
		rp.addPaintableChangedListener(this);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		startDrag = null;
		indicated = null;
	}
	
	private class InteractiveMouseListener extends MouseAdapter {
		
		private JPanel panel; 
		
		public InteractiveMouseListener(JPanel p) { panel = p; } 

		public void mousePressed(MouseEvent e) { 
			Region r = regionPaintable.getRegion();
			if(e.getButton() == MouseEvent.BUTTON1 && r != null) { 
				int x = e.getX();
				double f = (double)x / (double)panel.getWidth();
				startDrag = f;
				int start = (int)Math.round(f * (double)r.getWidth()) + r.getStart();
				indicated = new RegionKey(r.getChrom(), start, start, "+");
				System.out.println(String.format("Indicated: %s", indicated.toString()));
				e.getComponent().repaint();
			}
		}

		public void mouseReleased(MouseEvent e) {
			startDrag = null;
		}
	}
	
	private class InteractiveMouseMotionListener extends MouseMotionAdapter { 

		private JPanel panel; 
		
		public InteractiveMouseMotionListener(JPanel p) { panel = p; } 

		public void mouseDragged(MouseEvent e) { 
			Region r = regionPaintable.getRegion();
			if(r != null) { 
				int x = e.getX();
				double f = (double)x / (double)panel.getWidth();
				double leftf = Math.min(f, startDrag);
				double rightf = Math.max(f, startDrag);
				int start = (int)Math.round(leftf * (double)r.getWidth()) + r.getStart();
				int end = (int)Math.round(rightf * (double)r.getWidth()) + r.getStart();
				indicated = new RegionKey(r.getChrom(), start, end, "+");
				System.out.println(String.format("Indicated: %s", indicated.toString()));
				e.getComponent().repaint();
			}
		}		
	}
	
	private class RegionPaintablePanel extends PaintablePanel {
		
		public RegionPaintablePanel(Paintable p) { 
			super(p);
			addMouseListener(new InteractiveMouseListener(this));
			addMouseMotionListener(new InteractiveMouseMotionListener(this));
		}
		
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			Graphics2D g2 = (Graphics2D)g;
			Region region = regionPaintable.getRegion();
			int x1 = 0, y1 = 0, w = getWidth(), h = getHeight();
			int x2 = x1 + w, y2 = y1 + h;
			
			if(indicated != null && region != null) {
				double leftf = (double)(indicated.start - region.getStart()) / (double)region.getWidth();
				double rightf = (double)(indicated.end - region.getStart()) / (double)region.getWidth();
				int left = x1 + (int)Math.floor(leftf * (double)w);
				int right = x1 + (int)Math.floor(rightf * (double)w);
				
				g2.setColor(Coloring.clearer(Coloring.clearer(Color.red)));
				g2.fillRect(left, y1, right-left, y2-y1);
				
				g2.setColor(Color.black);
				g2.drawRect(left, y1, right-left, y2-y1);
				
				FontMetrics fm = g2.getFontMetrics();
				String leftLabel = String.format("%d", indicated.start);
				String rightLabel = String.format("%d", indicated.end);
				String lengthLabel = String.format("%d", indicated.width());
				int leftWidth = fm.charsWidth(leftLabel.toCharArray(), 0, leftLabel.length());
				int rightWidth = fm.charsWidth(rightLabel.toCharArray(), 0, rightLabel.length());
				int middleWidth = fm.charsWidth(lengthLabel.toCharArray(), 0, lengthLabel.length());
				
				g2.drawString(leftLabel, left-leftWidth-2, y1 + fm.getAscent() + 2);
				g2.drawString(rightLabel, right+2, y1 + fm.getAscent() + 2);
				
				if(middleWidth < (right-left)) {
					int offset = Math.max(0, (left+right)/2 - middleWidth/2);
					g2.drawString(lengthLabel, offset, y1 + fm.getAscent() + 2);
				}
				
				System.out.println(String.format("Indicated: %s", indicated.toString()));
			}
		}
	}
	
	public PaintablePanel getRegionPaintablePanel() { return regionPaintablePanel; }
	
	public void paintableChanged(PaintableChangedEvent e) { 
		System.err.println("Repaint!");
		repaint();
	}
	
	public void addActionMenuItem(Action a) { 
		SwingUtilities.invokeLater(new ActionMenuAdder(a));
	}
	
	public void addActionMenuSeparator() { 
		SwingUtilities.invokeLater(new MenuSeparationAdder(actionMenu));
	}
	
	private class MenuSeparationAdder implements Runnable { 
		private JMenu menu; 
		public MenuSeparationAdder(JMenu m) { menu = m; }
		public void run() { 
			menu.add(new JSeparator());
		}
	}
	
	private class ActionMenuAdder implements Runnable { 
		private Action[] acts;
		public ActionMenuAdder(Action aa) { acts = new Action[] { aa }; }
		public ActionMenuAdder(Collection<Action> as) { acts = as.toArray(new Action[0]); }
		public void run() { 
			for(int i = 0; i < acts.length; i++) { 
				actionMenu.add(new JMenuItem(acts[i]));
			}
		}
	}
	
	public Region lookupRegion(String name) { 
		return null;
	}
	
	public void navigateTo(String chrom, int start, int end) {
		System.out.println(String.format("navigateTo(%s, %d, %d)", chrom, start, end));
		controller.jumpTo(String.format("%s:%d-%d", chrom, start, end));
	}

	private class KeyController implements RegionController {
		
		private Pattern p = Pattern.compile("([^:]+):(\\d+)-(\\d+)");

		public void jumpTo(String loc) {
			Region r = regionPaintable.getRegion();
			System.out.println(String.format("Jump To: %s", loc));
			Matcher m = p.matcher(loc);
			if(m.matches()) { 
				String chrom = m.group(1);
				int start = Integer.parseInt(m.group(2));
				int end = Integer.parseInt(m.group(3));
				regionPaintable.setRegion(new Region(r.getGenome(), chrom, start, end));
			} else { 
				r = lookupRegion(loc);
				if(r == null) { 
					System.err.println(String.format("No Match: \"%s\"", loc));
				} else { 
					jumpTo(String.format("%s:%d-%d", r.getChrom(), r.getStart(), r.getEnd()));
				}
			}
		}

		public void moveLeft() {
			Region region = regionPaintable.getRegion();
			int w = region.getEnd()-region.getStart()+1;
			int w4 = w/4;
			int ns = region.getStart()-w4;
			int ne = region.getEnd()-w4;
			regionPaintable.setRegion(new Region(region.getGenome(), region.getChrom(), ns, ne));
		}

		public void moveRight() {
			Region region = regionPaintable.getRegion();
			int w = region.getEnd()-region.getStart()+1;
			int w4 = w/4;
			int ns = region.getStart()+w4;
			int ne = region.getEnd()+w4;
			regionPaintable.setRegion(new Region(region.getGenome(), region.getChrom(), ns, ne));
		}

		public Region region() {
			return regionPaintable.getRegion();
		}

		public void zoomIn() {
			Region region = regionPaintable.getRegion();
			int w = region.getEnd()-region.getStart()+1;
			int w4 = w/4;
			int ns = region.getStart()+w4;
			int ne = region.getEnd()-w4;
			regionPaintable.setRegion(new Region(region.getGenome(), region.getChrom(), ns, ne));
		}

		public void zoomOut() {
			Region region = regionPaintable.getRegion();
			int w = region.getEnd()-region.getStart()+1;
			int w4 = w/2;
			int ns = region.getStart()-w4;
			int ne = region.getEnd()+w4;
			regionPaintable.setRegion(new Region(region.getGenome(), region.getChrom(), ns, ne));
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
						Region region = regionPaintable.getRegion();
						navField.setText(String.format("%s:%d-%d", 
								region.getChrom(), region.getStart(), region.getEnd()));
						regionPaintable.update();
					}
				} 
			});
			
			left.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					controller.moveLeft();
					Region region = regionPaintable.getRegion();
					navField.setText(String.format("%s:%d-%d", 
							region.getChrom(), region.getStart(), region.getEnd()));
					regionPaintable.update();
				}
			});
			right.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					controller.moveRight();
					Region region = regionPaintable.getRegion();
					navField.setText(String.format("%s:%d-%d", 
							region.getChrom(), region.getStart(), region.getEnd()));
					regionPaintable.update();
				}
			});
			in.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					controller.zoomIn();
					Region region = regionPaintable.getRegion();
					navField.setText(String.format("%s:%d-%d", 
							region.getChrom(), region.getStart(), region.getEnd()));
					regionPaintable.update();
				}
			});
			out.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					controller.zoomOut();
					Region region = regionPaintable.getRegion();
					navField.setText(String.format("%s:%d-%d", 
							region.getChrom(), region.getStart(), region.getEnd()));
					regionPaintable.update();
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
		
		bar.add(menu = new JMenu("Actions"));

		Collection<Action> actions = collectActions();
		//menu.add(new JMenuItem(fPanel.getPaintable().getSaveImageAction()));
		for(Action a : actions) { 
			menu.add(new JMenuItem(a));
		}
		    
		actionMenu = menu;
		
		return bar;
	}

	private Collection<Action> collectActions() {
		return regionPaintable.getPaintableActions();
	}
	
	public void addAction(Action act) {
		SwingUtilities.invokeLater(new ActionMenuAdder(act));
	}

	public void addActions(Collection<Action> acts) {
		SwingUtilities.invokeLater(new ActionMenuAdder(acts));		
	}

	public Action createNavigationFrameAction() { 
		return new AbstractAction("Load Navigation Regions...") { 
			public void actionPerformed(ActionEvent e) { 
				FileChooser chooser = new FileChooser(GenericRegionFrame.this);
				File f = chooser.choose();
				if(f != null) { 
					try {
						RegionListNavigator frame = new RegionListNavigator(f);
						frame.addNavigationListener(GenericRegionFrame.this);
						frame.addWindowListener(new WindowAdapter() {
							public void windowClosed(WindowEvent e) {
								System.out.println("Closing Region Navigation List...");
								RegionListNavigator nav = (RegionListNavigator)e.getSource();
								nav.removeNavigationListener(GenericRegionFrame.this);
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
	
	public void showMe() { 
		SwingUtilities.invokeLater(new Runnable() { 
			public void run() { 
				setVisible(true);
				setLocation(100, 100);
				pack();
			}
		});
	}

}
