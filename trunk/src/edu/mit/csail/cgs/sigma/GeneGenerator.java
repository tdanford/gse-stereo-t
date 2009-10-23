/*
 * Author: tdanford
 * Date: Mar 17, 2009
 */
package edu.mit.csail.cgs.sigma;

import java.util.Iterator;

import edu.mit.csail.cgs.datasets.general.Region;
import edu.mit.csail.cgs.datasets.species.Gene;
import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.RefGeneGenerator;
import edu.mit.csail.cgs.utils.Closeable;

public interface GeneGenerator extends Expander<Region,Gene>, Closeable {

	public Iterator<Gene> byName(String name);
	
	public static class RefGeneWrapper implements GeneGenerator {
		private RefGeneGenerator generator;
		
		public RefGeneWrapper(RefGeneGenerator rgg) { 
			generator = rgg;
		}

		public Iterator<Gene> execute(Region a) {
			return generator.execute(a);
		}

		public Iterator<Gene> byName(String name) {
			return generator.byName(name);
		}

		public void close() {
			generator.close();
			generator = null;
		}

		public boolean isClosed() {
			return generator == null || generator.isClosed();
		}
	}
	
	
}
