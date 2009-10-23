package edu.mit.csail.cgs.sigma.expression.ewok;

import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.datasets.chipchip.ChipChipData;
import edu.mit.csail.cgs.datasets.chipchip.SQLData;
import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.sigma.expression.BaseExpressionProperties;
import edu.mit.csail.cgs.sigma.expression.models.ExpressionProbe;
import edu.mit.csail.cgs.utils.Closeable;
import edu.mit.csail.cgs.utils.NotFoundException;

/**
 * This class used to be called ExpressionProbeGenerator, but that is now an interface that this class
 * implements. 
 * 
 * @author tdanford
 */
public class StandardProbeGenerator implements Expander<Region,ExpressionProbe>, Closeable, ExpressionProbeGenerator {
	
	private String exptKey;
	private BaseExpressionProperties props;
	private ChipChipLocator loc;
	private ChipChipData data;
	private boolean ipData;
	
    public StandardProbeGenerator(BaseExpressionProperties ps, String ek) { 
        exptKey = ek;
        props = ps;
        loc = props.getLocator(exptKey);
        data = loc.createObject();
        ((SQLData)data).setMaxCount(1);
        ipData = props.isIPData(exptKey);
    }
    
    public StandardProbeGenerator(BaseExpressionProperties ps, String ek, boolean fg) { 
        exptKey = ek;
        props = ps;
        loc = props.getLocator(exptKey);
        data = loc.createObject();
        ((SQLData)data).setMaxCount(1);
        ipData = fg ? props.isIPData(exptKey) : !props.isIPData(exptKey);
    }
    
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.ExpressionProbeGenerator#close()
	 */
	public void close() { 
		if(data instanceof Closeable) { 
			((Closeable)data).close();
		}
		data = null;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.ExpressionProbeGenerator#isClosed()
	 */
	public boolean isClosed() { 
		return data == null;
	}

	/* (non-Javadoc)
	 * @see edu.mit.csail.cgs.sigma.expression.ExpressionProbeGenerator#execute(edu.mit.csail.cgs.datasets.general.Region)
	 */
	public Iterator<ExpressionProbe> execute(Region a) {
		LinkedList<ExpressionProbe> probes = new LinkedList<ExpressionProbe>();
		try {
			data.window(a.getChrom(), a.getStart(), a.getEnd());
			for(int i = 0; i < data.getCount(); i++) { 
				int loc = data.getPos(i);
				int reps = data.getReplicates(i);
				
				/*
				 * First, collect the Watson-strand probes.
				 */
				LinkedList<Double> values = new LinkedList<Double>();
				for(int j = 0; j < reps; j++) {
					if(data.getStrand(i, j) == '+') { 
						double value = ipData ? data.getIP(i, j) : data.getWCE(i, j);
						values.addLast(value);
					}
				}
				if(values.size() > 0) { 
					ExpressionProbe probe = new ExpressionProbe(a.getGenome(), 
							a.getChrom(), loc, '+', exptKey, values);
					probes.addLast(probe);
				}

				/*
				 * Then, the Crick-strand probes.
				 */
				values = new LinkedList<Double>();
				for(int j = 0; j < reps; j++) {
					if(data.getStrand(i, j) == '-') { 
						double value = ipData ? data.getIP(i, j) : data.getWCE(i, j);
						values.addLast(value);
					}
				}
				if(values.size() > 0) { 
					ExpressionProbe probe = new ExpressionProbe(a.getGenome(), 
							a.getChrom(), loc, '-', exptKey, values);
					probes.addLast(probe);
				}

			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		return probes.iterator();
	} 	
}
