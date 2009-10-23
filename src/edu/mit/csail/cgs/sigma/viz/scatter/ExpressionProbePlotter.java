/*
 * Author: tdanford
 * Date: May 15, 2008
 */
package edu.mit.csail.cgs.sigma.viz.scatter;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import edu.mit.csail.cgs.sigma.*;
import edu.mit.csail.cgs.sigma.expression.*;
import edu.mit.csail.cgs.sigma.expression.ewok.ExpressionProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.ewok.StandardTwoChannelProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.ewok.StandardProbeGenerator;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionTwoChannelProbe;
import edu.mit.csail.cgs.sigma.genes.*;

import edu.mit.csail.cgs.datasets.general.*;
import edu.mit.csail.cgs.datasets.species.*;

import edu.mit.csail.cgs.utils.*;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.data.DataFrame;
import edu.mit.csail.cgs.utils.models.data.DataRegression;
import edu.mit.csail.cgs.viz.paintable.*;
import edu.mit.csail.cgs.ewok.verbs.*;

/**
 * Plots probes in a two-dimensional scatter plot -- fg vs bg intensities.
 * 
 * Allows for gene-by-gene selection of probes, to highlight particular expression 
 * patterns.  This was largely used for a couple of diagrams for Robin and Stacie.  
 * 
 * @author tdanford
 */
public class ExpressionProbePlotter {
    
    public static void main(String[] args) { 
        BaseExpressionProperties props = new SudeepExpressionProperties();
        
        String ek1 = "original_sigma_mat_alpha";
        String ek2 = "replicate_sigma_mat_alpha";
        
        GeneAnnotationProperties gps = new GeneAnnotationProperties();
        ExpressionProbePlotter plotter = new ExpressionProbePlotter(props, gps);
        plotter.loadData(ek1, ek2);
        
        double r2 = plotter.calculateR2();
        System.out.println(String.format("R2: %.4f", r2));
        
        JFrame f = plotter.asFrame();
    }

    private BaseExpressionProperties exprProps;
    private GeneAnnotationProperties gaProps;
    
    private String exptKey, strain;
    private Genome genome;
    private GeneGenerator gener;

    private Vector<ProbePoint> points;
    private PaintableScale scale;
    
    public ExpressionProbePlotter(BaseExpressionProperties eps, GeneAnnotationProperties gps) { 
        exprProps = eps;
        gaProps = gps;
        points = new Vector<ProbePoint>();
        scale = new PaintableScale(0.0, 1.0);
    }
    
    public double calculateR2() { 
    	DataFrame<ProbePoint> frame = new DataFrame<ProbePoint>(ProbePoint.class, points);
    	String model = "fg ~ bg + 1";
    	DataRegression<ProbePoint> reg = new DataRegression<ProbePoint>(frame, model);
    	reg.calculate();
    	double r2 = reg.getR2();
    	return r2;
    }
    
    /**
     * Loads the foreground channel from two different experiments.
     * @param ek1
     * @param ek2
     */
    public void loadData(String ek1, String ek2) { 
    	ExpressionProbeGenerator gen1 = new StandardProbeGenerator(exprProps, ek1);
    	ExpressionProbeGenerator gen2 = new StandardProbeGenerator(exprProps, ek2);
    	loadData(gen1, gen2, ek1);
    }
    
    /**
     * Loads the foreground channel from two different experiments.
     * 
     * @param gen1
     * @param gen2
     * @param ek
     */
    public void loadData(ExpressionProbeGenerator gen1, ExpressionProbeGenerator gen2, String ek) { 
        exptKey = ek;
        strain = exprProps.parseStrainFromExptKey(exptKey);
        genome = exprProps.getGenome(strain);    	
        
        gener = exprProps.getGeneGenerator(strain);
        points.clear();
        
        GenomeExpander<ExpressionProbe> g1 = new GenomeExpander<ExpressionProbe>(gen1);
        GenomeExpander<ExpressionProbe> g2 = new GenomeExpander<ExpressionProbe>(gen2);
        
        OverlappingPointFinder<ExpressionProbe> g2Probes = new OverlappingPointFinder<ExpressionProbe>(g2.execute(genome));
        Iterator<ExpressionProbe> g1Probes = g1.execute(genome);
        
        while(g1Probes.hasNext()) { 
        	ExpressionProbe p1 = g1Probes.next();
        	ExpressionProbe p2 = g2Probes.findOneOverlapping(p1);
        	if(p2 != null) { 
        		Double fg = p1.meanlog();
        		Double bg = p2.meanlog();
            	ProbePoint pp = new ProbePoint(p1, fg, bg);
                if(!Double.isNaN(pp.fg) && !Double.isNaN(pp.bg)) { 
                	addProbePoint(pp);
                }
        	}
        }
    }
    
