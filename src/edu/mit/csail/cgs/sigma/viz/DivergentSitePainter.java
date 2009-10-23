/*
 * Created on Feb 27, 2008
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma.viz;

import java.util.*;
import java.io.*;
import java.util.logging.*;
import java.awt.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.Genome;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.models.DivergentSite;

public class DivergentSitePainter extends RegionPainter {
    
    private BaseExpressionProperties props;
    private String strain, exptKey;
    private int dspWidth;
    private int siteHeight;
    private OverlappingRegionFinder<DivergentSite> sitefinder;
    
    private Vector<DivergentSite> sites;
    private Vector<SiteViz> viz;

    
    public DivergentSitePainter(BaseExpressionProperties eps, String ek) { 
        props = eps;
        siteHeight = 15;
        dspWidth = props.getDivergenceWindow();
        exptKey = ek;
        strain = props.parseStrainFromExptKey(exptKey);
        
        File dspfile = props.getDivergentSiteFile(exptKey, dspWidth);
        Genome genome = props.getSigmaProperties().getGenome(strain);
        Mapper<String,DivergentSite> decoder = new DivergentSite.DecodingMapper(genome);
        Mapper<DivergentSite,String> encoder = new DivergentSite.EncodingMapper();
        sitefinder = null;
        
        try {
            SavedFile<DivergentSite> dspSaved = new SavedFile<DivergentSite>(decoder, encoder, dspfile);
            Collection<DivergentSite> sitelist = dspSaved.getValues();
            sitefinder = new OverlappingRegionFinder<DivergentSite>(sitelist);
            
        } catch (IOException e) {
            //e.printStackTrace();
        }
        
        viz = null;
        sites = new Vector<DivergentSite>();
    }

    public void setRegion(Region r) { 
        super.setRegion(r);
        viz = null;
        
        sites.clear();
        if(sitefinder != null) { 
        	sites.addAll(sitefinder.findOverlapping(region));
        }
    }
    
    @Override
    public void doLayout() {
        if(viz == null) { viz = new Vector<SiteViz>(); }
        viz.clear();
        
        for(DivergentSite s : sites) { 
            viz.add(new SiteViz(s));
        }
    }

    @Override
    public boolean isReadyToPaint() {
        return viz != null;
    }

    @Override
    public void paintRegion(Graphics2D g, int x1, int y1, int w, int h) {
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke((float)2.0));
        g.setColor(Color.red);

        for(SiteViz gv : viz) { 
            g.drawOval(x1+gv.x, y1+gv.y, gv.w, gv.h);
        }
        
        g.setStroke(old);
    }

    private class SiteViz { 

        public DivergentSite site;
        public int x, y, w, h;
        
        public SiteViz(DivergentSite g) { 
            site = g;
            x = getXOffset(g.getStart());
            w = getXOffset(g.getEnd()) - x;
            int hh = height / 2;
            h = siteHeight;
            y = hh-siteHeight/2;
        }
    }

}
