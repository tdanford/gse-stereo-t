package edu.mit.csail.cgs.sigma.metabolism;

import java.util.*;
import java.io.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.*;
import org.jgrapht.traverse.*;
import java.util.regex.Pattern;

public class OlgaTest { 
	
    public static void main(String[] args) { 
	try { 
	    test(args);
	} catch(IOException e) { 
	    e.printStackTrace(System.err);
	}
    }

    /* This method tests if the given reactant vertex is already in the network
     * by checking it against the given vertex set, and if the
     * reactant is one of the chosen ignored vertices.
     */
    public static boolean canAddToNetwork(Reactant r,
					  Graph<MetabolismVertex,
					  DefaultEdge> g) {
	for (MetabolismVertex v : g.vertexSet()) {
	    if (r.equals(v.getReactant())) { return false; }
	}
	return true;
    }

    public static MetabolismVertex
	findReactantInNetwork(Graph<MetabolismVertex, DefaultEdge> g, Reactant react) {
	Set<MetabolismVertex> reactantVertexSet = new
	    HashSet<MetabolismVertex>();
	for (MetabolismVertex v : g.vertexSet()) {
	    if (v.isReactant() && v.toString().equals(react.toString()))
		{ return v; }
	}
	/* new MetabolismVertex is initialized with no parameters, the
	 * reactant didn't show up if (returnVertex.isRxn() &&
	 * returnVertex.isReactant()) are both false.
	 */
	return new MetabolismVertex();
    }

    public static DirectedGraph<MetabolismVertex, DefaultEdge>
	makeGraph(MetabolismNetwork aNetwork,
		  Map<String, String> deletedGenes)  throws IOException { 
// 	    Map<String, Integer>  = new HashMap<String, Integer>();
	    
// 	    /* this makes a representation of the metabolism network
// 	     * using the "circular" methodology, i.e. bidirectional
// 	     * have separate forward and backward reactions to which
// 	     * the right and left reactants are the respective inputs
// 	     * and outputs.
// 	     */
	    Set<String> names = 
		aNetwork.getReactionAbbreviations();

	    DirectedGraph<MetabolismVertex, DefaultEdge> metabolismGraph = new
		SimpleDirectedGraph<MetabolismVertex,
		DefaultEdge>(DefaultEdge.class);
	    Set<MetabolismVertex> rxnVertices = new
		HashSet<MetabolismVertex>();
	    Set<MetabolismVertex> reactantVertices = new
		HashSet<MetabolismVertex>();

	Outer:
	    for (String rxnName : names) {
// 		fw.write("... reactant vertices: " + reactantVertices.toString());
// 		fw.write("... rxn vertices: " + rxnVertices.toString());
		Rxn rxn = aNetwork.getReaction(rxnName);
		//		System.out.println(rxn.getORF().getAllORFs().getORFs().toString()); // 
		for (String deletedORF : deletedGenes.keySet()) {
		    if (rxn.getORF().getAllORFs().containsORF(deletedORF)) {
// 			fw.write("\nDELETED GENE: " + deletedORF
// 			+ "is in:\n" + rxn.toString());
			continue Outer;
		    }
		}
// 		MetabolismVertex rxnVertex = new
// 		    MetabolismVertex(rxn);
// 		metabolismGraph.addVertex(rxnVertex);
// 		rxnVertices.add(rxnVertex);
		
		Set<Reactant> leftReactants = rxn.getLeftReact();
		Set<Reactant> rightReactants = rxn.getRightReact();
		
		boolean isBiDirectional =
		    rxn.getType().equals(Rxn.Type.BIDIRECTIONAL);
		
		if (isBiDirectional){
		    MetabolismVertex fwdRxn = new MetabolismVertex(rxn, "forward");
		    MetabolismVertex bkwdRxn = new MetabolismVertex(rxn, "backward");
		    metabolismGraph.addVertex(fwdRxn);
		    metabolismGraph.addVertex(bkwdRxn);
// 		    metabolismGraph.addEdge(rxnVertex, fwdRxn);
// 		    metabolismGraph.addEdge(rxnVertex, bkwdRxn);
		    
		    for (Reactant reactant : leftReactants) {
			//			fw.write("\n\nBiDir, Left Reactants: " + reactant.toString());
			String reactantName = reactant.getName();
// 			if (.containsKey(reactantName)) {
// 			    .put(reactantName,
// 					  .get(reactantName)
// 					      + 1);
// 			}
// 			else { .put(reactantName, 1); }

			MetabolismVertex reactantVertex;

			/* this will continue to the next reactant
			 * if the current reactant is one that was
			 * chosen to ignore (a primitive element)
			 */
			if (reactant.isTrivial()) { continue; }

			else if (canAddToNetwork(reactant, metabolismGraph)) { 
			    reactantVertex = new MetabolismVertex(reactant);
			    metabolismGraph.addVertex(reactantVertex);
			    reactantVertices.add(reactantVertex);
			}
			else { 
			    reactantVertex =
				findReactantInNetwork(metabolismGraph,
						      reactant); 
			}

			
			metabolismGraph.addEdge(reactantVertex, fwdRxn);
			metabolismGraph.addEdge(bkwdRxn, reactantVertex);
		    }
		    
		    
		    for (Reactant reactant : rightReactants) {
			//			fw.write("\n\nBiDir, Right Reactants: " + reactant.toString());
			String reactantName = reactant.getName();
// 			if (.containsKey(reactantName)) {
// 			    .put(reactantName,
// 					  .get(reactantName)
// 					      + 1);
// 			}
// 			else { .put(reactantName, 1); }
			MetabolismVertex reactantVertex;
			
			/* this will continue to the next reactant
			 * if the current reactant is one that was
			 * chosen to ignore (a primitive element)
			 */
			if (reactant.isTrivial()) { continue; }
			else if (canAddToNetwork(reactant, metabolismGraph)) { 
			    reactantVertex = new MetabolismVertex(reactant);
			    metabolismGraph.addVertex(reactantVertex);
			    reactantVertices.add(reactantVertex);
			}
			else { 
			    reactantVertex =
				findReactantInNetwork(metabolismGraph,
						      reactant); 
			}

			metabolismGraph.addEdge(fwdRxn, reactantVertex);
			metabolismGraph.addEdge(reactantVertex, bkwdRxn);

		    }
		}
		
		else {
		    MetabolismVertex rxnVertex = new
			MetabolismVertex(rxn);
		    metabolismGraph.addVertex(rxnVertex);
		    rxnVertices.add(rxnVertex);
		    for (Reactant reactant : leftReactants) {
			//			fw.write("\n\nUniDir, Left Reactants: " + reactant.toString());
			String reactantName = reactant.getName();
// 			if (.containsKey(reactantName)) {
// 			    .put(reactantName,
// 					  .get(reactantName)
// 					      + 1);
// 			}
// 			else { .put(reactantName, 1); }
			MetabolismVertex reactantVertex;

			/* this will continue to the next reactant
			 * if the current reactant is one that was
			 * chosen to ignore (a primitive element)
			 */
			if (reactant.isTrivial()) { continue; }
			else if (canAddToNetwork(reactant, metabolismGraph)) { 
			    reactantVertex = new MetabolismVertex(reactant);
			    metabolismGraph.addVertex(reactantVertex);
			    reactantVertices.add(reactantVertex);
			}
			else { 
			    reactantVertex =
				findReactantInNetwork(metabolismGraph,
						      reactant); 
			}

		
			metabolismGraph.addEdge(reactantVertex, rxnVertex); 

		    }
		    for (Reactant reactant : rightReactants) {
			//			fw.write("\nUniDir, Right reactants :" + reactant.toString());
			String reactantName = reactant.getName();
// 			if (.containsKey(reactantName)) {
// 			    .put(reactantName,
// 					  .get(reactantName)
// 					      + 1);
// 			}
// 			else { .put(reactantName, 1); }
			MetabolismVertex reactantVertex;

			/* this will continue to the next reactant
			 * if the current reactant is one that was
			 * chosen to ignore (a primitive element)
			 */
			if (reactant.isTrivial()) { continue; }
			else if (canAddToNetwork(reactant, metabolismGraph)) { 
			    reactantVertex = new MetabolismVertex(reactant);
			    metabolismGraph.addVertex(reactantVertex);
			    reactantVertices.add(reactantVertex);
			}
			else { 
			    reactantVertex =
				findReactantInNetwork(metabolismGraph,
						      reactant); 
			}
// 			System.out.println("rxnVertex="+rxnVertex.toString());
// 			System.out.println("reactantVertex="+reactantVertex.toString());
			metabolismGraph.addEdge(rxnVertex, reactantVertex); 

		    }
		}
	    }
	    return metabolismGraph;
    }