    /**
     * Loads data for foreground/background plotting.
     * @param ek
     */
    public void loadData(String ek) { 
        exptKey = ek;
        strain = exprProps.parseStrainFromExptKey(exptKey);
        genome = exprProps.getGenome(strain);
        StandardTwoChannelProbeGenerator prober = new StandardTwoChannelProbeGenerator(exprProps, exptKey);
     
        gener = exprProps.getGeneGenerator(strain);
        
        points.clear();
        GenomeExpander<ExpressionTwoChannelProbe> genomeProber = 
            new GenomeExpander<ExpressionTwoChannelProbe>(prober);
        
        Set<String> chroms = new HashSet<String>();
        
        Iterator<ExpressionTwoChannelProbe> probes = genomeProber.execute(genome);
        while(probes.hasNext()) { 
            ExpressionTwoChannelProbe probe = probes.next();
            if(!chroms.contains(probe.getChrom())) { 
                chroms.add(probe.getChrom());
                System.out.println(String.format("Expanding: %s", probe.getChrom()));
            }
            
            ProbePoint pp = new ProbePoint(probe);

            if(!Double.isNaN(pp.fg) && !Double.isNaN(pp.bg)) { 
            	addProbePoint(pp);
            }
        }
        
        prober.close();
    }
    
    private void addProbePoint(ProbePoint pp) { 
    	scale.updateScale(pp.fg*1.1);
    	scale.updateScale(pp.bg*1.1);
        points.add(pp);    	
    }
    
    public Set<Integer> findGeneSet(String name) { 
        TreeSet<Integer> inds = new TreeSet<Integer>();
        Iterator<Gene> gitr = gener.byName(name);
        LinkedList<Gene> genes = new LinkedList<Gene>();
        while(gitr.hasNext()) { genes.add(gitr.next()); }
        for(int i = 0; i < points.size(); i++) { 
            if(points.get(i).isInList(genes)) { 
                inds.add(i);
            }
        }
        return inds;
    }
    
    public void closeData() { 
        genome = null;
        exptKey = strain = null;
        points.clear();
    }
    
    public PlotterPaintable getPaintable() { return new PlotterPaintable(); }
    public JFrame asFrame() { return new PlotterFrame(); }
    
    public class PlotterFrame extends JFrame {
    	
    	private PlotterPanel panel;
    	
    	public PlotterFrame() { 
    		super("Plotter");
    		
    		panel = new PlotterPanel();
    		
    		Container c = (Container)getContentPane();
    		c.setLayout(new BorderLayout());
    		c.add(panel, BorderLayout.CENTER);
    		panel.setPreferredSize(new Dimension(400, 400));
    		
    		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    		
    		SwingUtilities.invokeLater(new Runnable() { 
    			public void run() { 
    				setVisible(true);
    				pack();
    			}
    		});
    	}
    }
    
    public class PlotterPanel extends JPanel implements PaintableChangedListener {
    	
    	private PlotterPaintable plotter;
    	private Paintable dbp;
    	private java.awt.Point pressed, released;
    	
    	public PlotterPanel() { 
    		plotter = new PlotterPaintable();
    		dbp = new DoubleBufferedPaintable(plotter);
    		plotter.addPaintableChangedListener(this);
    		pressed = released = null;
    		
    		addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if(e.getButton() == MouseEvent.BUTTON1) { 
						pressed = e.getPoint();
						released = pressed;
					}
				}

