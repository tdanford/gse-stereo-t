/*
 * Author: tdanford
 * Date: Aug 30, 2009
 */
package edu.mit.csail.cgs.sigma.expression.transcription.viz;

import java.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.sigma.expression.transcription.*;
import edu.mit.csail.cgs.sigma.expression.transcription.recursive.*;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.*;
import edu.mit.csail.cgs.utils.models.ModelListener;
import edu.mit.csail.cgs.viz.eye.ModelPrefs;
import edu.mit.csail.cgs.viz.paintable.*;

public class TranscriptCallingViewer extends JFrame {

	private WorkflowProperties props;
	private String key;
	private TranscriptionParameters transParams;
	private NewVizPaintableProperties vizProps;

	private Cluster cluster;
	private DataSegmentPaintable dataPainter;
	private ArrayList<DataSegment> segments;
	private Integer[] channels;
	
	private JMenuBar menuBar;
	private PaintablePanel dataPanel;
	
	
	public TranscriptCallingViewer(String k, WorkflowProperties ps, Cluster cl) {
		key = k;
		props = ps;
		transParams = props.getDefaultTranscriptionParameters();
		cluster = cl;
		segments = new ArrayList<DataSegment>();
		for(int i = 0; i < cluster.segments.length; i++) { 
			segments.add(cluster.segments[i]);
		}
		channels = cluster.channels.clone();
		vizProps = new NewVizPaintableProperties();
		dataPainter = new DataSegmentPaintable(vizProps, segments, channels);
		dataPainter.setRegion(new Region(props.getSigmaProperties().getS288cGenome(), 
				cl.segments[0].chrom, cl.segments[0].start, cl.segments[cl.segments.length-1].end));
		
		dataPanel = new PaintablePanel(dataPainter);
		dataPanel.setPreferredSize(new Dimension(600, 400));
		
		menuBar = createMenuBar();
		setJMenuBar(menuBar);
		
		Container c = (Container)getContentPane();
		c.setLayout(new BorderLayout());
		
		c.add(dataPanel, BorderLayout.CENTER);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
	
	private JMenuBar createMenuBar() { 
		JMenuBar bar = new JMenuBar();
		JMenu menu = null;
		JMenuItem item = null;
		
		bar.add(menu = new JMenu("File"));
		menu.add(item = new JMenuItem("Close"));
		item.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				dispose();
			}
		});

		bar.add(menu = new JMenu("Edit"));
		menu.add(item = new JMenuItem(createEditVizPropsAction()));
		menu.add(item = new JMenuItem(createTranscriptionVizPropsAction()));

		bar.add(menu = new JMenu("Transcripts")); 
		menu.add(item = new JMenuItem(new TranscriptFindingAction(key, 0)));
		
		return bar;
	}
	
	public Action createEditVizPropsAction() { 
		return new AbstractAction("Edit Viz Properties...") { 
			public void actionPerformed(ActionEvent e) { 
				
				ModelPrefs<NewVizPaintableProperties> sprefs = 
					new ModelPrefs<NewVizPaintableProperties>(vizProps);
				
				sprefs.addModelListener(new ModelListener<NewVizPaintableProperties>() {
					public void modelChanged(NewVizPaintableProperties model) {
						vizProps.setFromModel(model);
					} 
				});
				sprefs.display();
			}
		};
	}
	
	public Action createTranscriptionVizPropsAction() { 
		return new AbstractAction("Edit Transcription Properties...") { 
			public void actionPerformed(ActionEvent e) { 
				
				ModelPrefs<TranscriptionParameters> sprefs = 
					new ModelPrefs<TranscriptionParameters>(transParams);
				
				sprefs.addModelListener(new ModelListener<TranscriptionParameters>() {
					public void modelChanged(TranscriptionParameters model) {
						transParams.setFromModel(model);
					} 
				});
				sprefs.display();
			}
		};
	}
	
	private class TranscriptFindingAction extends AbstractAction {
		private String key;
		private Integer[] indices;
		
		public TranscriptFindingAction(String k, int i) {
			super(String.format("Call Transcripts", i));
			key = k;
			indices = new Integer[] { i };
		}
		public void actionPerformed(ActionEvent e) {
			dataPainter.findTranscripts(transParams, key, indices);
			dataPanel.repaint();
		}
	}
	
	public void showFrame() { 
		SwingUtilities.invokeLater(new Runnable() { 
			public void run() {
				setVisible(true);
				pack();
			}
		});
	}
}