    public static void
	testConnectivity(DirectedGraph<MetabolismVertex, DefaultEdge>
			 aGraph,  String graphName, String startOfPath,
			 String endOfPath) {
	System.out.println();
	List<DefaultEdge> shortestPath;
	ConnectivityInspector ci = new ConnectivityInspector(aGraph);
	for (MetabolismVertex v : aGraph.vertexSet()) {
// 	    if (v.isReactant()) {System.out.println(v.toString());}
	    if (v.getName().equals(startOfPath)) {
		System.out.println("["+graphName+"] Incoming edges of" 
				   + startOfPath + ":\n"
				   + aGraph.incomingEdgesOf(v).toString());
		System.out.println("[" + graphName + 
				   "] Outgoing edges of " 
				   + startOfPath + ":\n"
				   + aGraph.outgoingEdgesOf(v).toString());
		for (MetabolismVertex u : aGraph.vertexSet()){
		    if (u.getName().equals(endOfPath)) {
			System.out.println("[" + graphName +
					   "] Incoming edges of" + endOfPath + ":\n"
					   + aGraph.incomingEdgesOf(u).toString());
			System.out.println("[" + graphName + 
					   "] Outgoing edges of " + endOfPath + ":\n"
					   + aGraph.outgoingEdgesOf(u).toString());
			System.out.println("[" + graphName + 
					   "] There is a path between " + 
					   startOfPath + " and " + endOfPath + ": "
					   + ci.pathExists(v, u));

			shortestPath = DijkstraShortestPath.findPathBetween(aGraph,
									    v, u);
			if (shortestPath != null) {
			    System.out.println("["+graphName+"]" +
					       "ShortestPath between "
					       +  startOfPath + " and"
					       + " " + endOfPath + ":\n" +
					       shortestPath.toString());
			}
		    }
		}
	    }
	}
    }

    public static HashSet<MetabolismVertex> 
	getVerticesFromString( DirectedGraph<MetabolismVertex,
			       DefaultEdge> graph, HashSet<String> orfs
			       ) {
	HashSet<MetabolismVertex> verticesInGraph = 
	    new HashSet<MetabolismVertex>();
	for ( MetabolismVertex v: graph.vertexSet() ) {
	    if ( v.isRxn() ) {
		for ( String orf : orfs ) {
		    if ( v.getRxn().getORF().getAllORFs().getORFs().contains( orf ) ) {
			verticesInGraph.add( v ); } } } }
	return verticesInGraph; }

    public static ValuedVertex[]
	findBetweenness(DirectedGraph<MetabolismVertex, DefaultEdge>
	g, boolean ifPrint)
	{
	    Map<MetabolismVertex, Integer> btwn = new HashMap<MetabolismVertex,
		Integer>();
	    for (MetabolismVertex v : g.vertexSet()) {
		btwn.put(v, 0);
	    }
	    List<DefaultEdge> shortestPath;
	    for (MetabolismVertex v : g.vertexSet()) {
// 		System.out.println("to vertex = " + v.toString());
		for (MetabolismVertex u : g.vertexSet()) {
// 		    System.out.println("from vertex = " + u.toString());
		    shortestPath =
			DijkstraShortestPath.findPathBetween(g,
							     u, v);
		    if (shortestPath != null && !shortestPath.isEmpty()) {
// 			System.out.println(shortestPath.toString());
			for (DefaultEdge w : shortestPath) {
			    // count up all the edges in the path list
			    btwn.put(g.getEdgeSource(w),
				     btwn.get(g.getEdgeSource(w)) + 1);
			}
		    // have to get last target since counted sources before
			btwn.put(g.getEdgeTarget(shortestPath.get(shortestPath.size()-1)),
						  btwn.get(g.getEdgeTarget(shortestPath.get(shortestPath.size()-1))) + 1);
		    }
// 		    System.out.println("finished one vertex pair");
		}
		 if (ifPrint) { 
		     System.out.println("finished one vertex to every other vertex"); }
	    }
// 	    System.out.println(btwn.toString());

	    ValuedVertex[] betweennessValues =
		ValuedVertex.fromMap(btwn);
	    Arrays.sort(betweennessValues);
	    if (ifPrint) {
		for (ValuedVertex vv : betweennessValues) {
		    System.out.println(vv.toString());
		}
	    }
	    return betweennessValues;
	}

