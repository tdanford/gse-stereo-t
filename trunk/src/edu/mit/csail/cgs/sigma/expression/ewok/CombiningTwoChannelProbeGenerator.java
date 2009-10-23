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

/**
 * Takes two separate ExpressionProbeGenerators, and combines them into a stream of 
 * ExpressionTwoChannelProbe objects, by taking the foreground value from one underlying 
 * generator, and the background value from the other.
 * 
 * @author tdanford
 */
public class CombiningTwoChannelProbeGenerator 
    implements Expander<Region,ExpressionTwoChannelProbe>, Closeable {
	
	private String exptKey1, exptKey2, compositeKey;
	private BaseExpressionProperties props;
	private ExpressionProbeGenerator gen1, gen2;
	
    public CombiningTwoChannelProbeGenerator(BaseExpressionProperties ps, String ek1, String ek2) { 
        exptKey1 = ek1;
        exptKey2 = ek2;
        props = ps;
        compositeKey = String.format("%s+%s", exptKey1, exptKey2);
        
        gen1 = new StandardProbeGenerator(ps, exptKey1);
        gen2 = new StandardProbeGenerator(ps, exptKey2);
    }
    
	public void close() { 
		gen1.close();
		gen2.close();
		gen1 = gen2 = null;
	}
	
	public boolean isClosed() { 
		return gen1.isClosed() || gen2.isClosed();
	}

	public Iterator<ExpressionTwoChannelProbe> execute(Region a) {
		LinkedList<ExpressionTwoChannelProbe> probes = new LinkedList<ExpressionTwoChannelProbe>();
		
		Iterator<ExpressionProbe> itr1 = gen1.execute(a);
		Iterator<ExpressionProbe> itr2 = gen2.execute(a);
		
		TreeSet<ExpressionProbe> pos1 = new TreeSet<ExpressionProbe>();
		TreeSet<ExpressionProbe> neg1 = new TreeSet<ExpressionProbe>();

		TreeSet<ExpressionProbe> pos2 = new TreeSet<ExpressionProbe>();
		TreeSet<ExpressionProbe> neg2 = new TreeSet<ExpressionProbe>();
		
		while(itr1.hasNext()) { 
			ExpressionProbe p = itr1.next();
			if(p.getStrand() == '+') { 
				pos1.add(p);
			} else { 
				neg1.add(p);
			}
		}
		
		while(itr2.hasNext()) { 
			ExpressionProbe p = itr2.next();
			if(p.getStrand() == '+') { 
				pos2.add(p);
			} else { 
				neg2.add(p);
			}
		}
		
		Iterator<ExpressionProbe> pitr = pos1.iterator();
		Iterator<ExpressionProbe> nitr = neg1.iterator();
		
		for(int i = 0; i < pos1.size(); i++) { 
			ExpressionProbe p = pitr.next();
			ExpressionProbe n = nitr.next();
			
			if(p.getLocation() != n.getLocation()) { 
				System.err.println("UHOH!!!");
			}
			
			ExpressionTwoChannelProbe tcp = new ExpressionTwoChannelProbe(p.getGenome(), p.getChrom(), p.getLocation(), p.getStrand(), 
					compositeKey, p, n);
			probes.addLast(tcp);
		}
		
		pitr = pos2.iterator();
		nitr = neg2.iterator();
		
		for(int i = 0; i < pos2.size(); i++) { 
			ExpressionProbe p = pitr.next();
			ExpressionProbe n = nitr.next();
			
			if(p.getLocation() != n.getLocation()) { 
				System.err.println("UHOH!!!");
			}
			
			ExpressionTwoChannelProbe tcp = new ExpressionTwoChannelProbe(p.getGenome(), p.getChrom(), p.getLocation(), p.getStrand(), 
					compositeKey, p, n);
			probes.addLast(tcp);
		}
		
		return probes.iterator();
	} 	
}
