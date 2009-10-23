/*
 * Created on Nov 29, 2007
 *
 * TODO 
 * 
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.csail.cgs.sigma.metabolism;

import java.util.*;
import java.util.regex.*;

public class Rxn {
    
    private static Pattern locPattern = Pattern.compile("^\\s*\\[([^\\[\\]]+)\\]\\s*:\\s*(.*)$");
    private static Pattern countPattern = Pattern.compile("\\s*\\(([\\.\\d]+)\\)\\s*(.*)\\s*");
    private static Pattern uniPattern = Pattern.compile("(.*)\\s*-->\\s*(.*)");
    private static Pattern biPattern = Pattern.compile("(.*)\\s*<==>\\s*(.*)");
    private static Pattern locatedPattern = Pattern.compile("(.*)\\s*\\[(.*)\\]");
    
    public static enum Type { UNIDIRECTIONAL, BIDIRECTIONAL };
    public static final String UNKNOWN = "?";

    private Map<Reactant,Double> leftReact, rightReact;
    private Type rxnType;
    private String location;
    private String abbreviation, name;
    private LogicalORFTree orfTree;
    
    public Rxn(MetabolismProperties ps, String rStr, String abb, String nme, String orf) { 
        Matcher m1 = locPattern.matcher(rStr);
        if(m1.matches()) { 
            location = m1.group(1);
            rStr = m1.group(2);
        } else { 
            location = UNKNOWN;
        }
        
        abbreviation = abb;
        name = nme;
        orfTree = new LogicalORFTree(orf);
        
        leftReact = new LinkedHashMap<Reactant,Double>();
        rightReact = new LinkedHashMap<Reactant,Double>();
        
        Matcher m2 = uniPattern.matcher(rStr);
        Matcher m3 = biPattern.matcher(rStr);
        
        if(m2.matches()) {
            rxnType = Type.UNIDIRECTIONAL;
            String left = m2.group(1), right = m2.group(2);
            parseLeft(ps, left);
            parseRight(ps, right);
            
        } else if(m3.matches()) {
            rxnType = Type.BIDIRECTIONAL;
            String left = m3.group(1), right = m3.group(2);
            parseLeft(ps, left);
            parseRight(ps, right);
            
        } else { 
            throw new IllegalArgumentException(rStr);
        }
        
        System.out.println("RXN: " + toString());
    }
    
    public String getName() { return name; }
    public String getAbbreviation() { return abbreviation; }
    public LogicalORFTree getORF() { return orfTree; }
    
    public boolean connects(Rxn r) { 
        for(Reactant rr : rightReact.keySet()) { 
            if(!rr.isTrivial() && r.leftReact.containsKey(rr)) { 
                return true;
            }
        }

        if(rxnType.equals(Type.BIDIRECTIONAL)) { 
            for(Reactant rr : leftReact.keySet()) { 
                if(!rr.isTrivial() && r.leftReact.containsKey(rr)) { 
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void parseLeft(MetabolismProperties props, String left) {
        String[] a = left.split("\\+");
        for(int i = 0; i < a.length; i++) { 
            String loc = location;
            String at = a[i].trim();
            Matcher m = countPattern.matcher(at);
            double count = 1;
            if(m.matches()) { 
                count = Double.parseDouble(m.group(1));
                at = m.group(2);
            }
            
            m = locatedPattern.matcher(at);
            if(m.matches()) { 
                at = m.group(1);
                loc = m.group(2);
            }
            
            leftReact.put(new Reactant(at, loc, props), count);
        }
    }
    
    private void parseRight(MetabolismProperties props, String right) { 
        String[] a = right.split("\\+");
        for(int i = 0; i < a.length; i++) { 
            String loc = location;
            String at = a[i].trim();
            Matcher m = countPattern.matcher(at);
            double count = 1.0;
            if(m.matches()) { 
                count = Double.parseDouble(m.group(1));
                at = m.group(2);
            }

            m = locatedPattern.matcher(at);
            if(m.matches()) { 
                at = m.group(1);
                loc = m.group(2);
            }
            
            rightReact.put(new Reactant(at, loc, props), count);
        }
    }
    
    public String getLocation() { return location; }
    public Type getType() { return rxnType; }
    public Set<Reactant> getLeftReact() { return leftReact.keySet(); }
    public Set<Reactant> getRightReact() { return rightReact.keySet(); }
    public double getLeftCount(Reactant r) { return leftReact.get(r); }
    public double getRightCount(Reactant r) { return rightReact.get(r); }
    
    public int hashCode() { 
    	int code = 17;
    	code += abbreviation.hashCode(); code *= 37;
    	code += name.hashCode(); code *= 37;
    	return code;
    }
    
    public boolean equals(Object o) { 
    	if(!(o instanceof Rxn)) { return false; }
    	Rxn r = (Rxn)o;
    	if(!name.equals(r.name)) { return false; }
    	if(!abbreviation.equals(r.abbreviation)) { return false; }
    	return true;
    }
    
    public String toString() { 
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s [%s] : ", abbreviation, location));
        for(Reactant r : leftReact.keySet()) { 
            sb.append(r.getName());
            if(!r.getLocation().equals(location)) { 
                sb.append(String.format("[%s]", r.getLocation()));
            }
            sb.append(String.format("{x %.1f} ", leftReact.get(r)));
        }
        sb.append(rxnType.equals(Type.UNIDIRECTIONAL) ? "--> " : "<==> ");
        for(Reactant r : rightReact.keySet()) { 
            sb.append(r.getName());
            if(!r.getLocation().equals(location)) { 
                sb.append(String.format("[%s]", r.getLocation()));
            }
            sb.append(String.format("{x %.1f} ", rightReact.get(r)));
        }
        return sb.toString();
    }
}