    public static TreeMap<Integer, Integer>
	findShortestPaths( DirectedGraph<MetabolismVertex, DefaultEdge>
			   g, HashSet<String> from,
			   HashSet<MetabolismVertex> toVertices ) {
	TreeMap<Integer, Integer> numNodesBetween = new TreeMap<Integer,
	    Integer>();
	Integer n = 0;
	// 	    for (MetabolismVertex v : g.vertexSet()) {
	// 		numNodesBetween.put(n++, 0);
// 	    }
	HashSet<MetabolismVertex> fromVertices =
	    getVerticesFromString( g, from );
	System.out.println("made vertices from string");
// 	    HashSet<MetabolismVertex> toVertices =
// 		getVerticesFromString( g, to );
	List<DefaultEdge> shortestPath;
	for (MetabolismVertex v : toVertices ) {
// 		System.out.println("to vertex = " + v.toString());
	    for (MetabolismVertex u : fromVertices) {
// 		    System.out.println("from vertex = " + u.toString());
		if ( u != v ) {
		    shortestPath =
			DijkstraShortestPath.findPathBetween(g,
							     u, v);
		    if (shortestPath != null && !shortestPath.isEmpty()) {
			// 			System.out.println(shortestPath.toString());
			if ( numNodesBetween.containsKey(
							 shortestPath.size()
							 ) ) {
			    numNodesBetween.put(shortestPath.size(),
						numNodesBetween.get(shortestPath.size())
						+ 1 ); }
			else { 
			    numNodesBetween.put( shortestPath.size(),
						 1 ); }
						    
			    // have to get last target since counted sources before
// 			    btwn.put(g.getEdgeTarget(shortestPath.get(shortestPath.size()-1)),
// 				     btwn.get(g.getEdgeTarget(shortestPath.get(shortestPath.size()-1))) + 1);
			}
			// 		    System.out.println("finished one vertex pair");
		}
	    }
	    // 		 if (ifPrint) { 
// 	    System.out.println("finished one vertex to every other" \
// 			       "vertex"); 
	    System.out.println(numNodesBetween.toString());
	}
// 	    System.out.println(btwn.toString());

// 	    ValuedVertex[] betweennessValues =
// 		ValuedVertex.fromMap(btwn);
// 	    Arrays.sort(betweennessValues);
// 	    if (ifPrint) {
// 		for (ValuedVertex vv : betweennessValues) {
// 		    System.out.println(vv.toString());
// 		}
// 	    }
	return numNodesBetween;
	}

    public static DirectedSubgraph<MetabolismVertex, DefaultEdge>
	makeSubgraph(DirectedGraph<MetabolismVertex,
		     DefaultEdge> g, Set<String> geneSet) {
	    Set<MetabolismVertex> geneVertices = new
		HashSet<MetabolismVertex>();  /* geneVertices could
						 also be called  rxnVertices */
	    Set<DefaultEdge> edges = new HashSet
		<DefaultEdge>();
	    Set<MetabolismVertex> reactantVertices = new
		HashSet<MetabolismVertex>();
	    Set<DefaultEdge> incomingEdges = new
		HashSet<DefaultEdge>();
	    Set<DefaultEdge> outgoingEdges = new
		HashSet<DefaultEdge>();
	    
	    for (String gene : geneSet) {
		for (MetabolismVertex v : g.vertexSet()) {
		    if (v.isRxn() &&
			v.toString().toLowerCase().contains(gene.toLowerCase()))
			{ 
// 			    System.out.println(v.toString());
			    geneVertices.add(v); 
			    incomingEdges = g.incomingEdgesOf(v);
			    outgoingEdges = g.outgoingEdgesOf(v);
			    for ( DefaultEdge e : incomingEdges) {
				reactantVertices.add(g.getEdgeSource(e)); }
			    for (DefaultEdge e : outgoingEdges) {
				reactantVertices.add(g.getEdgeTarget(e));
}
			    edges.addAll(incomingEdges);
			    edges.addAll(outgoingEdges);
			}
		}
	    }
// 	    System.out.println(sterolGeneVertices.toString());
// 	    System.out.println(sterolRxnVertices.toString());
// 	    System.out.println(sterolEdges.toString());
	    Set<MetabolismVertex> allVertices = new
		HashSet<MetabolismVertex>();
	    allVertices.addAll(reactantVertices);
	    allVertices.addAll(geneVertices);

	    DirectedSubgraph<MetabolismVertex, DefaultEdge> subg
		= new DirectedSubgraph(g, allVertices,
		edges);
	    return subg;
    }

