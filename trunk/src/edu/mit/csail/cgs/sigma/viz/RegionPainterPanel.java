package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

import javax.swing.*;

import edu.mit.csail.cgs.datasets.general.Region;

public class RegionPainterPanel extends JPanel {
	
	private Region region;
	private SigmaRegionPainter sigmaPainter;
	
	public RegionPainterPanel(SigmaRegionPainter srp, Region r) { 
		super();
		setPreferredSize(new Dimension(300, 200));
		sigmaPainter = srp;
		region = r;
		
		addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent e) { 
				if(e.getButton() == MouseEvent.BUTTON1) { 
                    sigmaPainter.clearSelections();
					sigmaPainter.makeSelection(e.getPoint());
					repaint();
				} else {
					
					ExpressionPainter epp = sigmaPainter.getExpressionPainter(e.getPoint());
					
					JPopupMenu menu = new JPopupMenu("Popup Menu");
					menu.add(new JMenuItem(new AbstractAction("Clear Selections") { 
						public void actionPerformed(ActionEvent e) { 
		                    sigmaPainter.clearSelections();
							repaint();							
						}
					}));
					menu.add(sigmaPainter.createChangeLabelAction(epp));
					
					menu.show(RegionPainterPanel.this, e.getX(), e.getY());
				}
			}
		});
	}
	
	public SigmaRegionPainter getSigmaPainter() { return sigmaPainter; }
    
    public void saveImage(File f) { 
        try {
            sigmaPainter.saveImage(f, getWidth(), getHeight(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Region getRegion() { return region; }
	
	public void zoomOut() { 
		int diff = Math.min(region.getStart(), region.getWidth()/2);
        int maxRight = region.getGenome().getChromLength(region.getChrom());
        
        int leftDiff = diff;
        int rightDiff = diff;

        if(leftDiff > region.getStart()) { 
            leftDiff = region.getStart();
            rightDiff += diff - leftDiff;
        }
        
        if(region.getEnd() + rightDiff > maxRight) { 
            rightDiff = maxRight - region.getEnd();
        }
        
		Region r = new Region(region.getGenome(), region.getChrom(), 
				region.getStart()-leftDiff, region.getEnd()+rightDiff);
		setRegion(r);
	}
	
	public void zoomIn() { 
		int diff = region.getWidth() >= 200 ? region.getWidth()/4 : 0;
		Region r = new Region(region.getGenome(), region.getChrom(), 
				region.getStart()+diff, region.getEnd()-diff);
		setRegion(r);		
	}
    
    public void nextChrom() { 
        java.util.List<String> chroms = region.getGenome().getChromList();
        int idx = chroms.indexOf(region.getChrom());
        if(idx < chroms.size()-1) { 
            String nextChrom = chroms.get(idx+1);
            Region r = new Region(region.getGenome(), nextChrom, 0, 10000);
            setRegion(r);
        }        
    }
    
    public void prevChrom() { 
        java.util.List<String> chroms = region.getGenome().getChromList();
        int idx = chroms.indexOf(region.getChrom());
        if(idx > 0) { 
            String nextChrom = chroms.get(idx-1);
            Region r = new Region(region.getGenome(), nextChrom, 0, 10000);
            setRegion(r);
        }
    }
	
	public void shiftRight() { 
		int skip = region.getWidth()/3;
        int maxRight = region.getGenome().getChromLength(region.getChrom());
        
        if(region.getEnd() + skip > maxRight) { 
            skip = maxRight - region.getEnd();
        }
        
		setRegion(new Region(region.getGenome(), region.getChrom(), 
                region.getStart()+skip, region.getEnd()+skip));
	}
	
	public void shiftLeft() { 
		int skip = region.getWidth()/3;
        if(skip > region.getStart()) { 
            skip = region.getStart();
        }
		setRegion(new Region(region.getGenome(), region.getChrom(), 
                region.getStart()-skip, region.getEnd()-skip));
	}
	
	public void setRegion(Region r) {
		System.out.println("-> " + r.getLocationString());
		region = r;
		sigmaPainter.setRegion(r);
		repaint();
	}
	
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		int w = getWidth(), h = getHeight();
        System.out.println(String.format("paintComponent(): %d x %d pixels", w, h));
		
		sigmaPainter.paintItem(g, 0, 0, w, h);
	}
}
