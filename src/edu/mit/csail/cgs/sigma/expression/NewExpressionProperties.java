/*
 * Author: tdanford
 * Date: Oct 18, 2008
 */
package edu.mit.csail.cgs.sigma.expression;

import java.util.*;

import edu.mit.csail.cgs.datasets.locators.ChipChipLocator;
import edu.mit.csail.cgs.sigma.SigmaProperties;

public class NewExpressionProperties extends BaseExpressionProperties {
	
	public static void main(String[] args) { 
		NewExpressionProperties neps = new NewExpressionProperties("new_stacie");
		for(String key : neps.getExptKeys()) { 
			System.out.println(key);
		}
	}
	
	private ResourceBundle bundle;
	private String name;
	private Map<String,KeyProperties> keyProps;
	
	public NewExpressionProperties() { this("new_sudeep"); }
	
	public NewExpressionProperties(String n) { 
		super(new SigmaProperties(), n);
		name = n;
		keyProps = new TreeMap<String,KeyProperties>();
		bundle = ResourceBundle.getBundle(
				String.format("edu.mit.csail.cgs.sigma.expression.%s", name));
		
		String[] array = bundle.getString("s288c_expt_keys").split(",");
		for(int i = 0; i < array.length; i++) {
			String arrayKey = array[i].trim();
			if(arrayKey.length() > 0) { 
				KeyProperties kps = new KeyProperties(bundle, arrayKey);
				keyProps.put(kps.exptKey, kps);
			}
		}
		array = bundle.getString("sigma_expt_keys").split(",");
		for(int i = 0; i < array.length; i++) { 
			String arrayKey = array[i].trim();
			if(arrayKey.length() > 0) { 
				KeyProperties kps = new KeyProperties(bundle, arrayKey);
				keyProps.put(kps.exptKey, kps);
			}
		}
	}

	@Override
	public Set<String> getArrayExptKeys(String exptKey) {
		TreeSet<String> keys = new TreeSet<String>();
		keys.add(exptKey);
		return keys;
	}

	@Override
	public Collection<String> getExptKeys() {
		return keyProps.keySet();
	}

	@Override
	public ChipChipLocator getLocator(String key) {
		KeyProperties ps = keyProps.get(key);
		return super.createLocator(ps.strain, ps.name, ps.version, ps.replicate);
	}

	@Override
	public boolean isIPData(String exptKey) {
		return keyProps.get(exptKey).isIP;
	}

	@Override
	public String parseStrainFromExptKey(String key) {
		return keyProps.get(key).strain;
	}

}

class KeyProperties { 
	
	public String exptKey;
	public String name, version, replicate;
	public String strain;
	public boolean isIP;
	
	public KeyProperties(ResourceBundle bundle, String key) { 
		exptKey = key;
		name = bundle.getString(String.format("%s_name", exptKey));
		version = bundle.getString(String.format("%s_version", exptKey));
		replicate = bundle.getString(String.format("%s_replicate", exptKey));
		strain = bundle.getString(String.format("%s_strain", exptKey));
		isIP = bundle.getString(String.format("%s_ip", exptKey)).equals("true");
	}
	
	public String toString() { return exptKey; }
	
	public int hashCode() { return exptKey.hashCode(); }
	
	public boolean equals(Object o) { 
		if(!(o instanceof KeyProperties)) { return false; }
		KeyProperties p = (KeyProperties)o;
		return p.exptKey.equals(exptKey);
	}
}
