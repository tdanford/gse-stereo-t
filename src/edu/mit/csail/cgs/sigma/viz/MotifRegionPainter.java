package edu.mit.csail.cgs.sigma.viz;

import java.awt.*;
import java.util.*;

import edu.mit.csail.cgs.sigma.motifs.Motifs;
import edu.mit.csail.cgs.viz.NonOverlappingLayout;
import edu.mit.csail.cgs.viz.colors.ColorSet;
import edu.mit.csail.cgs.viz.paintable.*;
import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.motifs.*;
import edu.mit.csail.cgs.ewok.verbs.motifs.*;

public class MotifRegionPainter extends RegionPainter {

	private Motifs motifHandler;
	private Map<String,Color> motifColors;
	private ColorSet colorSet;

	private Map<String,WeightMatrix> motifs;
	private Vector<WeightMatrixHit> sites;
	
    private NonOverlappingLayout wlayout, clayout;
	private Vector<VizSite> vizSites;
    private Set<String> vizNames;

    private double fraction;
	private int motifHeight;
    private int maxMotifWindow;
	
	public MotifRegionPainter(Motifs m) {
		motifHandler = m;
		colorSet = new ColorSet();
		motifColors = new TreeMap<String,Color>();
		motifs = new TreeMap<String,WeightMatrix>();
		sites = new Vector<WeightMatrixHit>();
		
		vizSites = null;
        wlayout = null;
        clayout = null;
        vizNames = new TreeSet<String>();
		motifHeight = 15;
        maxMotifWindow = 2500;
        fraction = 0.9;
	}
	
	public void paintRegion(Graphics2D g, int x1, int y1, int w, int h) {
		int hh = h/2;
		
		g.setColor(Color.black);
		g.drawLine(x1, y1+hh, x1+w, y1+hh); 

		for(VizSite site : vizSites) { 
			int offset = site.hit.getStart() - region.getStart();
			String name = String.format("%s+%d", site.hit.getMatrix().name, offset);
			
			g.setColor(motifColors.get(site.hit.getMatrix().name));
			g.fillRect(x1+site.x, y1+site.y, site.w, site.h);
            g.setColor(Color.black);
            
            if(site.hit.getStrand() == '+') { 
                int nameTrack = wlayout.getTrack(site.hit);
                g.drawString(name, x1+site.x, y1+site.y-(nameTrack*motifHeight)-2);
            } else { 
                int nameTrack = clayout.getTrack(site.hit);
                g.drawString(name, x1+site.x, y1+site.y+site.h + 
                        ((nameTrack+1)*motifHeight));                
            }
            
            /*
            System.out.println(String.format("%s -> %d px, %d bp",
                    site.hit.getMatrix().name, site.x, site.hit.getStart()));
            */
		}
        
		int spacing = 20;
		int offset = y1 + spacing;
		
        /*
		for(String motifName : vizNames) { 
			Color c = motifColors.get(motifName);
			g.setColor(c);
			g.fillRect(x1+10, offset-10, 10, 10);
			g.setColor(Color.black);
			g.drawString(motifName, x1+22, offset);
			offset += spacing;
		}
        */
	}
	
	public void addWeightMatrix(WeightMatrix m) { 
		motifs.put(m.name, m);
		findColor(m.name);
		vizSites = null;
		dispatchChangedEvent();
	}
	
	private void findColor(String name) { 
		motifColors.put(name, colorSet.getColor());
	}
	
	public boolean isReadyToPaint() { 
		return vizSites != null;
	}
	
	public void setRegion(Region r) {
		super.setRegion(r);
		
		vizSites = null;
		sites.clear();
        vizNames.clear();
		
		if(region.getWidth() <= maxMotifWindow) { 
		    for(String matrixName : motifs.keySet()) { 
		        WeightMatrix matrix = motifs.get(matrixName);
		        Collection<WeightMatrixHit> hits = motifHandler.findMotifs(matrix, r, fraction);
                if(hits.size() > 0) { vizNames.add(matrix.name); }
		        sites.addAll(hits);
		    }
		}

		dispatchChangedEvent();
	}
	
	public void doLayout() {
		if(vizSites == null) { 
            vizSites = new Vector<VizSite>();
            wlayout = new NonOverlappingLayout();
            clayout = new NonOverlappingLayout();
		}
        
		vizSites.clear();

        if(region != null) {
            LinkedList<WeightMatrixHit> whits = new LinkedList<WeightMatrixHit>();
            LinkedList<WeightMatrixHit> chits = new LinkedList<WeightMatrixHit>();
			for(WeightMatrixHit hit : sites) { 
				VizSite vs = new VizSite(hit);
				vizSites.add(vs);
                if(hit.getStrand() == '+') { 
                    whits.add(hit);
                } else { 
                    chits.add(hit);
                }
			}
            
            wlayout.setRegions(whits);
            clayout.setRegions(chits);
		}
	}
	
	private class VizSite { 
		public int x, y, w, h;
		public WeightMatrixHit hit;
		
		public VizSite(WeightMatrixHit ht) { 
			hit = ht;
			x = getXOffset(ht.getStart());
			w = getXOffset(ht.getEnd()) - x + 2;
            h = motifHeight;
            
			if(hit.getStrand() == '+') { 
				y = (height/2) - h;
			} else { 
				y = (height/2) + 1;
			}
		}
	}
}
