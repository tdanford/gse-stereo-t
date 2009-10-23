/*
 * Author: tdanford
 * Date: Mar 2, 2009
 */
package edu.mit.csail.cgs.sigma.litdata;

import java.io.*;
import java.util.*;

import edu.mit.csail.cgs.ewok.verbs.Mapper;
import edu.mit.csail.cgs.ewok.verbs.MapperIterator;
import edu.mit.csail.cgs.utils.Predicate;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.viz.eye.ModelScatter;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;

public class TableParser implements MicroarrayExpression {
	
	public static void main(String[] args) { 
		File dir = new File(
				"C:\\Documents and Settings\\tdanford\\Desktop\\Microarray_Platforms\\GEO\\" +
				//"GPL2529");
				//"GPL90");
				"Iyer");

		try {
			//TableParser parser = new TableParser(new File(dir, "table_output.txt"));
			TableParser parser = new TableParser(new File(dir, "table_output.txt"));
			//parser.normalize();
			//parser.saveFile(new File(dir, "normalized_table_output.txt"));

			/*
			TableParser parser = new TableParser();
			parser.loadBinary(new File(dir, "table_output.xpr"));
			*/

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static class XYPoint extends Model { 
		
		public Double x, y;
		
		public XYPoint(Float f1, Float f2) { 
			x = (double)f1; y = (double)f2;
		}
		
		public XYPoint(Double f1, Double f2) { 
			x = (double)f1; y = (double)f2;
		}
		
		public XYPoint() {}
	}

	private Vector<Column> cols;
	private Map<String,Integer> ids;
	
	private Vector<String> samples;
	private Map<String,Integer> sampleIndices;
	
	public void normalize() { 
		for(int i = 0; i < samples.size(); i++) { 
			TreeSet<Column.Entry> entries = new TreeSet<Column.Entry>();
			for(int j = 0; j < cols.size(); j++) { 
				Column c = cols.get(j);
				if(c.values.get(i) != null) { 
					entries.add(c.entry(i));
				}
			}
			
			int ns = entries.size();
			int j = 0;
			for(Column.Entry e : entries) { 
				int cidx = ids.get(e.id());
				float ff = (float)j / (float)ns;
				cols.get(cidx).values.set(i, ff);
			}

			if(i > 0) { 
				if(i % 100 == 0) { System.out.print("."); System.out.flush(); }
			}
		}
		System.out.println();
	}
	
	public int findSampleIndex(String sample) { return sampleIndices.get(sample); }
	
	public Collection<String> findProbesInSample(String samp, Predicate<Float> valuePredicate) { 
		ArrayList<String> plist = new ArrayList<String>();
		int idx = sampleIndices.get(samp);
		for(Column c : cols) { 
			if(valuePredicate.accepts(c.values.get(idx))) { 
				plist.add(c.id);
			}
		}
		return plist;
	}
	
	public TableParser() { 
		ids = new TreeMap<String,Integer>();
		samples = new Vector<String>();
		sampleIndices = new TreeMap<String,Integer>();
		cols = new Vector<Column>();
	}
	
	public TableParser(File f) throws IOException {
		this();
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = br.readLine();
		String[] array = line.split("\t");
		for(int i = 1; i < array.length; i++) { 
			String id = array[i];
			cols.add(new Column(id));
			ids.put(id, cols.size()-1);
		}
		
		parseFile(br);
		br.close();
	}
	
	public void saveFile(File f) throws IOException { 
		PrintStream ps = new PrintStream(new FileOutputStream(f));
		ps.print("ID");
		for(Column c : cols) { ps.print(String.format("\t%s", c.id)); }
		ps.println();
		for(int i = 0; i < samples.size(); i++) { 
			ps.print(samples.get(i));
			for(int j = 0; j < cols.size(); j++) { 
				Float ff = cols.get(j).values.get(i);
				if(ff == null) { 
					ps.print("\tnull");
				} else { 
					ps.print(String.format("\t%.2f", ff));
				}
			}
			ps.println();
			
			if(i > 0) { 
				if(i % 100 == 0) { System.out.print("."); System.out.flush(); }
			}
		}
		System.out.println();
		ps.close();
	}
	
	public TableParser(File f, int samples) throws IOException {
		this();
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = br.readLine();
		String[] array = line.split("\t");
		for(int i = 1; i < array.length; i++) { 
			String id = array[i];
			cols.add(new Column(id));
			ids.put(id, cols.size()-1);
		}
		
		parseFile(br, samples);
		br.close();
	}
	
	public TableParser(DataInputStream dis) throws IOException {
		this();
		inputBinary(dis);
	}
	
	public void loadBinary(File f) throws IOException { 
		DataInputStream dis = new DataInputStream(new FileInputStream(f));
		inputBinary(dis);
		dis.close();
	}
	
	public void inputBinary(DataInputStream dis) throws IOException { 
		int ns = dis.readInt();
		for(int i = 0; i < ns; i++) { 
			samples.add(dis.readUTF());
		}

		int s = dis.readInt();
		for(int i = 0; i < s; i++) { 
			Column c = new Column(dis);
			cols.add(c);
			ids.put(c.id, cols.size()-1);
			if(i > 0) { 
				//if(i % 100 == 0) { System.out.print("."); System.out.flush(); }
				//if(i % 1000 == 0) { System.out.print("(" + i + ")"); System.out.flush(); }
			}
		}
		System.out.println();
	}
	
	public void saveBinary(File f) throws IOException { 
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
		outputBinary(dos);
		dos.close();
	}
	
	public void outputBinary(DataOutputStream dos) throws IOException {
		dos.writeInt(samples.size());
		for(String s : samples) { 
			dos.writeUTF(s);
		}
		dos.writeInt(cols.size());
		for(Column c : cols) { 
			c.outputBinary(dos);
		}
	}

	public MicroarrayProbe expression(String id) {
		if(!ids.containsKey(id)) { return null; }
		int idx = ids.get(id);
		Double[] v = new Double[samples.size()];
		String[] s = samples.toArray(new String[0]);
		for(int i = 0; i < samples.size(); i++) {
			v[i] = null;
			Float f = cols.get(idx).values.get(i);
			if(f != null) { 
				v[i] = f.doubleValue();
			}
		}
		
		MicroarrayProbe p = new MicroarrayProbe(id, v, s);
		return p;
	}

	public Collection<Float> values(String id) { 
		int idx = ids.get(id);
		ArrayList<Float> floats = new ArrayList<Float>();
		Column col = cols.get(idx);
		for(int i = 0; i < col.values.size(); i++) { 
			if(col.values.get(i) != null) { 
				floats.add(col.values.get(i));
			}
		}
		return floats;
	}
	
	public Collection<Float[]> valueSets(String... is) { 
		Column[] idxs = new Column[is.length];
		for(int i = 0; i < idxs.length; i++) { 
			idxs[i] = cols.get(ids.get(is[i]));
		}
		
		ArrayList<Float[]> floats = new ArrayList<Float[]>();
		for(int i = 0; i < samples.size(); i++) {
			Float[] fs = new Float[idxs.length];
			boolean hasNull = false;
			for(int j = 0; j < idxs.length; j++) { 
				fs[j] = idxs[j].values.get(i);
				hasNull = hasNull || fs[j] == null;
			}
			
			if(!hasNull) { 
				floats.add(fs);
			}
		}
		
		return floats;
	}
	
	private void parseFile(BufferedReader br) throws IOException { 
		parseFile(br, -1);
	}
	
	private void parseFile(BufferedReader br, int size) throws IOException { 
		String line = null;
		int i = 0;
		while((size == -1 || i < size) && (line = br.readLine()) != null) { 
			TableLine tline = new TableLine(line);
			addLine(tline);
			i += 1;
			if(i % 100 == 0) { System.out.print("."); System.out.flush(); }
			if(i % 1000 == 0) { System.out.print(String.format("(%d)", i)); System.out.flush(); }
		}
		System.out.println();
	}
	
	private void addLine(TableLine l) { 
		for(int i = 0; i < l.values.length; i++) { 
			cols.get(i).values.add(l.values[i]);
		}
		samples.add(l.id);
		sampleIndices.put(l.id, samples.size()-1);
	}

	public int getNumSamples() {
		return samples.size();
	}
	
	public int getNumProbes() { 
		return cols.size();
	}
}

class Column { 
	
	public class Entry implements Comparable<Entry> {
		
		private int sampleIdx;
		
		public Entry(int si) { sampleIdx = si; }
		
		public String id() { return id; }
		
		public int compareTo(Entry e) {
			double v1 = value(), v2 = e.value();
			if(v1 < v2) { return -1; }
			if(v1 > v2) { return 1; }
			return 0;
		}
		
		public Double value() { 
			Float f = values.get(sampleIdx);
			if(f != null) { 
				return (Double)f.doubleValue();
			} else { 
				return null;
			}
		}
		
		public int hashCode() { 
			int code = 17;
			code += id.hashCode(); code *= 37;
			code += sampleIdx; code *= 37;
			return code;
		}
		
		public boolean equals(Object o) { 
			if(!(o instanceof Entry)) { return false; }
			Entry e = (Entry)o;
			return id.equals(e.id()) && sampleIdx == e.sampleIdx;
		}
	}
	
	public Entry entry(int sample) { return new Entry(sample); }
	
	public String id; 
	public Vector<Float> values; 
	
	public Column(String id) { 
		this.id = id;
		values = new Vector<Float>();
	}
	
	public Column(DataInputStream dis) throws IOException { 
		id = dis.readUTF();
		values = new Vector<Float>();
		int s = dis.readInt();

		int i = 0;
		while(i < s) { 
			int c = dis.readInt();
			
			for(int j = 0; j < c && i < s; j++, i++) { 
				values.add(dis.readFloat());
			}
			
			if(i < s) { 
				values.add(null);
				i++;
			}
		}
	}
	
	public void addValue(Float f) { 
		values.add(f);
	}
	
	public void outputBinary(DataOutputStream dos) throws IOException { 
		dos.writeUTF(id);
		dos.writeInt(values.size());
		
		int i = 0, j = 0;
		while(i < values.size()) { 
			for(j = i; j < values.size() && values.get(j) != null; j++) 
				{} 
			int count = j - i - 1;
			dos.writeInt(count);
			for(j = 1; j <= count && i + j < values.size(); j++) { 
				dos.writeFloat(values.get(i + j));
			}
			i = j + 1;
		}
	}
}

class TableLine { 
	
	public String id;
	public Float[] values;
	
	public TableLine(String l) { 
		String[] array = l.split("\t");
		id = array[0];
		values = new Float[array.length-1];
		for(int i = 0; i < values.length; i++) { 
			try { 
				values[i] = Float.parseFloat(array[i+1]);
			} catch(NumberFormatException e) { 
				values[i] = null;
			}
		}
	}
}