				public void mouseReleased(MouseEvent e) {
					if(pressed != null) { 
						released = e.getPoint(); 
						if(released.x != pressed.x || released.y != pressed.y) { 
							Rectangle r = new Rectangle(Math.min(pressed.x, released.x), 
									Math.min(pressed.y, released.y), 
									Math.abs(pressed.x - released.x), 
									Math.abs(pressed.y - released.y));
							released = pressed = null;
							plotter.setSelected(r);
						}
					}
				} 
    		});
    		
    		addMouseMotionListener(new MouseMotionAdapter() { 
    			public void mouseDragged(MouseEvent e) { 
    				if(pressed != null) { 
    					released = e.getPoint();
    					repaint();
    				}
    			}
    		});
    	}
    	
    	protected void paintComponent(Graphics g) { 
    		int w = getWidth(), h = getHeight();
    		dbp.paintItem(g, 0, 0, w, h);
    		
    		g.setColor(Color.red);
    		if(pressed != null && released != null) { 
    			int x = Math.min(pressed.x, released.x);
    			int y = Math.min(pressed.y, released.y);
    			int rw = Math.abs(pressed.x - released.x);
    			int rh = Math.abs(pressed.y - released.y);
    			g.drawRect(x, y, rw, rh);
    		}
    	}

		public void paintableChanged(PaintableChangedEvent pce) {
			repaint();
		}
    }
    
    public class PlotterPaintable extends AbstractPaintable {
        
        private int prevDim;
        private LinkedList<PaintablePoint> pts;
        private Set<Integer> selected;
        
        public PlotterPaintable() {
            prevDim = 0;
            pts = new LinkedList<PaintablePoint>();
            selected = new TreeSet<Integer>();
        }
        
        public void setSelected(String name) { 
            selected.clear();
            selected.addAll(findGeneSet(name));
            dispatchChangedEvent();
        }
        
        private Collection<Integer> findRectangleSet(Rectangle r) {
        	ArrayList<Integer> lst = new ArrayList<Integer>();
        	for(PaintablePoint pp : pts) { 
        		if(r.contains(new java.awt.Point(pp.x, pp.y))) { 
        			lst.add(pp.idx);
        		}
        	}
        	return lst;
        }
        
        public void setSelected(Rectangle rect) { 
        	selected.clear();
        	selected.addAll(findRectangleSet(rect));
        	dispatchChangedEvent();
        }
        
        public int pix(double value, int maxpix) { 
        	double f = scale.fractionalOffset(value);
            return (int)Math.round(f * maxpix);
        }
        
        public void layout(int dim) { 
            pts.clear();
            for(int i = 0; i < points.size(); i++) { 
                ProbePoint pp = points.get(i);
                int x = pix(pp.bg, dim);
                int y = pix(pp.fg, dim);
                pts.add(new PaintablePoint(x, y, i));
            }
            prevDim = dim;
        }

        public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
            int w= x2 - x1, h = y2 - y1;
            g.setColor(Color.white);
            g.fillRect(x1, y1, w, h);
            
            int dim = Math.min(w-w/5, h-h/5);
            
            if(prevDim <= 0 || prevDim != dim) { 
                layout(dim);
            }
            
            int xbase = x1 + (w - dim), ybase = y2 - (h - dim);
            
            int rad = 2;
            int diam = rad * 2;
            
            LinkedList<PaintablePoint> sel = new LinkedList<PaintablePoint>();
            
            Color c = Color.blue;
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 15);
            
            g.setColor(c);
            for(PaintablePoint pp : pts) { 
                int x = xbase + pp.x;
                int y = ybase - pp.y;
                
                if(selected.contains(pp.idx)) { 
                    sel.addLast(pp);
                } else { 
                    g.fillOval(x-rad, y-rad, diam, diam);
                }
            }

            g.setColor(Color.red);
            for(PaintablePoint pp : sel) { 
                int x = xbase + pp.x;
                int y = ybase - pp.y;
                
                g.fillOval(x-rad, y-rad, diam, diam);
            }

            g.setColor(Color.pink);
            g.drawLine(xbase, ybase, xbase, ybase-dim);
            g.drawLine(xbase, ybase, xbase+dim, ybase);

            g.setColor(Color.pink);
            g.drawLine(xbase, ybase, xbase+dim, ybase-dim);
        } 
    }
    
    private static class PaintablePoint {
        
        public int x, y, idx;
        
        public PaintablePoint(int x, int y, int i) { 
            this.x = x; 
            this.y = y;
            idx = i;
        }
    }
    
    public static class ProbePoint extends Model {
        
        public Double fg, bg;
        public StrandedPoint probe;
        private int hashCode;
        
        public ProbePoint(ExpressionTwoChannelProbe p) { 
            probe = p;
            /*
            fg = p.mean(true);
            bg = p.mean(false);
            */
            fg = p.meanlog(true);
            bg = p.meanlog(false);
            
            calculateHash();
        }
        
        public ProbePoint(StrandedPoint p, double fg, double bg) { 
            probe = p;
            this.fg = fg;
            this.bg = bg;
            
            calculateHash();
        }
        
        public boolean isInList(Collection<? extends StrandedRegion> regions) { 
            for(StrandedRegion r : regions) { 
                if(r.contains(probe) && r.getStrand() == probe.getStrand()) { 
                    return true;
                }
            }
            return false;
        }
        
        private void calculateHash() { 
            hashCode = probe.hashCode();
        }
        
        public int hashCode() { return hashCode; }
        
        public boolean equals(Object o) { 
            if(!(o instanceof ProbePoint)) { return false; }
            ProbePoint pp = (ProbePoint)o;
            return pp.probe.equals(probe);
        }
    }
}
