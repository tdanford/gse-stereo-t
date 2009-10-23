package edu.mit.csail.cgs.sigma.motifs;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.*;

import edu.mit.csail.cgs.datasets.motifs.*;
import edu.mit.csail.cgs.datasets.species.Organism;

/**
 * @author tdanford
 * 
 * Organizes a complete set of matrices by name, version, and type.
 */
public class TotalWeightMatrices {
	
	public static void main(String[] args) { 
		MotifProperties props = new MotifProperties();
		TotalWeightMatrices matrices = new TotalWeightMatrices(props);
		
		matrices.loadMatrices();
		matrices.displayMatrices();
	}
	
	private MotifProperties props;
	private Logger logger;
	private Map<String,WeightMatrixSet> sets;
	
	public TotalWeightMatrices(MotifProperties ps) {
		props = ps;
		logger = props.getLogger("TotalWeightMatrices");
		sets = new TreeMap<String,WeightMatrixSet>();
	}
	
	public void loadMatrices() { 
		WeightMatrixLoader loader = props.createLoader();
		try {
            Organism yeast = props.getSigmaProperties().getOrganism(); 
			Collection<WeightMatrix> matrices = loader.loadMatrices(yeast);
            
            String type = props.getMotifType();
            String version = props.getMotifVersion();
			if(type == null) { type = "TAMO"; }
			if(version==null) { version="MacIsaac06"; }

			for(WeightMatrix m : matrices) {
                if(m != null && m.type != null && m.version != null && 
				m.type.equals(type) && m.version.equals(version)) { 
                    addWeightMatrix(m);
                }
			}
			
		} catch (SQLException e) {
			logger.log(Level.SEVERE, String.format("loadMatrices() SQLException: %s", e.getMessage()));
		}
		loader.close();
	}
	
	public void addWeightMatrix(WeightMatrix m) { 
		if(!sets.containsKey(m.name)) { 
			sets.put(m.name, new WeightMatrixSet(m));
		} else { 
			sets.get(m.name).addMatrix(m);
		}
	}
	
	public void displayMatrices() { 
		for(String mname : sets.keySet()) { 
			System.out.println(String.format("%s", mname));
			sets.get(mname).displayMatrices();
		}
        System.out.println(String.format("# Motif-Names: %d", sets.keySet().size()));
        System.out.println(String.format("# Motifs: %d", size()));
	}
    
    public Collection<WeightMatrix> getMatrices() { 
        LinkedList<WeightMatrix> mats = new LinkedList<WeightMatrix>();
        for(String n : sets.keySet()) { 
            mats.addAll(getMatrices(n));
        }
        return mats;
    }
	
	public Collection<WeightMatrix> getMatrices(String name) { 
		return sets.containsKey(name) ? sets.get(name).getMatrices() : 
			new LinkedList<WeightMatrix>();
	}
	
	public Collection<WeightMatrix> getMatrices(String name, String type) { 
		return sets.containsKey(name) ? sets.get(name).getMatrices(type) : 
			new LinkedList<WeightMatrix>();
	}
	
	public Collection<WeightMatrix> getMatrices(String name, String type, String version) { 
		return sets.containsKey(name) ? sets.get(name).getMatrices(type, version) : 
			new LinkedList<WeightMatrix>();
	}
	
	public boolean containsMatrix(WeightMatrix m) { 
		return sets.containsKey(m.name) ? sets.get(m.name).containsMatrix(m) : false;
	}
	
	public Set<String> getNames() { 
		TreeSet<String> names = new TreeSet<String>(sets.keySet());
		return names;
	}
    
    public int size() {
        int s = 0;
        for(String name : sets.keySet()) { 
            s += sets.get(name).size();
        }
        return s;
    }
	
	public Set<String> getTypes() { 
		TreeSet<String> values = new TreeSet<String>();
		for(String m : sets.keySet()) { 
			values.addAll(sets.get(m).matrices.keySet());
		}
		return values;
	}

	public Set<String> getVersions() { 
		TreeSet<String> values = new TreeSet<String>();
		for(String m : sets.keySet()) {
			for(String t : sets.get(m).matrices.keySet()) { 
				values.addAll(sets.get(m).matrices.get(t).keySet());
			}
		}
		return values;
	}
}

class WeightMatrixSet { 
	
	// type -> version -> matrix-set.
	public Map<String,Map<String,Set<WeightMatrix>>> matrices;
	public String name;
	
	public WeightMatrixSet(WeightMatrix m) { 
		name = m.name;
		matrices = new HashMap<String,Map<String,Set<WeightMatrix>>>();
		addMatrix(m);
	}
	
	public boolean containsMatrix(WeightMatrix m) { 
		if(!name.equals(m.name)) { return false; }
		if(!matrices.containsKey(m.type) || !matrices.get(m.type).containsKey(m.version)) { 
			return false;
		}
		return matrices.get(m.type).get(m.version).contains(m);
	}
    
    public int size() { 
        int s = 0;
        for(String type : matrices.keySet()) { 
            for(String version : matrices.get(type).keySet()) { 
                s += matrices.get(type).get(version).size();
            }
        }
        return s;
    }
	
	public Collection<WeightMatrix> getMatrices() { 
		LinkedList<WeightMatrix> wms = new LinkedList<WeightMatrix>();
		for(String type : matrices.keySet()) { 
			for(String version : matrices.get(type).keySet()) { 
				wms.addAll(matrices.get(type).get(version));
			}
		}
		return wms;
	}
	
	public Collection<WeightMatrix> getMatrices(String type) {
		if(!matrices.containsKey(type)) { 
			return new LinkedList<WeightMatrix>();
		}
		LinkedList<WeightMatrix> wms = new LinkedList<WeightMatrix>();
		for(String version : matrices.get(type).keySet()) { 
			wms.addAll(matrices.get(type).get(version));
		}
		return wms;		
	}
	
	public Collection<WeightMatrix> getMatrices(String type, String version) { 
		if(!matrices.containsKey(type) || !matrices.get(type).containsKey(version)) { 
			return new LinkedList<WeightMatrix>();
		} else { 
			return matrices.get(type).get(version);
		}
	}
	
	public void addMatrix(WeightMatrix m) {
		if(!name.equals(m.name)) { throw new IllegalArgumentException(m.name); }
		if(!matrices.containsKey(m.type)) { 
			matrices.put(m.type, new TreeMap<String,Set<WeightMatrix>>());
		}
		
		if(!matrices.get(m.type).containsKey(m.version)) { 
			matrices.get(m.type).put(m.version, new LinkedHashSet<WeightMatrix>());
		}
		
		matrices.get(m.type).get(m.version).add(m);
	}
	
	public void displayMatrices() { 
		for(String type : matrices.keySet()) { 
			System.out.println(String.format("\t%s", type));
			for(String version : matrices.get(type).keySet()) { 
				System.out.println(String.format("\t\t%s", version));
				Set<WeightMatrix> mats = matrices.get(type).get(version);
				for(WeightMatrix matrix : mats) { 
					int cols = matrix.matrix.length;
					System.out.println(String.format("\t\t\t%d cols", cols));
				}
			}
		}
	}
}
