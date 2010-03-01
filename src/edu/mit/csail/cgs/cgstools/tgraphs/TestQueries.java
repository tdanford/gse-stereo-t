/*
 * Author: tdanford
 * Date: Apr 19, 2009
 */
package edu.mit.csail.cgs.cgstools.tgraphs;

import java.io.*;
import java.util.*;

import edu.mit.csail.cgs.cgstools.tgraphs.asynch.*;
import edu.mit.csail.cgs.cgstools.tgraphs.patterns.*;
import edu.mit.csail.cgs.utils.graphs.GraphModel;

public class TestQueries {

	public static DefaultTaggedGraph graph = new DefaultTaggedGraph();

	public static void main(String[] args) { 
		graph.addTaggedGraph("a", new String[] { 
				"w,x", "x,y", "y,z", });
		graph.addTaggedGraph("b", new String[] { 
				"z,f", "f,g", "g,h" });

		evalQuery(new TestPathQuery());
	}
	
	public static void printIterator(Iterator<String> itr) { 
		while(itr.hasNext()) { 
			System.out.println(itr.next());
		}
		System.out.println();
	}
	
	public static void evalQuery(GraphQuery query) { 
		//QueryEvaluator eval = new SerialQueryEvaluator(graph);
		QueryEvaluator eval = new AsynchQueryEvaluator(graph);
		
		GraphPattern[] path = query.queryPath();
		
		System.out.println("Query: ");
		SerialQueryEvaluator.printQueryPath(path, System.out);
		System.out.println();
		
		printResults(eval.eval(query));
	}
	
	public static void printResults(Iterator<GraphContext> ctxts) { 
		System.out.println("Results: "); 
		int c = 0;
		while(ctxts.hasNext()) { 
			GraphContext ctxt = ctxts.next();
			System.out.println(String.format("%d: %s", c++, ctxt.toString()));
		}
		System.out.println(String.format("# Results: %d", c));
	}
	
	public static String deftype = "default";
	
	public static class TestQuery extends GraphQuery { 
		public Node v1 = new Node("v1", deftype);
		public Node v2 = new Node("v2", deftype);
		public Node v3 = new Node("v3", deftype);
		public Edge e1 = new Edge(v1, v2, "a"); 
		public Edge e2 = new Edge(v2, v3, "b");
	}
	
	public static class TestPathQuery extends GraphQuery { 
		public Node v1 = new Node("v1", deftype);
		public Node v2 = new Node("v2", deftype);
		public Node v3 = new Node("v3", deftype);
		public Path p1 = new Path("p1", v1, v2, "a");
		//public Edge e2 = new Edge(v2, v3, "b");
		public Path p2 = new Path("p2", v2, v3, "b");
		//public Unique uniq = new Unique(v1, v2, v3);
	}
}
