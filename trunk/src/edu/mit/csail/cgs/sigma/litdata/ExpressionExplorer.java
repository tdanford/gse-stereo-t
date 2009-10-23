/*
 * Author: tdanford
 * Date: Mar 4, 2009
 */
package edu.mit.csail.cgs.sigma.litdata;

import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;
import edu.mit.csail.cgs.sigma.genes.GeneNameAssociation;
import edu.mit.csail.cgs.sigma.viz.scatter.DynamicRegressionPainter;

/**
 * Quick-and-dirty Swing components, built to allow us to explore the 
 * data provided by a MicroarrayExpression component. 
 * 
 * @author tdanford
 *
 */
public class ExpressionExplorer {

	public static void main(String[] args) { 
		LitDataProperties props =new LitDataProperties();
		GeneAnnotationProperties gprops = new GeneAnnotationProperties();
		ExplorerFrame frame = new ExplorerFrame(props, gprops, "PET10", "ARG3");
	}
	
	public static class ExplorerFrame extends JFrame {
		
		private ExpressionExplorer explorer;
		private String s1, s2;
		private JButton next;
		
		private DynamicRegressionPainter.DynamicRegressionPanel panel;
		
		public DynamicRegressionPainter getPainter() { return panel.getPainter(); } 
		
		public ExplorerFrame(LitDataProperties lprops, GeneAnnotationProperties gprops, 
				String s1, String s2) { 
			super("Expression");
			
			DynamicRegressionPainter painter = new DynamicRegressionPainter();
			
			this.s1 = s1; this.s2 = s2;
			explorer = new ExpressionExplorer(gprops, lprops, painter);
			
			Container c = (Container)getContentPane();
			c.setLayout(new BorderLayout());
			
			c.add(panel = new DynamicRegressionPainter.DynamicRegressionPanel(painter), BorderLayout.CENTER);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			JPanel buttons = new JPanel(); 
			buttons.setLayout(new FlowLayout());
			buttons.add(next = new JButton("Next"));
			next.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					next();
				}
			});
			
			c.add(buttons, BorderLayout.SOUTH);
			
			SwingUtilities.invokeLater(new Runnable() { 
				public void run() { 
					setVisible(true);
					pack();					
				}
			});
			
			explorer.explore(s1, s2);
		}
		
		public void next() { 
			s2 = explorer.chooseName();
			explorer.explore(s1, s2);
		}
	}
	
	private GeneAnnotationProperties props;
	private GeneNameAssociation assoc;
	private MicroarrayExpression expr;
	private DynamicRegressionPainter painter;
	
	public ExpressionExplorer(GeneAnnotationProperties gps, LitDataProperties ps, DynamicRegressionPainter p) {
		props = gps;
		assoc = props.getGeneNameAssociation("s288c");
		painter = p;
		expr = new GeneNameAssociationAveraging(assoc, ps.getDefaultExpression());
	}
	
	public String chooseName() { 
		Random rand = new Random();
		Collection<String> ids = assoc.allNames();
		int s = rand.nextInt(ids.size());
		int i = 0;
		Iterator<String> itr = ids.iterator();
		String ss = null;
		while(itr.hasNext() && i < s) { 
			i += 1;
			ss = itr.next();
		}
		return ss;
	}
	
	public void explore(String s1, String s2) {
		MicroarrayProbe p1 = expr.expression(s1), p2 = expr.expression(s2);
		painter.disableRegressions();
		painter.clear();
		
		ArrayList<Double> v1s = new ArrayList<Double>();
		ArrayList<Double> v2s = new ArrayList<Double>();
		double v1sum = 0.0, v2sum = 0.0;
		
		for(int i = 0; p1 != null && p2 != null &&
					i < Math.min(p1.values.length, p2.values.length); i++) { 
			Double v1 = p1.values[i], v2 = p2.values[i];
			if(v1 != null && v2 != null) { 
				v1s.add(v1);
				v2s.add(v2);
				v1sum += v1; 
				v2sum += v2;
			}
		}
		
		v1sum /= (double)Math.max(1, v1s.size());
		v2sum /= (double)Math.max(1, v2s.size());
		for(int i = 0; i < v1s.size(); i++) { 
			double mv1 = v1s.get(i) - v1sum;
			double mv2 = v2s.get(i) - v2sum;
			//double mv1 = v1s.get(i);
			//double mv2 = v2s.get(i);
			painter.addDatapoint(mv1, mv2);
		}
		
		painter.matchBounds();
		painter.enableRegressions();
		System.out.println(String.format("Explored: %s, %s", s1, s2));
	}
}
