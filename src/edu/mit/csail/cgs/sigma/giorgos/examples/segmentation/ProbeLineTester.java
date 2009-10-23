package edu.mit.csail.cgs.sigma.giorgos.examples.segmentation;


import java.util.ArrayList;
import java.util.Vector;
import java.io.PrintStream;

import edu.mit.csail.cgs.sigma.expression.workflow.models.Chunk;
import edu.mit.csail.cgs.sigma.expression.workflow.models.ProbeLine;

public class ProbeLineTester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Integer[] d1 = {1, 2, 4, 3};
		Integer[] d2 = {2, 1, 4, 6};
		Vector<Integer[]> vv = new Vector<Integer[]>();
		vv.add(d1); vv.add(d2);
		
		String[] probeValues = {"7	+	53	-0.52", "7	+	62	-0.8", "7	+	70	-0.9", "7	+	77	-1.0", "7	+	83	-0.7"};
		ArrayList<ProbeLine> probeValuesList = new ArrayList<ProbeLine>();
		for(String prVal : probeValues)
		{
			ProbeLine prLine = new ProbeLine(prVal);
			probeValuesList.add(prLine);
		}
		
		Chunk chunk = new Chunk(probeValuesList);
		chunk.print(new PrintStream(System.out));
	}

}