	public static void test(String[] args)  throws IOException { 
// 	    Calendar now  = new GregorianCalendar();
// 	    FileWriter fw = new	FileWriter((now.get(Calendar.MONTH)+1) +
// 					   "-" +
// 					   now.get(Calendar.DAY_OF_MONTH) + "_" +
// 					   now.get(Calendar.HOUR_OF_DAY)
// 					   + "." +
// 					   now.get(Calendar.MINUTE) +
// 					    "." + now.get(Calendar.SECOND));

	    MetabolismProperties props = new MetabolismProperties();
	    MetabolismNetwork network  = new MetabolismNetwork(props);
	    network.loadNetwork();

// 	    System.out.println(props.getTrivialReactants().toString());

	    FileInputStream fstreamORFs  = new FileInputStream("missingSigmaORFs.txt");
	    FileInputStream fstreamGenes = new FileInputStream("missingSigmaGenes.txt");
	    DataInputStream inORFs       = new DataInputStream(fstreamORFs);
	    DataInputStream inGenes      = new DataInputStream(fstreamGenes);
	    BufferedReader brORFs        = new BufferedReader(new InputStreamReader(inORFs));
	    BufferedReader brGenes       = new BufferedReader(new InputStreamReader(inGenes));
	    String strLineORFs;
	    String strLineGenes;

	    Map<String, String> controlDeleted = new HashMap<String, String>();

	    Map<String, String> sigmaDeleted = new HashMap<String, String>();
	    Map<String, String> s288cDeleted = new HashMap<String, String>();

	    s288cDeleted.put("YBR299W", "Mal32");
	    s288cDeleted.put("YGR292W", "Mal12");
	    s288cDeleted.put("YGR287C", "Mal33");
	    s288cDeleted.put("YIL172C", "Mal13");
	    s288cDeleted.put("YJL216C", "Mal14");
	    s288cDeleted.put("YGR289C", "Mal11");
	    s288cDeleted.put("YBR298C", "Mal31");

	    while ((strLineORFs = brORFs.readLine()) != null &&
		   (strLineGenes = brGenes.readLine()) != null)   {
		sigmaDeleted.put(strLineORFs, strLineGenes);
	    }
// 	    System.out.println("\n\nDeleted genes in Sigma:\n" +
// 			       sigmaDeleted.toString());

	    DirectedGraph<MetabolismVertex, DefaultEdge> controlGraph =
		makeGraph(network, controlDeleted);

	    DirectedGraph<MetabolismVertex, DefaultEdge> sigmaGraph =
		makeGraph(network, sigmaDeleted);
	    DirectedGraph<MetabolismVertex, DefaultEdge> s288cGraph =
		makeGraph(network, s288cDeleted);

	    FileInputStream fstreamSigmaLethals = new
		FileInputStream( "sigmaLethalsPreconfirmation" ); 
	    FileInputStream fstreamS288CLethals = new
		FileInputStream( "s288cLethalsPreconfirmation" ); 
	    DataInputStream inSigmaLethals = new 
		DataInputStream( fstreamSigmaLethals );
	    DataInputStream inS288CLethals = new 
		DataInputStream( fstreamS288CLethals );
	    BufferedReader brSigmaLethals = new 
		BufferedReader( new 
				InputStreamReader( inSigmaLethals ) );
	    BufferedReader brS288CLethals = new 
		BufferedReader( new 
				InputStreamReader( inS288CLethals ) );
	    String strLineSigmaLethals;
	    String strLineS288CLethals;

	    String[] orfs;
	    String orf;
	    
	    HashSet<String> allSigmaLethals = new HashSet<String>();
// 	    HashSet<String> allS288CLethals = new HashSet<String>();
	    HashSet<String> allLethals = new HashSet<String>();

// 	    Pattern whitespace = Pattern.compile( "\\s+" );

	    while ( ( strLineSigmaLethals = brSigmaLethals.readLine()
		      ) != null ) {
		orfs = strLineSigmaLethals.split( "\\s+" );
// 		System.out.println("strLineSigmaLethals: ");
// 		for (String s : orfs ) {
// 		    System.out.print( s + "..." ); }
		allSigmaLethals.add( orfs[1] ); }
	    while ( ( strLineS288CLethals = brS288CLethals.readLine()
		      ) != null ) {
// 		allS288CLethals.add(strLineS288CLethals);
		orf = strLineS288CLethals.split(" ")[0].toString();
// 		System.out.println("strLineS288CLethals = " +
// 				   orf );
		if ( !allSigmaLethals.add(strLineS288CLethals.split("\\s+")[0]) ) {
		    allLethals.add( orf );
		      } }
// 	    System.out.println( "All Lethals: " + allLethals.toString() );

	    fstreamSigmaLethals = new
		FileInputStream( "sigma_lethals" ); 
	    fstreamS288CLethals = new
		FileInputStream( "s288c_lethals" ); 
	    inSigmaLethals = new 
		DataInputStream( fstreamSigmaLethals );
	    inS288CLethals = new 
		DataInputStream( fstreamS288CLethals );
	    brSigmaLethals = new 
		BufferedReader( new 
				InputStreamReader( inSigmaLethals ) );
	    brS288CLethals = new 
		BufferedReader( new 
				InputStreamReader( inS288CLethals ) );
	    
	    HashSet<String> sigmaLethals = new HashSet<String>();
	    HashSet<String> s288cLethals = new HashSet<String>();


	    while ( ( strLineSigmaLethals = brSigmaLethals.readLine()
		      ) != null ) {
		sigmaLethals.add(strLineSigmaLethals.split(" ")[0]); }
	    while ( ( strLineS288CLethals = brS288CLethals.readLine()
		      ) != null ) {
		s288cLethals.add(strLineS288CLethals.split(" ")[0]); }

// 	    System.out.println( "Sigma Lethals: " + sigmaLethals.toString() );
// 	    System.out.println( "S288C Lethals: " + s288cLethals.toString() );

	    HashSet<MetabolismVertex> allLethalsControl =
		getVerticesFromString( controlGraph, allLethals );
	    HashSet<MetabolismVertex> allLethalsSigma =
		getVerticesFromString( sigmaGraph, allLethals );
	    HashSet<MetabolismVertex> allLethalsS288C =
		getVerticesFromString( s288cGraph, allLethals );

	    // [strain whose essential genes are used]ToLethals[graph
	    // used]
	    System.out.println("\n---\nsigmaToLethalsControl");
	    TreeMap<Integer, Integer> sigmaToLethalsControl =
		findShortestPaths( controlGraph, sigmaLethals, allLethalsControl);
	    System.out.println("\n---\ns288cToLethalsControl");
	    TreeMap<Integer, Integer> s288cToLethalsControl =
		findShortestPaths( controlGraph, s288cLethals, allLethalsControl);
	    System.out.println("\n---\nsigmaToLethalsSigma");
	    TreeMap<Integer, Integer> sigmaToLethalsSigma =
		findShortestPaths( sigmaGraph, sigmaLethals, allLethalsSigma);
	    System.out.println("\n---\ns288cToLethalsSigma");
	    TreeMap<Integer, Integer> s288cToLethalsSigma =
		findShortestPaths( sigmaGraph, s288cLethals, allLethalsSigma);
	    System.out.println("\n---\nsigmaToLethalsS288C");
	    TreeMap<Integer, Integer> sigmaToLethalsS288C =
		findShortestPaths( s288cGraph, sigmaLethals, allLethalsS288C);
	    System.out.println("\n---\ns288cToLethalsS288C");
	    TreeMap<Integer, Integer> s288cToLethalsS288C =
		findShortestPaths( s288cGraph, s288cLethals, allLethalsS288C);

	    System.out.println( sigmaToLethalsControl.toString() );
	    System.out.println( s288cToLethalsControl.toString() );
	    System.out.println( sigmaToLethalsSigma.toString() );
	    System.out.println( s288cToLethalsSigma.toString() );
	    System.out.println( sigmaToLethalsS288C.toString() );
	    System.out.println( s288cToLethalsS288C.toString() );

// 	    fstreamSigmaLethals = FileInputStream( "sigma_lethals" ); 
// 	    fstreamS288CLethals = new
// 		FileInputStream( "s288cLethalsPreconfirmation" ); 
// 	    DataInputStream inSigmaLethals = new 
// 		DataInputStream( fstreamSigmaLethals );
// 	    DataInputStream inS288CLethals = new 
// 		DataInputStream( fstreamS288CLethals );
// 	    BufferedReader brSigmaLethals = new 
// 		BufferedReader( new 
// 				InputStreamReader( inSigmaLethals ) );
// 	    BufferedReader brS288CLethals = new 
// 		BufferedReader( new 
// 				InputStreamReader( inS288CLethals ) );
// 	    String strLineSigmaLethals;
// 	    String strLineS288CLethals;

// 	    FileInputStream fGlycolysisGenes  = new
// 		FileInputStream("MaltoseMetabolism_and_Glycolysis.TCA.Fermentation-Reactions.txt"); 
// 	    DataInputStream inGlycolysisGenes = new DataInputStream(fGlycolysisGenes);
// 	    BufferedReader brGlycolysisGenes  = new BufferedReader(new
// 								   InputStreamReader(inGlycolysisGenes));
// 	    String strGlycolysisGenes;
// 	    Set<String> glycolysisGenesSet    = new HashSet<String>();
// 	    while ((strGlycolysisGenes = brGlycolysisGenes.readLine()) != null)   {
// 		glycolysisGenesSet.add(strGlycolysisGenes);
// 	    }

// 	    DirectedSubgraph<MetabolismVertex, DefaultEdge> glycolysis
// 		= makeSubgraph(controlGraph, glycolysisGenesSet);
// 	    DirectedSubgraph<MetabolismVertex, DefaultEdge> glySigma =
// 		makeSubgraph(sigmaGraph, glycolysisGenesSet);
// 	    DirectedSubgraph<MetabolismVertex, DefaultEdge> glyS288C =
// 		makeSubgraph(s288cGraph, glycolysisGenesSet);

// 	    System.out.println("\n--------Control [Glycolysis] Graph:");
// 	    ValuedVertex glycolysisBtwn[] = findBetweenness(glycolysis, false);

// 	    System.out.println("\n--------Sigma Graph:");
// 	    ValuedVertex glySigmaBtwn[] =
// 		findBetweenness(glySigma, false);

// 	    System.out.println("\n--------S288C Graph:");
// 	    ValuedVertex glyS288CBtwn[] =
// 		findBetweenness(glyS288C, false);

// 	    List<ValuedVertex> glyBtwnList =
// 		Arrays.asList(glycolysisBtwn);
// 	    List<ValuedVertex> glySigmaBtwnList =
// 		Arrays.asList(glySigmaBtwn);
// 	    List<ValuedVertex> glyS288CBtwnList =
// 		Arrays.asList(glyS288CBtwn);
	    
// 	    System.out.println("Sizes of graphs: Control=" +
// 			       glyBtwnList.size() + ", Sigma=" +
// 			       glySigmaBtwnList.size() + " , S288C="
// 			       + glyS288CBtwnList.size());

// 	    Integer glycolysisPathLength = glyBtwnList.size();
// 	    for ( ValuedVertex gVV : glycolysisBtwn) {
// 		for ( ValuedVertex sVV : glySigmaBtwn) {
// 		    for ( ValuedVertex cVV :glyS288CBtwn) {
// 			if ( gVV.getVertex().equals(sVV.getVertex()) &&
// 			     gVV.getVertex().equals(cVV.getVertex()) && 
// 			     !gVV.equals(sVV) && !gVV.equals(cVV) ) {
// 			    System.out.println("\nAll betweenness scores are different");
// 			    System.out.println("Control: " +
// 					       gVV.toString());
// 			    System.out.println("Sigma: " +
// 					       sVV.toString());
// 			    System.out.println("S288C: " +
// 					       cVV.toString());
// 			}
// 			if ( gVV.getVertex().equals(sVV.getVertex()) &&
// 			     sVV.getVertex().equals(cVV.getVertex()) &&
// 			     !gVV.equals(sVV) && gVV.equals(cVV) ) {
// 			    System.out.println("\nOnly Control and "\
	    // 	    "Sigma have different values"); 
// 			    System.out.println("Control: " +
// 					       gVV.toString());
// 			    System.out.println("Sigma: " +
// 					       sVV.toString());
// 			    System.out.println("S288C: " +
// 					       cVV.toString());
// 			}
// 			if ( gVV.getVertex().equals(sVV.getVertex()) &&
// 			     sVV.getVertex().equals(cVV.getVertex()) &&
// 			     !gVV.equals(cVV) && gVV.equals(sVV) ) {
// 			    System.out.println("\nOnly Control and "
// 	    "288C have different values"); 
// 			    System.out.println("Control: " +
// 					       gVV.toString());
// 			    System.out.println("Sigma: " +
// 					       sVV.toString());
// 			    System.out.println("S288C: " +
// 					       cVV.toString());
// 			}
// 			if ( gVV.getVertex().equals(sVV.getVertex()) &&
// 			     sVV.getVertex().equals(cVV.getVertex()) &&
// 			     !sVV.equals(cVV) && (gVV.equals(cVV) ||
// 						  gVV.equals(sVV)) ) {
// 			    System.out.println("\nOnly Sigma and "
// 	    " S288C have different values"); 
// 			    System.out.println("Control: " +
// 					       gVV.toString());
// 			    System.out.println("Sigma: " +
// 					       sVV.toString());
// 			    System.out.println("S288C: " +
// 					       cVV.toString());
// 			}
// 		    }
// 		}
		
// 	    }

// 	    FileInputStream fSterolGenes = new FileInputStream("sterolBiosynthesisGenes.txt");
// 	    DataInputStream inSterolGenes = new DataInputStream(fSterolGenes);
// 	    BufferedReader brSterolGenes = new BufferedReader(new InputStreamReader(inSterolGenes));
// 	    String strSterolGenes;
// 	    Set<String> sterolGenesSet = new HashSet<String>();
// 	    while ((strSterolGenes = brSterolGenes.readLine()) != null)   {
// 		sterolGenesSet.add(strSterolGenes);
// 	    }

// 	    DirectedSubgraph<MetabolismVertex, DefaultEdge> ergosterol
// 		= makeSubgraph(controlGraph, sterolGenesSet);
// 	    System.out.println("Size of Ergosterol graph="+ergosterol.vertexSet().size());
// 	    DirectedSubgraph<MetabolismVertex, DefaultEdge> ergSigma =
// 		makeSubgraph(sigmaGraph, sterolGenesSet);
// 	    DirectedSubgraph<MetabolismVertex, DefaultEdge> ergS288C =
// 		makeSubgraph(s288cGraph, sterolGenesSet);

// 	    System.out.println("\n--------Ergosterol Graph:");
// 	    ValuedVertex ergosterolBetweenness[] = findBetweenness(ergosterol, false);

// 	    System.out.println("\n--------Sigma Graph:");
// 	    ValuedVertex ergSigmaBetweenness[] =
// 		findBetweenness(ergSigma, false);

// 	    System.out.println("\n--------S288C Graph:");
// 	    ValuedVertex ergS288CBetweenness[] =
// 		findBetweenness(ergS288C, false);

// 	    List<ValuedVertex> ergBtwnList =
// 		Arrays.asList(ergosterolBetweenness);
// 	    List<ValuedVertex> ergSigmaBtwnList =
// 		Arrays.asList(ergSigmaBetweenness);
// 	    List<ValuedVertex> ergS288CBtwnList =
// 		Arrays.asList(ergS288CBetweenness);
	    
// 	    System.out.println("Sizes of graphs: Control=" +
// 			       ergBtwnList.size() + ", Sigma=" +
// 			       ergSigmaBtwnList.size() + " , S288C="
// 			       + ergS288CBtwnList.size());

// 	    Integer ergPathwayLength = ergBtwnList.size();
// 	    for (int i = 0 ; i < ergPathwayLength ; i++) {
// 		if
// 		    (!ergosterolBetweenness[i].equals(ergSigmaBetweenness[i]))
// 		    { 
// 			System.out.println("Control: " +
// 					   ergosterolBetweenness[i].toString());
// 			System.out.println("Sigma: " +
// 					   ergSigmaBetweenness[i].toString());
// 		    }
// 		if
// 		    (!ergosterolBetweenness[i].equals(ergS288CBetweenness[i]))
// 		    {
// 			System.out.println("Control: " +
// 					   ergosterolBetweenness[i].toString());
// 			System.out.println("S288C: " +
// 					   ergS288CBetweenness[i].toString());
// 		    }
// 		if
// 		    (!ergS288CBetweenness[i].equals(ergSigmaBetweenness[i]))
// 		    {
// 			System.out.println("1. Should see both this one");
// 			System.out.println("S288C: " +
// 					   ergS288CBetweenness[i].toString());
// 			System.out.println("Sigma: " +
// 					   ergSigmaBetweenness[i].toString());
// 		    }
// 		if
// 		    (!ergSigmaBetweenness[i].equals(ergS288CBetweenness[i]))
// 		    {
// 			System.out.println("2. And this one");
// 			System.out.println("Sigma: " +
// 					   ergSigmaBetweenness[i].toString());
// 			System.out.println("S288C: " +
// 					   ergS288CBetweenness[i].toString());
// 		    }
// 	    }

// 	    System.out.println("\n--------Control Graph:");
// 	    ValuedVertex controlBtwn[] = findBetweenness(controlGraph, true);

// 	    System.out.println("\n--------Sigma Graph:");
// 	    ValuedVertex sigmaBtwn[] =
// 		findBetweenness(sigmaGraph, true);

// 	    System.out.println("\n--------S288C Graph:");
// 	    ValuedVertex s288cBtwn[] =
// 		findBetweenness(s288cGraph, true);

// 	    List<ValuedVertex> controlBtwnList =
// 		Arrays.asList(controlBtwn);
// 	    List<ValuedVertex> sigmaBtwnList =
// 		Arrays.asList(sigmaBtwn);
// 	    List<ValuedVertex> s288cBtwnList =
// 		Arrays.asList(s288cBtwn);
	    
// 	    System.out.println("Sizes of graphs: Control=" +
// 			       controlBtwnList.size() + ", Sigma=" +
// 			       sigmaBtwnList.size() + " , S288C="
// 			       + s288cBtwnList.size());


// 	    Map<MetabolismVertex, Integer> sigmaBetweenness = new HashMap<MetabolismVertex,
// 		Integer>();
// 	    for (MetabolismVertex v : sigmaGraph.vertexSet()) {
// 		sigmaBetweenness.put(v, 0);
// 	    }
// 	    Map<MetabolismVertex, Integer> s288cBetweenness = new HashMap<MetabolismVertex,
// 		Integer>();
// 	    for (MetabolismVertex v : s288cGraph.vertexSet()) {
// 		s288cBetweenness.put(v, 0);
// 	    }
// 	    Map<String, String> sigmaEssentials = new HashMap<String, String>();
// 	    Map<String, String> s288cEssentials = new HashMap<String, String>();

// 	    sigmaEssentials.put("Aco1", "YLR304C");
// 	    sigmaEssentials.put("Erg2", "YMR202W");
// 	    sigmaEssentials.put("Cys3", "YAL012W");
// 	    sigmaEssentials.put("Adk1", "YDR226W"); /* Note: there are
// 						       two pathways
// 						       controlled by
// 						       this gene */
// 	    sigmaEssentials.put("Pho90", "YNR013C"); /* Note: this
// 							gene is in a
// 							giant "or" ORF
// 							tree - seems
// 							weird that
// 							it's essential */
// 	    sigmaEssentials.put("Sdh3", "YKL141W");
// 	    sigmaEssentials.put("Aat2", "YLR027C");
// 	    sigmaEssentials.put("Vps34", "YLR240W"); /* Note: in an
// 							"or" ORF tree*/
// 	    sigmaEssentials.put("Pfk26", "YIL107C"); /* Note: in an
// 							"or" ORF
// 							tree*/
// 	    sigmaEssentials.put("Zwf1", "YNL241C");


// 	    s288cEssentials.put("Ret2", "YFR051C"); /* Note: not in
// 						       iND750_network */
// 	    s288cEssentials.put("Fen1", "YCR34W");
// 	    s288cEssentials.put("Hip1", "YGR191W"); /* Note: In a
// 						       giant "or" ORF
// 						       Tree */
// 	    s288cEssentials.put("Trr1", "YDR353W");

// 	    Map<String, Integer> sigmaLethalsControl = new
// 		HashMap<String, Integer>();
// 	    Map<String, Integer> s288cLethalsControl = new
// 		HashMap<String, Integer>();
// 	    sigmaLethalsControl.put("ACO1", 0);
// 	    sigmaLethalsControl.put("ERG2", 0);
// 	    sigmaLethalsControl.put("CYS3", 0);
// 	    sigmaLethalsControl.put("ADK1", 0);
// 	    sigmaLethalsControl.put("PHO90", 0);
// 	    sigmaLethalsControl.put("SDH3", 0);
// 	    sigmaLethalsControl.put("AAT2", 0);
// 	    sigmaLethalsControl.put("VPS34", 0);
// 	    sigmaLethalsControl.put("PFK26", 0);
// 	    sigmaLethalsControl.put("ZWF1", 0);

// 	    s288cLethalsControl.put("RET2", 0);
// 	    s288cLethalsControl.put("FEN1", 0);
// 	    s288cLethalsControl.put("HIP1", 0);
// 	    s288cLethalsControl.put("TRR1", 0);

// 	    Map<String, Integer> sigmaLethalsInSigma = new
// 		HashMap<String, Integer>();
// 	    Map<String, Integer> s288cLethalsInSigma = new
// 		HashMap<String, Integer>();
// 	    sigmaLethalsInSigma.put("ACO1", 0);
// 	    sigmaLethalsInSigma.put("ERG2", 0);
// 	    sigmaLethalsInSigma.put("CYS3", 0);
// 	    sigmaLethalsInSigma.put("ADK1", 0);
// 	    sigmaLethalsInSigma.put("PHO90", 0);
// 	    sigmaLethalsInSigma.put("SDH3", 0);
// 	    sigmaLethalsInSigma.put("AAT2", 0);
// 	    sigmaLethalsInSigma.put("VPS34", 0);
// 	    sigmaLethalsInSigma.put("PFK26", 0);
// 	    sigmaLethalsInSigma.put("ZWF1", 0);

// 	    s288cLethalsInSigma.put("RET2", 0);
// 	    s288cLethalsInSigma.put("FEN1", 0);
// 	    s288cLethalsInSigma.put("HIP1", 0);
// 	    s288cLethalsInSigma.put("TRR1", 0);

// 	    Map<String, Integer> sigmaLethalsInS288C = new
// 		HashMap<String, Integer>();
// 	    Map<String, Integer> s288cLethalsInS288C = new
// 		HashMap<String, Integer>();
// 	    sigmaLethalsInS288C.put("ACO1", 0);
// 	    sigmaLethalsInS288C.put("ERG2", 0);
// 	    sigmaLethalsInS288C.put("CYS3", 0);
// 	    sigmaLethalsInS288C.put("ADK1", 0);
// 	    sigmaLethalsInS288C.put("PHO90", 0);
// 	    sigmaLethalsInS288C.put("SDH3", 0);
// 	    sigmaLethalsInS288C.put("AAT2", 0);
// 	    sigmaLethalsInS288C.put("VPS34", 0);
// 	    sigmaLethalsInS288C.put("PFK26", 0);
// 	    sigmaLethalsInS288C.put("ZWF1", 0);

// 	    s288cLethalsInS288C.put("RET2", 0);
// 	    s288cLethalsInS288C.put("FEN1", 0);
// 	    s288cLethalsInS288C.put("HIP1", 0);
// 	    s288cLethalsInS288C.put("TRR1", 0);

// 	    String begOfPath = "Ssq23epx";
// 	    String endOfPath = "ergst";

// 	    String biDirRxn = "L-valine reversible transport via proton symport";

// 	    testConnectivity(sigmaGraph, "Sigma", begOfPath,
// 			     endOfPath);
// 	    testConnectivity(s288cGraph, "S288C", begOfPath, endOfPath);
// 	    testConnectivity(controlGraph, "Control", begOfPath,
// 			     endOfPath);

// 	    ConnectivityInspector c = new
// 		ConnectivityInspector(controlGraph);
// 	    for (MetabolismVertex v : controlGraph.vertexSet()) {
// 		if (v.isRxn()) {
// 		    ORFSet orfs = v.getRxn().getORF().getAllORFs();
// 		    for (String sigmaOrf : sigmaEssentials.values()) {
// 			if (orfs.containsORF(sigmaOrf)) {
// 			    Set<MetabolismVertex> connectedSet =
// 				c.connectedSetOf(v);
// 			    Integer sizeOfConnectedSet = connectedSet.size();
// 			    System.out.println("[Sigma]\t" + sigmaOrf
// 		+ "\tSize of connected set: " + sizeOfConnectedSet);
// 			    if (sizeOfConnectedSet < 100) {
// 				System.out.println(connectedSet.toString()); }
// 			}
// 		    }
// 		    for (String s288cOrf : s288cEssentials.values()) {
// 			if (orfs.containsORF(s288cOrf)) {
// 			    Set<MetabolismVertex> connectedSet =
// 				c.connectedSetOf(v);
// 			    Integer sizeOfConnectedSet = connectedSet.size();
// 			    System.out.println("[S288C]\t" + s288cOrf
// 		+ "\tSize of connected set: " + sizeOfConnectedSet);
// 			    if (sizeOfConnectedSet < 100) {
// 				System.out.println(connectedSet.toString()); }
// 			}
// 		    }
// 		}
// 	    }
// 	    System.out.println("............ Size of graph [# vertices]:" + controlGraph.vertexSet().size());
	    
		    

// 	    Calculate betweenness for both graphs
// 	    List<DefaultEdge> shortestPath;
// 	    System.out.println("\n--------Sigma Graph:");
// 	    for (MetabolismVertex v : sigmaGraph.vertexSet()) {
// 		for (MetabolismVertex u : sigmaGraph.vertexSet()) {
// 		    shortestPath =
// 			DijkstraShortestPath.findPathBetween(sigmaGraph,
// 							     u, v);
// 		    if (shortestPath != null) {
// // 		    fw.write("\n"+shortestPath.toString());
// 			for (DefaultEdge w : shortestPath) {
// 			    for (String gene :
// 				     sigmaLethalsInSigma.keySet())
// 				{

// 				    if
// 					(
// 					shortestPath.toString().toUpperCase().contains(gene)
// 					)
// 					{
// // 					    System.out.println("\nSigma lethals in Sigma:");
// // 					    System.out.println(".." + gene +
// // 							       " " + sigmaLethalsInSigma.get(gene));
// 					    sigmaLethalsInSigma.put(gene,
// 								    sigmaLethalsInSigma.get(gene) + 1);
// 						}
// 				}
// 			    for (String gene : s288cLethalsInSigma.keySet())
// 				{

// 				    if
// 					(shortestPath.toString().toUpperCase().contains(gene))
// 					{
// // 				    System.out.println("\nS288C lethals in sigma:");
// // 					    System.out.println(",," +
// // 							       gene + " " + s288cLethalsInSigma.get(gene));
// 					    s288cLethalsInSigma.put(gene,
// 								    s288cLethalsInSigma.get(gene) + 1);
// 						}
// 				}
// 			    // count up all the edges in the path list
// // 			    			sigmaBetweenness.put(w.getSource(),
// // 			    					     sigmaBetweenness.get(w.getSource()) +
// // 			    					     1);
// 			}
// 		    }
// 		    // have to get last target since counted sources before
// // 		    sigmaBetweenness.put(shortestPath.get(shortestPath.size-1).getTarget(),
// // 					 sigmaBetweenness.get(shortestPath.get(shortestPath.size-1).getTarget()
// // 							      )+ 1);
// 		}
// 	    }
// 	    fw.write("\n..........S288C Graph:");
// 	    for (MetabolismVertex v : s288cGraph.vertexSet()) {
// 		for (MetabolismVertex u : s288cGraph.vertexSet()) {
// 		    shortestPath = 
// 			DijkstraShortestPath.findPathBetween(s288cGraph,
// 							     u, v);
// 		    if (shortestPath != null) {
// // 		    fw.write("\n"+shortestPath.toString());
// 			    for (DefaultEdge w : shortestPath) {
// // 				fw.write("\n" + w.toString());
// 			    for (String gene : sigmaLethalsInS288C.keySet())
// 				{

// 				    if
// 					(shortestPath.toString().toUpperCase().contains(gene))
// 					{
// // 				    System.out.println("\nSigma lethals in S288C:");
// // 					    System.out.println("::" +
// // 							       gene + " " + sigmaLethalsInS288C.get(gene));
// 					    sigmaLethalsInS288C.put(gene,
// 								    sigmaLethalsInS288C.get(gene) + 1);
// 						}
// 				}
// 			    for (String gene : s288cLethalsInS288C.keySet())
// 				{

// 				    if
// 					(shortestPath.toString().toUpperCase().contains(gene))
// 					{
// // 				    System.out.println("\nS288C lethals in S288C:");
// // 					    System.out.println(";;" +
// // 							       gene + " " + s288cLethalsInSigma.get(gene));
// 					    s288cLethalsInS288C.put(gene,
// 								    s288cLethalsInS288C.get(gene) + 1);
// 						}
// 				}
// 				// count up all the edges in the path list
// // 							s288cBetweenness.put(w.getSource(),
// // 									     s288cBetweenness.get(w.getSource()) +
// // 									     1);
// 			    }
// 			}
// 		    // have to get last target since counted sources before
// // 		    s288cBetweenness.put(shortestPath.get(shortestPath.size-1).getTarget(),
// // 					 s288cBetweenness.get(shortestPath.get(shortestPath.size-1).getTarget()
// // 							      )+ 1);
// 		}
// 	    }
// // 	    fw.write("\n::::::::::::Control Graph:");
// 	    for (MetabolismVertex v : controlGraph.vertexSet()) {
// 		for (MetabolismVertex u : controlGraph.vertexSet()) {
// 		    shortestPath =
// 			DijkstraShortestPath.findPathBetween(controlGraph,
// 							     u, v);
// 		    if (shortestPath != null) {
// // 		    fw.write("\n"+shortestPath.toString());
// 			for (DefaultEdge w : shortestPath) {
// 			    for (String gene : sigmaLethalsControl.keySet())
// 				{

// 				    if
// 					(shortestPath.toString().toUpperCase().contains(gene))
// 					{
// // 				    System.out.println("\nSigma lethals in control:");
// // 					    System.out.println("--" +
// // 							       gene + " " + sigmaLethalsControl.get(gene));
// 					    sigmaLethalsControl.put(gene,
// 								    sigmaLethalsControl.get(gene) + 1);
// 						}
// 				}
// 			    for (String gene : s288cLethalsControl.keySet())
// 				{

// 				    if
// 					(shortestPath.toString().toUpperCase().contains(gene))
// 					{
// // 				    System.out.println("\nS288C lethals in control:");
// // 					    System.out.println("==" +
// // 							       gene + " " + s288cLethalsControl.get(gene));
// 					    s288cLethalsControl.put(gene,
// 								    s288cLethalsControl.get(gene) + 1);
// 						}
// 				}
// 			    // count up all the edges in the path list
// 			    // 			sigmaBetweenness.put(w.getSource(),
// 			    // 					     sigmaBetweenness.get(w.getSource()) +
// 			    // 					     1);
// 			}
// 		    }
// 		    // have to get last target since counted sources before
// // 		    sigmaBetweenness.put(shortestPath.get(shortestPath.size-1).getTarget(),
// // 					 sigmaBetweenness.get(shortestPath.get(shortestPath.size-1).getTarget()
// // 							      )+ 1);
// 		}
// 	    }

// 	    System.out.println("Betweenness scores of Sigma Strain:\nSigma Essentials:\n"+sigmaLethalsInSigma.toString()+"\n\nS288C Essentials:\n"+s288cLethalsInSigma.toString());
// 	    System.out.println("Betweenness scores of S288C Strain:\nSigma Essentials:\n"+sigmaLethalsInS288C.toString()+"\n\nS288C Essentials:\n"+s288cLethalsInS288C.toString());
// 	    System.out.println("Betweenness scores of Default Network (control) Strain:\nSigma Essentials:\n"+sigmaLethalsControl.toString()+"\n\nS288C Essentials:\n"+s288cLethalsControl.toString());

// 	    TreeSet<Integer> sigmaBetweennessValues =
// 		sigmaBetweeness.values();
// 	    TreeSet<Integer> s288cBetweennessValues =
// 		s288cBetweenness.values();

// 	    System.out.println("Sigma Betweenness values: " +
// 			       sigmaBetweennessValues.toString());
// 	    System.out.println("S288C Betweenness values: " + s288cBetweennessValues.toString());
// 	}
	}
}

