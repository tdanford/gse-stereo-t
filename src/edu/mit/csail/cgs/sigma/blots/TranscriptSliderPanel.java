/*
 * Author: tdanford
 * Date: Jul 15, 2008
 */
package edu.mit.csail.cgs.sigma.blots;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class TranscriptSliderPanel extends JPanel {
	
	public static void main(String[] args) { 
		TranscriptSliderPanel tsp = 
			new TranscriptSliderPanel(1000, 500,  
					new int[] { 250, 300, 350, 400, 450, 500 });
		TranscriptSliderPanel.Frame f = new TranscriptSliderPanel.Frame(tsp);
		SwingUtilities.invokeLater(f);
	}
	
	private int regionWidth, probeOffset;
	private int[] lengths;
	private int[] offsets;
	
	private int[] blockx, blockwidths;
	
	private Integer selected, baseOffset, baseX, dragX;
	private LinkedList<ActionListener> listeners;
	
	public TranscriptSliderPanel(int w, int poffset, int[] lens) { 
		regionWidth = w;
		probeOffset = poffset;
		lengths = lens.clone();
		
		listeners = new LinkedList<ActionListener>();
		
		offsets = new int[lengths.length];
		for(int i = 0; i < lengths.length; i++) { 
			offsets[i] = probeOffset - lengths[i]/2;
		}
		
		blockx = new int[lengths.length];
		blockwidths = new int[lengths.length];
		
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				baseOffset =null;
				selected = baseX = dragX = null;
				relayout();
			}
			public void componentShown(ComponentEvent e) {
				baseOffset = null;
				selected = baseX = dragX = null;
				relayout();
			}
		});
		
		addMouseListener(new MouseAdapter() {
			
			public void mousePressed(MouseEvent e) { 
				if(e.getButton() == MouseEvent.BUTTON1) { 
					int y = e.getY();
					int trackHeight = Math.max(1, getHeight()/lengths.length);
					int track = y / trackHeight;
					
					selected = track;
					baseOffset = offsets[selected];
					baseX = e.getX();
					dragX = e.getX();
				}
			}
			
			public void mouseReleased(MouseEvent e) { 
				if(e.getButton() == MouseEvent.BUTTON1) {
					selected = null;
					baseOffset = null;
					baseX = dragX = null;
				}
			}
		});
		
		addMouseMotionListener(new MouseMotionAdapter() {
			
			public void mouseDragged(MouseEvent e) { 
				if(selected != null) {
					dragX = e.getX();
			
					int dx = dragX - baseX;
					int dbp = offsetToXPix(xPixToOffset(dx));
					
					int minOffset = probeOffset - lengths[selected];
					int maxOffset = probeOffset;
					
					int offset = Math.min(maxOffset, baseOffset + dbp);
					offset = Math.max(minOffset, offset);
					
					blockx[selected] = offsetToXPix(offset); 
					offsets[selected] = offset;
					
					repaint();
					dispatchActionEvent(selected);
				}
			}
		});
	}
	
	public void addActionListener(ActionListener l) { 
		listeners.add(l);
	}
	
	public void removeActionListener(ActionListener l) { 
		listeners.remove(l);
	}
	
	private void dispatchActionEvent(int idx) { 
		ActionEvent e = new TranscriptAdjustedEvent(this, idx);
		for(ActionListener l : listeners) { 
			l.actionPerformed(e);
		}
	}
	
	public int getNumBlocks() { return lengths.length; }
	public int getBlockLength(int i) { return lengths[i]; }
	public int getBlockOffset(int i) { return offsets[i]; }

	private void relayout() { 
		for(int i = 0; i < lengths.length; i++) { 
			blockx[i] = offsetToXPix(offsets[i]);
			blockwidths[i] = offsetToXPix(offsets[i] + lengths[i]) - blockx[i];
		}
	}
	
	private int xPixToOffset(int x) { 
		int width = Math.max(1, getWidth());
		double f = (double)x / (double)width;
		return (int)Math.round(f * (double)regionWidth);
	}
	
	private int offsetToXPix(int offset) { 
		int width = Math.max(1, getWidth());
		double f = (double)offset / (double)regionWidth;
		return (int)Math.round(f * (double)width);
	}
	
	protected void paintComponent(Graphics g) { 
		super.paintComponent(g);
		
		int w = getWidth(), h = getHeight();
		
		g.setColor(Color.white);
		g.fillRect(0, 0, w, h);
		
		int trackHeight = h / lengths.length;
		
		int y = 0;
		for(int i = 0; i < blockx.length; i++, y += trackHeight) {
			/*
			System.out.println(String.format("i: %d", i));
			System.out.println(String.format("\tLength %d, Offset %d", lengths[i], offsets[i]));
			System.out.println(String.format("\tX %d, Width %d", blockx[i], blockwidths[i]));
			*/
			g.setColor(Color.blue);
			g.fillRect(blockx[i], y, blockwidths[i], trackHeight);
			g.setColor(Color.black);
			g.drawRect(blockx[i], y, blockwidths[i], trackHeight);
		}
		
		g.setColor(Color.red);
		int probeX = offsetToXPix(probeOffset);
		g.drawLine(probeX, 0, probeX, h);
	}
	
	public static class Frame extends JFrame implements Runnable {
		
		private TranscriptSliderPanel panel;
		
		public Frame(TranscriptSliderPanel tsp) {  
			super("Transcripts");
			
			Container c = (Container)getContentPane();
			c.setLayout(new BorderLayout());
			
			c.add(panel =  tsp, BorderLayout.CENTER);
			panel.setPreferredSize(new Dimension(600, 100));
			
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		
		public void run() { 
			setVisible(true);
			pack();
		}
	}
		
}
