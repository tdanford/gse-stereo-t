package edu.mit.csail.cgs.sigma.litdata.miura;

import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;

import java.io.*;
import edu.mit.csail.cgs.utils.models.*;

public class MiuraHits extends ModelInputIterator<MiuraHit> {

	public MiuraHits(File f) throws IOException {
		super(new ModelInput.LineReader<MiuraHit>(MiuraHit.class, new FileReader(f)));
	}

	public MiuraHits(WorkflowProperties ps) throws IOException { 
		this(new File(ps.getDirectory(), "Miura_ESTs_hits.txt"));
	}
}