package edu.mit.csail.cgs.sigma.expression.ewok;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.datasets.chipchip.ChipChipData;
import edu.mit.csail.cgs.datasets.chipchip.SQLData;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionTwoChannelProbe;
import edu.mit.csail.cgs.utils.Closeable;
import edu.mit.csail.cgs.utils.NotFoundException;

public class StandardTwoChannelProbeGenerator 
    implements Expander<Region,ExpressionTwoChannelProbe>, Closeable, ExpressionTwoChannelProbeGenerator {
	
	private String exptKey;
	private BaseExpressionProperties props;
	private ChipChipLocator loc;
	private ChipChipData ipdata, wcedata;
	private boolean ipIP, wceIP;
	
    public StandardTwoChannelProbeGenerator(BaseExpressionProperties ps, String ek) { 
        exptKey = ek;
        props = ps;
        loc = props.getLocator(exptKey);
        ipdata = loc.createObject();
        ((SQLData)ipdata).setMaxCount(1);
        ipIP = props.isIPData(exptKey);
        wceIP = ipIP;

		//System.out.println(String.format("Loaded: \"%s\", \"%s\"", loc.getName(), loc.getVersion()));

        if(ek.endsWith("IME4")) { 
        	ChipChipLocator loc2 = props.getLocator("original_sigma_mat_a");
        	wceIP = props.isIPData("original_sigma_mat_a");
        	wcedata = loc2.createObject();
        } else { 
        	wcedata = ipdata;
        }
        
        if(wcedata != ipdata) { ((SQLData)wcedata).setMaxCount(1); }
    }
    
    public StandardTwoChannelProbeGenerator(BaseExpressionProperties ps, String ek, boolean fg) { 
        exptKey = ek;
        props = ps;
        loc = props.getLocator(exptKey);
        ipdata = loc.createObject();
        ((SQLData)ipdata).setMaxCount(1);
        ipIP = fg ? props.isIPData(exptKey) : !props.isIPData(exptKey);
        wceIP = ipIP;

		//System.out.println(String.format("Loaded: \"%s\", \"%s\"", loc.getName(), loc.getVersion()));

        if(ek.endsWith("IME4")) { 
        	ChipChipLocator loc2 = props.getLocator("original_sigma_mat_a");
        	wceIP = props.isIPData("original_sigma_mat_a");
        	wcedata = loc2.createObject();
        } else { 
        	wcedata = ipdata;
        }
        if(wcedata != ipdata) { ((SQLData)wcedata).setMaxCount(1); }
    }
    
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.ewok.ExpressionTwoChannelProbeGenerator#close()
	 */
	public void close() { 
		if(ipdata instanceof Closeable) { 
			((Closeable)ipdata).close();
			((Closeable)wcedata).close();
		}
		ipdata = wcedata = null;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.ewok.ExpressionTwoChannelProbeGenerator#isClosed()
	 */
	public boolean isClosed() { 
		return ipdata == null || wcedata == null;
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.ewok.ExpressionTwoChannelProbeGenerator#execute(edu.mit.csail.cgs.datasets.general.Region)
	 */
	public Iterator<ExpressionTwoChannelProbe> execute(Region a) {
		LinkedList<ExpressionTwoChannelProbe> probes = new LinkedList<ExpressionTwoChannelProbe>();
		try {
			ipdata.window(a.getChrom(), a.getStart(), a.getEnd());
			if(wcedata != ipdata) { wcedata.window(a.getChrom(), a.getStart(), a.getEnd()); }
			
			for(int i = 0; i < ipdata.getCount(); i++) { 
				int loc = ipdata.getPos(i);
				int reps = ipdata.getReplicates(i);
				
				/*
				 * First, collect the Watson-strand probes.
				 */
                LinkedList<Double> fgvalues = new LinkedList<Double>();
                LinkedList<Double> bgvalues = new LinkedList<Double>();
				for(int j = 0; j < reps; j++) {
					if(ipdata.getStrand(i, j) == '+') { 
                        double fgvalue = ipIP ? ipdata.getIP(i, j) : ipdata.getWCE(i, j);
                        double bgvalue = wceIP ? wcedata.getWCE(i, j) : wcedata.getIP(i, j);
                        fgvalues.addLast(fgvalue);
                        bgvalues.addLast(bgvalue);
					}
				}
                if(fgvalues.size() > 0 && bgvalues.size() > 0) { 
                    ExpressionProbe fgprobe = new ExpressionProbe(a.getGenome(), 
                            a.getChrom(), loc, '+', exptKey, fgvalues);
                    ExpressionProbe bgprobe = new ExpressionProbe(a.getGenome(), 
                            a.getChrom(), loc, '+', exptKey, bgvalues);
                    ExpressionTwoChannelProbe twoChannels = new ExpressionTwoChannelProbe(a.getGenome(),
                            a.getChrom(), loc, '+', exptKey, fgprobe, bgprobe);
                    probes.addLast(twoChannels);
                }

				/*
				 * Then, the Crick-strand probes.
				 */
				fgvalues.clear();
                bgvalues.clear();
                
				for(int j = 0; j < reps; j++) {
					if(ipdata.getStrand(i, j) == '-') { 
                        double fgvalue = ipIP ? ipdata.getIP(i, j) : ipdata.getWCE(i, j);
                        double bgvalue = wceIP ? wcedata.getWCE(i, j) : wcedata.getIP(i, j);
                        fgvalues.addLast(fgvalue);
                        bgvalues.addLast(bgvalue);
					}
				}
                if(fgvalues.size() > 0 && bgvalues.size() > 0) { 
                    ExpressionProbe fgprobe = new ExpressionProbe(a.getGenome(), 
                            a.getChrom(), loc, '-', exptKey, fgvalues);
                    ExpressionProbe bgprobe = new ExpressionProbe(a.getGenome(), 
                            a.getChrom(), loc, '-', exptKey, bgvalues);
                    ExpressionTwoChannelProbe twoChannels = new ExpressionTwoChannelProbe(a.getGenome(),
                            a.getChrom(), loc, '-', exptKey, fgprobe, bgprobe);
                    probes.addLast(twoChannels);
                }

			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		return probes.iterator();
	} 	
}
