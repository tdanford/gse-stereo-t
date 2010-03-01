package edu.mit.csail.cgs.cgstools.tgraphs;

import java.util.*;
import java.lang.reflect.*;

import edu.mit.csail.cgs.utils.ArrayUtils;
import edu.mit.csail.cgs.utils.graphs.DirectedAlgorithms;
import edu.mit.csail.cgs.utils.graphs.DirectedCycleChecker;
import edu.mit.csail.cgs.utils.graphs.DirectedGraph;
import edu.mit.csail.cgs.utils.models.*;
import edu.mit.csail.cgs.cgstools.tgraphs.asynch.*;
import edu.mit.csail.cgs.cgstools.tgraphs.patterns.*;

public class GraphQuery extends Model {
	
	/**
	 * Returns the ordered set of patterns that should be piped together to produce
	 * the ultimate answer to the query.  This method will make sure that the order
	 * returned respects the dependencies of each of the queries, and may even try 
	 * some simple optimizations on the ordering.
	 * 
	 * It will throw an exception (IllegalStateException) if the pattern dependencies
	 * of the query are circular.
	 * 
	 * The face that we're returning an array (that is, a linear order) of access 
	 * patterns is equivalent to the fact that we're looking at left-deep (or right-deep,
	 * whatever) access patterns.  
	 * 
	 * @return
	 */
	public GraphPattern[] queryPath() { 
		Accessor[] paccs = patternFields();
		String[] pnames = accessorNames(paccs);

		GraphPattern[] patterns = patternObjects(paccs);
		DirectedGraph deps = patternDependencies();
		
		DirectedCycleChecker cycler = new DirectedCycleChecker(deps);
		if(cycler.containsCycle()) { 
			throw new IllegalStateException(
					String.format("Pattern dependencies contain cycle."));
		}
		
		DirectedAlgorithms algos = new DirectedAlgorithms(deps);
		Vector<String> topo = algos.getTopologicalOrdering();
		LinkedList<GraphPattern> topoPatts = new LinkedList<GraphPattern>();
		
		for(int i = 0; i < topo.size(); i++) { 
			String name = topo.get(i);
			int pidx = stringArrayFind(pnames, name);
			topoPatts.addFirst(patterns[pidx]);
		}
		
		GraphPattern[] basic = topoPatts.toArray(new GraphPattern[0]);
		String[] basicNames = topo.toArray(new String[0]);
		
		optimize(basic, basicNames, deps);
		
		return basic;
	}
	
