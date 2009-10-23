package edu.mit.csail.cgs.sigma.probnetwork;

import java.util.*;
import java.util.logging.*;

import java.io.*;

import edu.mit.csail.cgs.sigma.Parser;
import edu.mit.csail.cgs.utils.graphs.*;

import edu.mit.csail.cgs.viz.graphs.*;

/**
 * @author tdanford
 */
public class ProbNetwork {
	
	public static void main(String[] args) { 
		ProbNetworkProperties props = new ProbNetworkProperties();
		ProbNetwork net = new ProbNetwork(props);
		try {
			net.loadData();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		UndirectedGraph graph = net.createOrfGraph(2.0);
		UndirectedGraphAdapter adapter = new UndirectedGraphAdapter(graph);
		GraphView view = adapter.getView();
		GraphView.TestGraphFrame f = new GraphView.TestGraphFrame(view.createInteractive());
	}

	private ProbNetworkProperties props;
	private Vector<YeastNetEntry> orfEvidence, geneEvidence;
	private Logger logger;
	
	public ProbNetwork(ProbNetworkProperties ps) { 
		props = ps;
		logger = props.getLogger("ProbNetwork");
		orfEvidence = new Vector<YeastNetEntry>();
		geneEvidence = new Vector<YeastNetEntry>();
	}
	
	private UndirectedGraph createGraph(Collection<YeastNetEntry> es, double cutoff) { 
		UndirectedGraph graph = new UndirectedGraph();
		Set<String> nodes = new HashSet<String>();
		
		for(YeastNetEntry e : es) { 
			String n1 = e.getName1(), n2 = e.getName2();
			
			if(!nodes.contains(n1)) { graph.addVertex(n1); nodes.add(n1); }
			if(!nodes.contains(n2)) { graph.addVertex(n2); nodes.add(n2); }
			
			double value = e.getValue();
			if(value >= cutoff) { 
				graph.addEdge(n1, n2);
			}
		}
		
		return graph;
	}
	
	public UndirectedGraph createGeneGraph(double cutoff) { 
		return createGraph(geneEvidence, cutoff);
	}
	
	public UndirectedGraph createOrfGraph(double cutoff) { 
		return createGraph(orfEvidence, cutoff);
	}
	
	public void loadData() throws IOException {
		logger.log(Level.INFO, "Loading probabilistic network information...");
		
		File file = props.getOrfEdgesFile();
		Parser<YeastNetEntry> parser = 
			new Parser<YeastNetEntry>(file, new YeastNetEntry.ParsingMapper());
		
		int c = 0;
		while(parser.hasNext()) { 
			YeastNetEntry e = parser.next();
			orfEvidence.add(e);
			c += 1;
		}
		logger.log(Level.FINE, String.format("Loaded %d ORF evidence entries.", c));
		
		file = props.getGeneEvidenceEdgesFile();
		parser = new Parser<YeastNetEntry>(file, new YeastNetEntry.ParsingMapper());
		
		c = 0;
		while(parser.hasNext()) { 
			YeastNetEntry e = parser.next();
			geneEvidence.add(e);
			c += 1;
		}
		logger.log(Level.FINE, String.format("Loaded %d GENE evidence entries.", c));
	}
}