// }
class MetabolismVertex { 
    
    private boolean booleanRxn = false;
    private boolean booleanReactant = false;
    private Rxn rxn;
    private Reactant reactant;
    private String direction = "";
    private String name = "";
    
    public MetabolismVertex() { }

    public MetabolismVertex(Rxn r) { 
	rxn = r;
	name = r.getName();
	booleanRxn = true;
	booleanReactant = false;
	direction = "one_way";
    }

    public MetabolismVertex(Rxn r, String dir) { 
	rxn = r;
	name = r.getName() + "_" + dir;
	booleanRxn = true;
	booleanReactant = false;
	direction = dir;
    }

    public MetabolismVertex(Reactant r) {
	reactant = r;
	name = r.getName();
	booleanRxn = false;
	booleanReactant = true;
    }

    public boolean isRxn() { return booleanRxn; }
    public boolean isReactant() { return booleanReactant; }

    public Rxn getRxn() { return rxn; }
    public Reactant getReactant() { return reactant; }
    public String getName() { return name; }
    public String getDirection() { return direction; }
    // 	public Rxn setDirection(String dir) { 
    // 	    direction = dir; 
    // return metabolismVertex} 

    public boolean equals(Object o) {
	if (!(o instanceof MetabolismVertex)) { return false; }
	if (o == null) { return false; }
	MetabolismVertex mv = (MetabolismVertex)o;
	if (mv.isRxn() && this.isRxn()) {
	    return direction.equals(mv.direction) &&
		rxn.toString().equals(mv.rxn.toString()); }
	if (mv.isReactant() && this.isReactant()) {
	    return reactant.toString().equals(mv.reactant.toString()); }
	else { return false; }
    }

