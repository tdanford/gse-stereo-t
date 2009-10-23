/*
 * Author: tdanford
 * Date: Apr 29, 2009
 */
package edu.mit.csail.cgs.sigma.litdata.harbison;

import java.awt.*;
import javax.swing.*;

import edu.mit.csail.cgs.sigma.litdata.LitDataProperties;
import edu.mit.csail.cgs.utils.graphs.*;
import edu.mit.csail.cgs.viz.graphs.*;

import java.awt.event.*;
import java.io.IOException;

import java.util.*;

public class URNViewer extends JFrame {
	
	public static void main(String[] args) { 
		LitDataProperties lps = new LitDataProperties();
		String target = "YLL004W";
		
		try {
			HarbisonData data = new HarbisonData(lps);
			DirectedGraph graph = data.upstreamRegulatoryNetwork(target, 0.001);
			System.out.println(String.format("# Nodes: %d", graph.size()));
			URNViewer viewer = new URNViewer(graph);
			viewer.showFrame();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private DirectedGraph graph;
	private DirectedGraphAdapter adapter;
	private InteractiveGraphPanel panel;
	
	public URNViewer(DirectedGraph g) {
		super("URN Viewer");
		
		graph = g;
		adapter = new DirectedGraphAdapter(graph);
		panel = new URNPanel(adapter.getView());
		
		Container c = (Container)getContentPane();
		c.setLayout(new BorderLayout());
		
		c.add(panel, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(500, 600));
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
	
	public void showFrame() { 
		SwingUtilities.invokeLater(new Runnable() { 
			public void run() { 
				setVisible(true);
				pack();
			}
		});
	}

	private class URNPanel extends InteractiveGraphPanel {
		public URNPanel(GraphView v) {
			super(v);
		}
	}
}