	/**
	 * Re-arranges the access path in a way that will attempt to basic optimizations 
	 * -- the essential method, right now, is that it tries to replace the methods which 
	 * introduce nodes "ab initio" (the Node class) with any later Edge classes that 
	 * lead to those same nodes.  
	 * 
	 * @param patterns
	 * @param names
	 * @param deps
	 */
	private static void optimize(GraphPattern[] patterns, String[] names, DirectedGraph deps) {
		for(int i = 0; i < patterns.length; i++) { 
			Class pclass = patterns[i].getClass();
			if(Model.isSubclass(pclass, Node.class)) { 
				Node nodePattern = (Node)patterns[i];
				String npName = names[i];
				String nodeName = nodePattern.nodeName();
				
				found: for(int j = i + 1; j < patterns.length; j++) { 
					if(Model.isSubclass(patterns[j].getClass(), Edge.class)) { 
						Edge edgePattern = (Edge)patterns[j];
						String epName = names[j];
						
						if(edgePattern.fromNode().equals(nodeName) || 
								edgePattern.toNode().equals(nodeName)) {
							
							String[] intro = introduced(patterns, i+1, j);
							if(!stringArraysOverlap(intro, edgePattern.dependencies())) {
								
								for(int k = j; k > i + 1; k--) { 
									patterns[k] = patterns[k-1];
									names[k] = names[k-1];
								}
								
								patterns[i] = edgePattern; 
								names[i] = epName; 
								patterns[i+1] = nodePattern;
								names[i+1] = npName;
								
								break found; 
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Finds the names (as an array) of variables that are introduced by 
	 * a range [i1, i2) of the given patterns in an array.  Used by optimize(), to 
	 * make sure that patterns aren't flipped around in a way that violates their
	 * dependencies.  
	 * 
	 * @param p
	 * @param i1
	 * @param i2
	 * @return
	 */
	private static String[] introduced(GraphPattern[] p, int i1, int i2) { 
		int n = 0; 
		for(int i = i1; i < i2; i++) { 
			n += p[i].names().length;
		}
		String[] intro = new String[n]; 
		int j = 0;
		for(int i = i1; i < i2; i++) { 
			String[] nms = p[i].names();
			for(int k = 0; k < nms.length; k++, j++) { 
				intro[j] = nms[k];
			}
		}
		return intro;
	}
	
	/**
	 * Each pattern introduces a set of names, and depends on a set of names.  
	 * The dependency graph between patterns is a directed graph where P1 depends on 
	 * P2 (there exists an edge from P1 to P2 in the dep-graph) if P2 introduces 
	 * a name which P1 depends on.  
	 * 
	 * @return
	 */
	public DirectedGraph patternDependencies() { 
		DirectedGraph deps = new DirectedGraph();

		Accessor[] paccs = patternFields();
		GraphPattern[] patterns = patternObjects(paccs);
		String[] pnames = accessorNames(paccs);
		
		for(int i = 0; i < paccs.length; i++) { 
			deps.addVertex(pnames[i]);
		}
		
		for(int i = 0; i < patterns.length; i++) { 
			for(int j = 0; j < patterns.length; j++) { 
				if(i != j) {
					// The basic dependency: 
					// One pattern I introduces (via names()) a variable that another 
					// pattern J depends on (via dependencies()).  In this case, 
					// patern I depends on pattern J.  
					if(patternDepends(patterns[i], patterns[j])) { 
						deps.addEdge(pnames[i], pnames[j]);
					}
					
					// A slightly more subtle requirement: NodePatterns can be 
					// used to introduce variables, or to further restrict the values 
					// of those variables after they've been introduced by some other 
					// (non-Node) pattern.  For example, Edge and Path patterns can 
					// introduce variables which can be further restricted by Node patterns.
					// Therefore, if we see two patterns I and J, and if one of them 
					// (say, I) is a Node pattern, and the other is not, and if I 
					// introduces a node which J *also* introduces -- then we want to 
					// put I after J.  Therefore, we introduce a dependency from I to J.  
					if(patterns[i] instanceof NodePattern) { 
						if(!(patterns[j] instanceof NodePattern)) {  
							if(stringArrayContains(patterns[j].names(), patterns[i].names())) { 
								deps.addEdge(pnames[i], pnames[j]);
							}
						}
					}
				}
			}
		}
		
		return deps;
	}
	
	private GraphPattern[] patternObjects(Accessor[] fields) { 
		GraphPattern[] patts = new GraphPattern[fields.length];
		for(int i = 0; i < fields.length; i++) { 
			patts[i] = (GraphPattern)fields[i].get(this);
		}
		return patts;
	}
	
	private String[] accessorNames(Accessor[] fields) { 
		String[] patts = new String[fields.length];
		for(int i = 0; i < fields.length; i++) { 
			patts[i] = fields[i].getName();
		}
		return patts;
	}
	
	public Accessor[] patternFields() { 
		HashSet<GraphQuery> qs = new HashSet<GraphQuery>();
		return patternFields(qs);
	}
	
	private Accessor[] patternFields(Set<GraphQuery> parentQueries) {
		if(parentQueries.contains(this)) { return new Accessor[] {}; }
		
		ModelFieldAnalysis analysis = new ModelFieldAnalysis(getClass());
		Vector<Field> fields = analysis.findTypedFields(GraphPattern.class);
		Accessor[] accs = new Accessor[fields.size()];
		for(int i = 0; i < accs.length; i++) { 
			accs[i] = new FieldAccessor(fields.get(i)); 
		}
		
		Accessor[] qaccs = subQueryFields();
		for(Accessor qacc : qaccs) { 
			GraphQuery subQuery = (GraphQuery)qacc.get(this);
			parentQueries.add(this);
			accs = ArrayUtils.cat(accs, subQuery.patternFields());
		}
		
		return accs;
	}
	
	private Accessor[] subQueryFields() { 
		ModelFieldAnalysis analysis = new ModelFieldAnalysis(getClass());
		Vector<Field> fields = analysis.findTypedFields(GraphQuery.class);
		Accessor[] accs = new Accessor[fields.size()];
		for(int i = 0; i < accs.length; i++) { 
			accs[i] = new FieldAccessor(fields.get(i)); 
		}
		return accs;
	}
	
	/** Static Helper Methods ******************************************************/
	
	public static boolean patternConflicts(GraphPattern p1, GraphPattern p2) {
		return stringArraysOverlap(p1.names(), p2.names());
	}
	
	public static boolean patternDepends(GraphPattern p1, GraphPattern p2) {
		return stringArraysOverlap(p2.names(), p1.dependencies());
	}
	
	public static boolean stringArrayEquals(String[] s1, String[] s2) { 
		if(s1.length != s2.length) { return false; }
		for(int i = 0; i < s1.length; i++) { 
			if(!s1[i].equals(s2[i])) { 
				return false;
			}
		}
		return true;
	}
	
	public static boolean stringArrayContains(String[] superset, String[] subset) {  
		for(int i = 0; i < subset.length; i++) { 
			if(stringArrayFind(superset, subset[i]) == -1) { 
				return false;
			}
		}
		return true;
	}
	
	public static boolean stringArraysOverlap(String[] a1, String[] a2) { 
		return stringArraysOverlap(a1, a2, 0, a1.length);
	}
	
	public static boolean stringArraysOverlap(String[] a1, String[] a2, int i1, int i2) { 
		for(int i = i1; i < i2; i++) { 
			if(stringArrayFind(a2, a1[i]) != -1) { 
				return true;
			}
		}
		return false;
	}
	
	public static int stringArrayFind(String[] array, String value) { 
		return stringArrayFind(array, value, 0, array.length);
	}
	
	public static int stringArrayFind(String[] array, String value, int i1, int i2) { 
		for(int i = i1; i < i2; i++) { 
			if(array[i].equals(value)) { 
				return i;
			}
		}
		return -1;
	}
}
