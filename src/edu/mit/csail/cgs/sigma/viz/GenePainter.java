package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.util.logging.*;
import java.awt.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;
import edu.mit.csail.cgs.ewok.verbs.RefGeneGenerator;
import edu.mit.csail.cgs.sigma.GeneGenerator;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.genes.GeneAnnotationProperties;
import edu.mit.csail.cgs.sigma.genes.GeneNameAssociation;

public class GenePainter extends RegionPainter {

	private GeneAnnotationProperties props;
	private GeneNameAssociation assoc;
	private SigmaProperties sprops;
	private Logger logger;
	private int geneHeight;
	private GeneGenerator geneGen;
	private Vector<Gene> genes;
	private Vector<GeneViz> viz;
	private String strain;
	
	private boolean paintGenes;;
	
	public GenePainter(GeneAnnotationProperties ps, String strain) {
		this.strain = strain;
		props = ps;
		assoc = props.getGeneNameAssociation(strain);
		sprops = props.getSigmaProperties();
		logger = ps.getLogger("GenePainter");
		geneGen = sprops.getGeneGenerator(strain);
		geneHeight = 15;
		genes = new Vector<Gene>();
		viz = null;
		paintGenes = true;
	}
	
	public Region lookupRegion(String name) { 
		Iterator<Gene> regions = geneGen.byName(name);
		Region r = regions.hasNext() ? regions.next() : null;
		return r;
	}
	
	public void setPaintGenes(boolean v) { paintGenes = v; }
	
	public void setRegion(Region r) { 
		super.setRegion(r);
        viz = null;

        genes.clear();
		Iterator<Gene> gi = geneGen.execute(region);
		while(gi.hasNext()) { 
			genes.add(gi.next());
		}
	}

	public void doLayout() {
        logger.log(Level.INFO, String.format("GenePainter.doLayout(), width=%dpx", width));
        
		if(viz == null) { viz = new Vector<GeneViz>(); }
		viz.clear();
		for(Gene g : genes) { 
			viz.add(new GeneViz(g));
		}
        
        logger.log(Level.INFO, String.format("GenePainter.doLayout() laid out %d genes.", viz.size()));
	}

	public boolean isReadyToPaint() {
		return viz != null;
	}

	public void paintRegion(Graphics2D g, int x1, int y1, int w, int h) {
		if(!paintGenes) { return; }
		
		logger.log(Level.INFO, String.format("GenePainter.paintRegion(%d,%d,%d,%d)", x1, y1, w, h));
        logger.log(Level.INFO, String.format("GenePainter Region: %s",
                (region == null ? "null" : region.getLocationString())));
		
		Stroke old = g.getStroke();
		g.setStroke(new BasicStroke((float)2.0));
		g.setColor(Color.black);

        for(GeneViz gv : viz) { 
            //logger.log(Level.INFO, String.format("Gene +%d -> %d pix", gv.gene.getStart(), gv.x));
            g.drawRect(x1+gv.x, y1+gv.y, gv.w, gv.h);
        }
        
		g.setStroke(old);

        for(GeneViz gv : viz) { 
        	String id = gv.gene.getID();
            String name = assoc.containsID(id) ? assoc.getName(id) : id;
            System.out.println("-> " + name);
            
            if(gv.gene.getStrand() == '+') { 
                g.drawString(name, x1+gv.x, y1+gv.y-2);
            } else { 
                g.drawString(name, x1+gv.x, y1+gv.y+gv.h*2);
            }
        }
        
	}
	
	private class GeneViz { 
		public Gene gene;
		public int x, y, w, h;
		
		public GeneViz(Gene g) { 
			gene = g;
			x = getXOffset(g.getStart());
			w = getXOffset(g.getEnd()) - x;
			int hh = height / 2;
			h = geneHeight;
			if(gene.getStrand() == '+') { 
				y = hh-geneHeight;
			} else { 
				y = hh;
			}
		}
	}
}
