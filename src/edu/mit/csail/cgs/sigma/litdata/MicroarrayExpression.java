/*
 * Author: tdanford
 * Date: Mar 3, 2009
 */
package edu.mit.csail.cgs.sigma.litdata;

import java.io.*;

import edu.mit.csail.cgs.sigma.genes.GeneNameAssociation;
import edu.mit.csail.cgs.sigma.litdata.huiyer.AffyAnnotationDecoder;

public interface MicroarrayExpression {
	public MicroarrayProbe expression(String id);
}

class AppendingExpression implements MicroarrayExpression {
	
	private MicroarrayExpression[] expr;
	
	public AppendingExpression(MicroarrayExpression... e) { 
		expr = e.clone();
	}

	public MicroarrayProbe expression(String id) {
		MicroarrayProbe p = new MicroarrayProbe();
		for(int i = 0; i < expr.length; i++) { 
			MicroarrayProbe pp = expr[i].expression(id);
			if(pp != null) { 
				p.append(pp);
			}
		}
		return p.id == null ? null : p;
	} 
}

class AffyAveraging implements MicroarrayExpression {
	
	private AffyAnnotationDecoder decoder; 
	private MicroarrayExpression inner;
	
	public AffyAveraging(File f, MicroarrayExpression e) throws IOException { 
		decoder = new AffyAnnotationDecoder(f);
		inner = e;
	}
	
	public MicroarrayProbe expression(String id) {
		MicroarrayProbe p = new MicroarrayProbe();
		for(String probe : decoder.probes(id)) { 
			MicroarrayProbe p2 = inner.expression(probe);
			if(p2 != null) { p.append(p2); }
		}
		return p.id == null ? null : p;
	} 
}

class GeneNameAssociationAveraging implements MicroarrayExpression {
	
	private GeneNameAssociation decoder;
	private MicroarrayExpression inner;
	
	public GeneNameAssociationAveraging(GeneNameAssociation asso, MicroarrayExpression e) { 
		decoder = asso;
		inner = e;
	}

	public MicroarrayProbe expression(String id) {
		MicroarrayProbe p = new MicroarrayProbe();
		if(decoder.containsName(id)) { 
			for(String probe : decoder.getIDs(id)) {  
				MicroarrayProbe p2 = inner.expression(probe);
				if(p2 != null) { p.append(p2); }
			}
		}
		if(decoder.containsID(id)) { 
			MicroarrayProbe p2 = inner.expression(decoder.getName(id));
			if(p2 != null) { p.append(p2); }
		}
		return p.id == null ? null : p;
	} 
}

