package edu.mit.csail.cgs.sigma.networks;

import java.util.*;
import java.util.logging.*;
import java.io.*;

import edu.mit.csail.cgs.ewok.verbs.Expander;
import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.sigma.SigmaProperties;
import edu.mit.csail.cgs.sigma.lethality.*;
import edu.mit.csail.cgs.sigma.metabolism.*;
import edu.mit.csail.cgs.sigma.biogrid.*;
import edu.mit.csail.cgs.sigma.genes.*;
import edu.mit.csail.cgs.utils.graphs.*;
import edu.mit.csail.cgs.viz.graphs.*;

public class SigmaNetwork {

	public static void main(String[] args) { 
		SigmaProperties props = new SigmaProperties();
		SigmaNetwork network = new SigmaNetwork(props);
		DirectedGraph netgraph = network.createNetworkGraph();

        int maxDescendants = 0;
        String maxNode = null;
        
        Map<String,Set<String>> descMap = new HashMap<String,Set<String>>();
        for(String node : netgraph.getVertices()) {
            Set<String> descs = netgraph.getDescendants(node);
            descMap.put(node, descs);

            if((descs.size() <= 100 && (maxNode == null || descs.size() > maxDescendants))) { 
                maxDescendants = descs.size();
                maxNode = node;
            }
        }
        
        netgraph = createDescendantGraph(netgraph, maxNode);
        network.logger.log(Level.INFO, "Max Descendants: " + maxDescendants);

        network.logger.log(Level.INFO, "Creating visualization of graph.");
        DirectedGraphAdapter adapter = new DirectedGraphAdapter(netgraph);
        GraphView view = adapter.getView();
        GraphView.TestGraphFrame f = new GraphView.TestGraphFrame(view.createInteractive());
	}
    
    public static DirectedGraph createDescendantGraph(DirectedGraph dg, String root) { 
        DirectedGraph graph = new DirectedGraph();
        
        Set<String> children = dg.getDescendants(root);
        graph.addVertex(root); 
        
        for(String child : children) { 
            if(!child.equals(root)) { 
                graph.addVertex(child);
            }
        }
        
        Set<String> nodes = new HashSet<String>(children); nodes.add(root);
        for(String n1 : nodes) { 
            for(String n2 : nodes) {
                if(n1.compareTo(n2) <= 0) { 
                    if(dg.containsEdge(n1, n2)) { 
                        graph.addEdge(n1, n2);
                    }

                    if(!n1.equals(n2) && dg.containsEdge(n2, n1)) { 
                        graph.addEdge(n2, n1);
                    }
                }
            }
        }
        
        return graph;
    }

	private Logger logger;
	private SigmaProperties props;

	public SigmaNetwork(SigmaProperties ps) { 
		props = ps;
		logger = props.getLogger("SigmaNetwork");
	}

	public DirectedGraph createNetworkGraph() { 
        
        logger.log(Level.INFO, "Building Metabolic Network graph...");

		MetabolismProperties mprops = new MetabolismProperties(props, "default");
		MetabolismNetwork metabNet = new MetabolismNetwork(mprops);

		try { 
			metabNet.loadNetwork();
		} catch(IOException e) {
			logger.log(Level.SEVERE, e.getMessage());
		}

		DirectedGraph network = new DirectedGraph();

        logger.log(Level.INFO, "Creating DirectedGraph object...");
        
		Collection<String> orfs = metabNet.getTotalORFs();
		for(String orf : orfs) { network.addVertex(orf); }

		Map<String,Rxn> rxns = metabNet.getReactions();

		for(String rn : rxns.keySet()) { 
			ORFSet baseORFs = metabNet.getLogicalORFTree(rn).getAllORFs();

			Set<String> inputs = metabNet.getInputReactions(rxns.get(rn));
			Set<String> outputs = metabNet.getOutputReactions(rxns.get(rn));

			for(String inputName : inputs) { 
				ORFSet inputORFs = metabNet.getLogicalORFTree(inputName).getAllORFs();
				for(String input : inputORFs.getORFs()) { 
					for(String base : baseORFs.getORFs()) { 
						network.addEdge(input, base);
					}
				}
			}

			for(String outputName : outputs) { 
				ORFSet outputORFs = metabNet.getLogicalORFTree(outputName).getAllORFs();
				for(String output : outputORFs.getORFs()) { 
					for(String base : baseORFs.getORFs()) { 
						network.addEdge(base, output);
					}
				}
			}
		}

		return network;
	}

}
