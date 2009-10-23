/*
 * Author: tdanford
 * Date: May 12, 2008
 */
package edu.mit.csail.cgs.sigma.blots;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

public class BlotViewPanel extends JPanel {
	
	private Blot blot;
	private Vector<AffineLine> pendingLines;
    
	// for dynamic line entry.
	private Point p1, p2;
	private LinkedList<AffineQuadrangle> quads;
    private LinkedList<AffineLine> lines;
    private LinkedList<BlotSpot> spots;
	
	public BlotViewPanel(Blot b) {
		super();
		blot = b;
		setPreferredSize(new Dimension(blot.getWidth(), blot.getHeight()));
		p1 = p2 = null;
		pendingLines = new Vector<AffineLine>();
		quads = new LinkedList<AffineQuadrangle>();
        
        lines = new LinkedList<AffineLine>();
        spots = new LinkedList<BlotSpot>();
		
		addMouseListener(new MouseAdapter() { 
			public void mousePressed(MouseEvent e) { 
				if(e.getButton() == MouseEvent.BUTTON1) { 
					p1 = e.getPoint();
					p2 = null;
				}
				repaint();
			}
			
			public void mouseReleased(MouseEvent e) { 
				if(p2 != null) { 
                    pendingLines.add(new AffineLine(p1, p2));
				}
				
				p1 = p2 = null;
				repaint();
			}
		});
		
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) { 
				if(p1 != null) {
					p2 = e.getPoint();
					repaint();
				}
			}
		});
	}

	public void setBlot(Blot b) {
		blot = b;
		setPreferredSize(new Dimension(blot.getWidth(), blot.getHeight()));
		pendingLines.clear();
		p1 = p2 = null;
		repaint();
	}

	public Blot getBlot() { return blot; }
	
	public void addLastAsColumn(String desc) {
		AffineLine lastLine = pendingLines.remove(pendingLines.size()-1);
		addBlotColumn(lastLine, desc);
		repaint();
	}
	
	public void addLastAsRow(String desc) { 
		AffineLine lastLine = pendingLines.remove(pendingLines.size()-1);
		addBlotRow(lastLine, desc);
		repaint();
	}
	
    private void addBlotColumn(AffineLine line, String description) { 
        BlotColumn newCol = new BlotColumn(blot, description, line); 
        blot.addColumn(newCol);
        
        for(AffineLine line2 : lines) { 
            BlotSpot s = new BlotSpot(line.intersection(line2));
            spots.add(s);            
        }
    }

    private void addBlotRow(AffineLine line, String description) {
        lines.addLast(line);
        for(int i = 0; i < blot.columns(); i++) { 
            AffineLine line2 = blot.getColumn(i).getLine();
            BlotSpot s = new BlotSpot(line.intersection(line2));
            spots.add(s);
        }
    }
    
    public void addQuad() { 
    	if(pendingLines.size() >= 2) { 
    		AffineLine l1 = pendingLines.remove(pendingLines.size()-1);
    		AffineLine l2 = pendingLines.remove(pendingLines.size()-1);
    		Point p1 = l1.getP1(), p2 = l1.getP2();
    		Point p3 = l2.getP1(), p4 = l2.getP2();
    		AffineQuadrangle quad = new AffineQuadrangle(p1, p2, p3, p4);
    		quads.add(quad);
    		repaint();
    	}
    }

	protected void paintComponent(Graphics g) { 
		super.paintComponent(g);
		paintBlot(g);
	}
	
	private void paintBlot(Graphics g) { 
		g.drawImage(blot.getImage(), 0, 0, blot.getWidth(), blot.getHeight(), null);
		paintLines(g);
		paintSpots(g);
		paintQuads(g);
	}
	
	private void paintQuads(Graphics g) { 
		Graphics2D g2 = (Graphics2D)g;
		Stroke oldStroke = g2.getStroke();
		g2.setStroke(new BasicStroke((float)2.0));
		
		g2.setColor(Color.cyan);
		int[] x = new int[4];
		int[] y = new int[4];
		
		for(AffineQuadrangle quad : quads) { 
			Point[] verts = quad.getVertices();
			for(int i = 0; i < 4; i++) { 
				x[i] = verts[i].x; 
				y[i] = verts[i].y;
			}
			g2.drawPolygon(x, y, 4);
		}

		g2.setStroke(oldStroke);
	}

	private void paintSpots(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		Stroke oldStroke = g2.getStroke();
		g2.setStroke(new BasicStroke((float)2.0));
        
        int radius = 8;
        int diam = radius * 2;

		g2.setColor(Color.orange);
		for(BlotSpot spot : spots) { 
			Point p = spot.getPoint();
			g2.drawOval(p.x-radius, p.y-radius, diam, diam);
		}
		
		g2.setStroke(oldStroke); 
	}

	private void paintLines(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		Stroke oldStroke = g2.getStroke();
		g2.setStroke(new BasicStroke((float)2.0));
		int rad = 3;
		int diam = rad * 2;
		
		g2.setColor(Color.red);
		for(int i = 0; i < blot.columns(); i++) { 
			BlotColumn c = blot.getColumn(i);
			AffineLine cline = c.getLine();
			Point c1 = cline.getP1(), c2 = cline.getP2();
			g2.drawOval(c1.x-rad, c1.y-rad, diam, diam);
			g2.drawOval(c2.x-rad, c2.y-rad, diam, diam);
			g2.drawLine(c1.x, c1.y, c2.x, c2.y);
		}
		
		g2.setColor(Color.blue);
		for(AffineLine cline : lines) { 
            Point c1 = cline.getP1(), c2 = cline.getP2();
            g2.drawOval(c1.x-rad, c1.y-rad, diam, diam);
            g2.drawOval(c2.x-rad, c2.y-rad, diam, diam);
            g2.drawLine(c1.x, c1.y, c2.x, c2.y);
		}
        
		for(AffineLine lastLine : pendingLines) { 
            g2.setColor(Color.green);
            Point c1 = lastLine.getP1(), c2 = lastLine.getP2();
            g2.drawOval(c1.x-rad, c1.y-rad, diam, diam);
            g2.drawOval(c2.x-rad, c2.y-rad, diam, diam);
            g2.drawLine(c1.x, c1.y, c2.x, c2.y);      
        }
		
		if(p2 != null) { 
            g2.setColor(Color.yellow);
            g2.drawOval(p1.x-rad, p1.y-rad, diam, diam);
            g2.drawOval(p2.x-rad, p2.y-rad, diam, diam);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
		}
		
		g2.setStroke(oldStroke);
	}

	public void saveImage(File f) {
		int w = blot.getWidth(), h = blot.getHeight();
        BufferedImage im = 
            new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics g = im.getGraphics();
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
        paintBlot(g);
        try {
			ImageIO.write(im, "png", f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

class ZoomScale { 
	
	private int coordWidth, coordHeight;
	private int screenWidth, screenHeight;
	
	public ZoomScale(int cw, int ch, int sw, int sh) { 
		coordWidth = cw; 
		coordHeight = ch;
		screenWidth = sw; 
		screenHeight = sh;
	}
	
	public int xCoord2Screen(int x) { 
		double f = (double)x / (double)coordWidth;
		return (int)Math.round(f * (double)screenWidth);
	}
	
	public int yCoord2Screen(int y) { 
		double f = (double)y / (double)coordHeight;
		return (int)Math.round(f * (double)screenHeight);
	}
	
	public int xScreen2Coord(int x) { 
		double f = (double)x / (double)screenWidth;
		return (int)Math.round(f * (double)coordWidth);
	}
	
	public int yScreen2Coord(int y) { 
		double f = (double)y / (double)screenHeight;
		return (int)Math.round(f * (double)coordHeight);
	}
	
	public Point coords2Screen(Point p) { 
		return new Point(xCoord2Screen(p.x), yCoord2Screen(p.y));
	}

	public Point screen2Coords(Point p) { 
		return new Point(xScreen2Coord(p.x), yScreen2Coord(p.y));
	}
}
