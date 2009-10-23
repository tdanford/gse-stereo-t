/*
 * Author: tdanford
 * Date: May 5, 2009
 */
package edu.mit.csail.cgs.sigma.validation;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.utils.models.ModelFieldAnalysis;

/**
 * ParameterGrid is a helper class, that lets us quickly define grids of parameter settings, 
 * so that we can perform cross-validation or optimization over a range of those parameter values.
 * 
 * Classes should subclass the ParameterGrid model, and then define either:
 * (a) Double[] fields, or 
 * (b) GridBounds fields,
 * 
 * with the same names as public fields in a parameters (i.e., Model) class.  
 * 
 * Then, ParameterGrid does the rest -- creating objects that will iterate over all possible
 * values of the parameter settings, and allow us to automatically set the values in a corresponding
 * parameter object.
 * 
 * <pre>
 * 	class MyGrid extends ParameterGrid { 
 * 		public Double[] probSplit = new Double[] { 0.1, 0.2, 0.3 };
 * 		public Double[] probShare = new Double[] { 0.5, 0.6, 0.7 };
 * 		public GridBounds flatIntensityPenalty = new GridBounds(0.0, 1.0, 0.1);
 * 	}
 * 
 *  ...
 * 
 * 	MyGrid g = new MyGrid();
 * 	SegmentationParameters p = new SegmentationParameters();
 * 	ParameterGridState stateItr = g.createGridState();
 * 
 * 	int i = 0;
 * 	while(stateItr.hasNext()) { 
 * 		stateItr.next();
 * 		stateItr.setParameters(p);
 * 		System.out.println(String.format("%d; %s", i, p.toString()));	
 * 		i++;
 * 	}
 * </pre>
 * 
 * @author tdanford
 *
 */
public class ParameterGrid extends Model { 
	
	private ModelFieldAnalysis<ParameterGrid> analysis;
	private Map<String,Double[]> valueArrays;
	private boolean initialized;
	
	public ParameterGrid() { 
		analysis = new ModelFieldAnalysis<ParameterGrid>(this.getClass());
		valueArrays = new TreeMap<String,Double[]>();
		initialized = false;
	} 
	
	public void init() { 
		for(Field f : analysis.getFields()) {
			try { 
				String name = f.getName();
				Class type = f.getType();
				if(type.isArray()) {
					Class arrayType = type.getComponentType();
					if(Model.isSubclass(arrayType, Double.class)) { 
						Double[] values = (Double[]) f.get(this);

						if(values != null && values.length > 0) { 
							valueArrays.put(name, values);
							System.out.println(String.format(
									"%s: %d values", name, values.length));
						}
						
					}
				} else if (Model.isSubclass(type, GridBounds.class)) { 
					GridBounds bounds = (GridBounds)f.get(this);
					if(bounds != null) { 
						Double[] values = bounds.createArray();
						valueArrays.put(name, values);
						System.out.println(String.format(
								"%s: %d values", name, values.length));
					}
				}
			} catch (IllegalAccessException e) {
				System.err.println(String.format(
						"Can't access field %s in %s", f.getName(), this.toString()));
			}
		}
		initialized = true;
	}
	
	public ParameterGridState createGridState() {
		if(!initialized) { init(); }
		return new InternalParameterGridState();
	}

	private class InternalParameterGridState implements ParameterGridState {

		private String[] names;
		private Integer[] indices, limits;
		
		public InternalParameterGridState() { 
			names = valueArrays.keySet().toArray(new String[0]);
			for(int i = 0; i < names.length; i++) { 
				System.out.print(names[i] + " ");
			}
			System.out.println();
			indices = new Integer[names.length];
			limits = new Integer[names.length];
			
			for(int i = 0; i < names.length; i++) { 
				limits[i] = valueArrays.get(names[i]).length;
				indices[i] = 0;
			}
			
			// So that we see the very first element of the array, after a call to next().
			indices[0] = -1;
		}
		
		public void setParameters(Model m) { 
			ModelFieldAnalysis manalysis = new ModelFieldAnalysis(m.getClass()); 
			for(int i = 0; i < names.length; i++) {
				Field f = manalysis.findField(names[i]);
				if(f != null && Model.isSubclass(f.getType(), Double.class)) { 
					Double value = valueArrays.get(names[i])[indices[i]];
					try {
						f.set(m, value);
					} catch (IllegalAccessException e) {
						System.err.println(String.format(
								"Couldn't set field %s to value %f in %s", 
								f.getName(), value, m.toString()));
					}
				}
			}
		}

		public boolean hasNext() {
			for(int i = 0; i < names.length; i++) { 
				if(indices[i] < limits[i]-1) { 
					return true;
				}
			}
			return false;
		}

		public void next() {
			int i = 0;
			for(; i < names.length && indices[i] >= limits[i]-1; i++) { 
				// do nothing.
			}
			if(i==names.length) { 
				throw new NoSuchElementException();
			}
			
			indices[i] += 1;
			//System.out.println(String.format("%s -> %d", names[i], indices[i]));
			
			for(i = i - 1; i >= 0; i--) { 
				indices[i] = 0;
			}
		}
	}
}