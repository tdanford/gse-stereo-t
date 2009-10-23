package edu.mit.csail.cgs.sigma.metabolism;

import java.io.PrintStream;
import java.util.*;
import java.util.regex.*;

import edu.mit.csail.cgs.utils.Pair;

/**
 * LogicalORFTree
 * @author tdanford
 *
 * Each element of this class represents what proteins/genes are related to a particular
 * entry of the metabolic network.  It is a "tree" because the proteins in each reaction
 * are often structured in a parenthetical way -- and it's "logical" because the combinators
 * used to combine the proteins for each reaction are 'AND' and 'OR'.  
 * 
 * It's so *complicated* because the people who compiled the data file for the Duarte paper
 * used a --ridiculous-- parenthetical scheme; ridiculous because it's totally inconsistent.
 * Sometimes you'll see "(A or B)", and sometimes "(A) or (B)".  But we want to represent
 * both of these combinations in a homogeneous way.  Therefore, all the crazy logic for parsing
 * goes into *this* file, and none of that weird structuring/formatting is shown to the end
 * user.
 */
public class LogicalORFTree {
	
	public static String removeSpaces(String s) {  
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < s.length(); i++) { 
			if(!Character.isWhitespace(s.charAt(i))) { 
				sb.append(s.charAt(i));
			}
		}
		return sb.toString();
	}
	
	public static enum LogicalType { AND, OR, VALUE };
	
	private LogicalType type;
	private String value;
	private Vector<LogicalORFTree> subtrees;
	
	public LogicalORFTree(String s) { 
		s = s.trim();
		Parenthesization p = new Parenthesization(s);
		Object t = p.createTree();
		
		if(t instanceof Vector) { 
			initFromVector((Vector)t);
		} else { 
			type = LogicalType.VALUE;
			subtrees = null;
			value = t.toString();
		}
	}
	
	public LogicalORFTree(Vector v) { 
		initFromVector(v);
	}

	public LogicalORFTree(LogicalType t, String v, Collection<LogicalORFTree> sts) { 
		type = t;
		value = v;
		subtrees = new Vector<LogicalORFTree>();
		if(sts != null) { subtrees.addAll(sts); }
	}
	
	private Double min(Double v1, Double v2) { 
		if(v1 == null) { return v2; }
		if(v2 == null) { return v1; }
		return Math.min(v1, v2);
	}
	
	private Double max(Double v1, Double v2) { 
		if(v1 == null) { return v2; }
		if(v2 == null) { return v1; }
		return Math.max(v1, v2);
	}
	
	public Double findORFValue(Map<String,Double> orfValues) { 
		switch(type) { 
		
		case AND:
			Double min = null;
			for(LogicalORFTree t : subtrees) {
				Double stValue = t.findORFValue(orfValues);
				min = min(min, stValue);
			}
			return min;
			
		case OR:
			Double max = null;
			for(LogicalORFTree t : subtrees) {
				Double stValue = t.findORFValue(orfValues);
				max = max(max, stValue);
			}
			return max;
			
		case VALUE:
			return orfValues.containsKey(value) ? orfValues.get(value) : null;
		}
		
		return null;		
	}
	
	public boolean isSatisfiedBy(ORFSet orfs) { 
		switch(type) { 
		
		case AND:
			for(LogicalORFTree t : subtrees) { 
				if(!t.isSatisfiedBy(orfs)) { 
					return false;
				}
			}
			return true;
			
		case OR:
			for(LogicalORFTree t : subtrees) { 
				if(t.isSatisfiedBy(orfs)) { 
					return true;
				}
			}
			return false;
			
		case VALUE:
			return orfs.containsORF(value);
		}
		
		return false;
	}
	
	private static Vector cleanTree(Vector t) {
		Vector newv = new Vector();
		Iterator itr = t.iterator();
		
		while(itr.hasNext()) { 
			Object val = itr.next();
			if(val instanceof Vector) { 
				Vector sv = cleanTree((Vector)val);
				if (sv.size() == 1) { 
					newv.add(sv.get(0));
				} else if(!sv.isEmpty()) { 
					newv.add(sv);
				} 
			} else { 
				String str = (String)val;
				String[] a = str.split("\\s+");
				for(int i = 0; i < a.length; i++) { 
					if(a[i].length() > 0) { 
						newv.add(a[i].toUpperCase());
					}
				}
			}
		}
		
		if(newv.size() == 1 && newv.get(0) instanceof Vector) { 
			return (Vector)newv.get(0);
		}
		
		return newv;
	}
	
	public static boolean isConnective(String a) { 
		return a.equals("AND") || a.equals("OR");
	}
	
	public static String findConnective(Vector v) { 
		String cxn = null;
		
		for(Object val : v) { 
			if(val instanceof String) { 
				String s = (String)val;
				if(isConnective(s)) { 
					if(cxn == null) { 
						cxn = s;
					} else if (!cxn.equals(s)) { 
						return null;
					}
				}
			}
		}
		
		return cxn;
	}
	
	private static LinkedList findNonConnectives(Vector v) { 
		LinkedList ncxns = new LinkedList();
		for(Object val : v) { 
			if(val instanceof String) { 
				String s = (String)val;
				if(!isConnective(s)) { 
					ncxns.add(s);
				}
			} else { 
				ncxns.add(val);
			}
		}
		return ncxns;
	}
	
	public void printTree(PrintStream ps) { printTree(ps, 0); }
	
	public void printTree(PrintStream ps, int depth) { 
		for(int i = 0; i < depth; i++) { ps.print("  "); }
		if(type.equals(LogicalType.VALUE)) { 
			ps.println(String.format("*%s", value));
		} else { 
			ps.println(String.format("+%s", type.toString()));
			for(LogicalORFTree tree : subtrees) { 
				tree.printTree(ps, depth+1);
			}
		}
	}

	private void initFromVector(Vector parenTree) {
		parenTree = cleanTree(parenTree);
		//System.out.println("----------------------------------");
		//Parenthesization.printTree(parenTree, System.out, 0);

		String cxn = findConnective(parenTree);
		LinkedList objs = findNonConnectives(parenTree);
		
		if(cxn == null) {
			if(objs.size() != 1) { throw new IllegalArgumentException(); }
			type = LogicalType.VALUE;
			subtrees = null;
			value = (String)objs.getFirst();
		} else { 
			type = LogicalType.valueOf(cxn);
			value = null;
			subtrees = new Vector<LogicalORFTree>();
			for(Object obj : objs) { 
				if(obj instanceof String) { 
					String s = (String)obj;
					subtrees.add(new LogicalORFTree(LogicalType.VALUE, s, null));
				} else { 
					subtrees.add(new LogicalORFTree((Vector)obj));
				}
			}
		}
	}
	
	public LogicalType getType() { return type; }
	public boolean isOr() { return type.equals(LogicalType.OR); }
	public boolean isAnd() { return type.equals(LogicalType.AND); }
	public boolean isValue() { return type.equals(LogicalType.VALUE); }
	
	public String getValue() { return value; }
	public Collection<LogicalORFTree> getSubtrees() { return subtrees; }
	
	public ORFSet getAllORFs() {  
		ORFSet orfs = new ORFSet();
		if(isValue()) { 
			orfs.addORF(value);
		} else { 
			for(LogicalORFTree tree : subtrees) { 
				orfs.addORFSet(tree.getAllORFs());
			}
		}
		return orfs;
	}
	
	
}