    public int hashCode() {
	int code = 0;
	if (this.isRxn()) { 
	    code += rxn.hashCode(); 
	    code += direction.hashCode();
	}
	if (this.isReactant()) { 
	    code += reactant.hashCode(); 
	}
	code *= 37;
	return code;
    }

    public String toString() {
	if (booleanRxn) { return rxn.toString(); } 
	if (booleanReactant) { return reactant.toString(); }
	else { return ""; }
    }
}

class ValuedVertex<N extends Integer> implements
					 Comparable<ValuedVertex<N>> { 

    public static <N extends Integer> ValuedVertex[]
    fromMap(Map<MetabolismVertex,N> m) { 
	ValuedVertex[] array = new ValuedVertex[m.size()];
	int i = 0;
	for(MetabolismVertex vert : m.keySet()) { 
	    array[i++] = new ValuedVertex(vert, m.get(vert));
	}
	return array;
    }

    private N value;
    private MetabolismVertex vertex;

    public ValuedVertex(MetabolismVertex vert, N v) { 
	vertex = vert;
	value = v;
    }

    public MetabolismVertex getVertex() { return vertex; }

    public int hashCode() { return vertex.hashCode(); }
    public boolean equals(Object o) { 
	if(!(o instanceof ValuedVertex)) { return false; }
	ValuedVertex vs = (ValuedVertex)o;
	return vertex.equals(vs.vertex) && value.equals(vs.value);
    }

    public int compareTo(ValuedVertex<N> vs) { 
	return value.compareTo(vs.value); 
    }

    public String toString() {
	return "(" + vertex.toString() + ", " + value.toString() + ")";
    }

    public MetabolismVertex vertex() { return vertex; }
    public N value() { return value; }
} 