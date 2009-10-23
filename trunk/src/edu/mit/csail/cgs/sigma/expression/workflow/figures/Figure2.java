/*
 * Author: tdanford
 * Date: Feb 13, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.figures;

import java.util.regex.*;
import java.util.*;
import java.awt.*;
import java.io.*;

import edu.mit.csail.cgs.sigma.expression.segmentation.Segment;
import edu.mit.csail.cgs.sigma.expression.transcription.*;
import edu.mit.csail.cgs.sigma.expression.workflow.*;
import edu.mit.csail.cgs.sigma.expression.workflow.models.InputSegmentation;
import edu.mit.csail.cgs.utils.ArrayUtils;
import edu.mit.csail.cgs.utils.models.Model;
import edu.mit.csail.cgs.viz.colors.Coloring;
import edu.mit.csail.cgs.viz.paintable.AbstractPaintable;
import edu.mit.csail.cgs.viz.paintable.PaintableFrame;
import edu.mit.csail.cgs.viz.paintable.PaintableScale;

public class Figure2 extends AbstractPaintable {
	
	public static void main(String[] args) { 
		try {
			boolean noncoding = args.length > 0 && args[0].equals("noncoding");
			LineSet set = noncoding ? loadNoncodingLines("txns288c", "matalpha") : loadCodingLines();
			Figure2 fig2 = new Figure2(set);

			if(!noncoding) { 
				fig2.addBarchart(new String[] { "s288c" });
				fig2.addBarchart(new String[] { "sigma" });
				fig2.addBarchart(new String[] { "both" });
				fig2.addBarchart(new String[] { "both", "E" });
				fig2.addLinegraph();
			} else {
				fig2.addBarchart(new String[] { "E", });				
				fig2.addBarchart(new String[] { "E", "A" });				
				fig2.addLinegraph();
				fig2.addBarchart(new String[] { "E", "C" });
				fig2.addLinegraph();
				fig2.addBarchart(new String[] { "E", "I" });
				fig2.addLinegraph();
			}

			PaintableFrame pf = new PaintableFrame("Figure 2", fig2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static LineSet loadCodingLines() throws IOException { 
		WorkflowProperties props = new WorkflowProperties();
		File dir = props.getDirectory();
		File lineFile = new File(dir, "fig2-coding.txt");
		return new LineSet(lineFile);
	}
	
	public static LineSet loadNoncodingLines(String key, String expt) throws IOException { 
		WorkflowProperties props = new WorkflowProperties();
		File dir = props.getDirectory();
		File lineFile = new File(dir, String.format("%s-%s-fig2-noncoding.txt", key, expt));
		return new LineSet(lineFile);
	}
	
	private LineSet total;
	private ArrayList<BarchartSet> charts;
	private ArrayList<Linegraph> graphs;
	private Set<Integer> drawLinegraphs;
	
	public Figure2(LineSet t) { 
		total = t;
		charts = new ArrayList<BarchartSet>();
		graphs = new ArrayList<Linegraph>();
		
		String[] opts = total.options(0).toArray(new String[0]);
		BarchartSet chart = new BarchartSet(total, 0, opts);
		charts.add(chart);
		graphs.add(new Linegraph(chart));
		
		drawLinegraphs = new TreeSet<Integer>();
	}
	
	public void addBarchart(String[] fs) { 
		Integer[] cs = new Integer[fs.length];
		for(int i = 0; i < fs.length; i++) { 
			cs[i] = i;
		}
		
		LineSet set = total.subset(cs, fs);
		int next = fs.length;
		String[] opts = set.options(next).toArray(new String[0]);
		BarchartSet chart = new BarchartSet(set, next, opts);
		charts.add(chart);
		graphs.add(new Linegraph(chart));
		//graphs.add(new GradientGraph(chart));
	}

	public void addLinegraph() {
		drawLinegraphs.add(charts.size()-1);
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2-x1, h = y2-y1;
		g.setColor(Color.white);
		g.fillRect(x1, y1, w, h);
		
		int trackHeight = (int)Math.floor((double)h / (double)Math.max(1, 2*charts.size()));
		int spacing = Math.max(2, trackHeight/10);
		
		for(int i = 0; i < charts.size(); i++) { 
			int ty = y1 + i * 2 * trackHeight;
			int cx = x1 + spacing;
			int cy = ty + spacing;
			int cw = w - spacing*2;
			int ch = trackHeight - spacing*2;
			
			int gx = cx;
			int gy = cy + trackHeight;
			
			charts.get(i).paintItem(g, cx, cy, cx+cw, cy+ch);
			if(drawLinegraphs.contains(i)) { 
				graphs.get(i).paintItem(g, gx, gy, gx+cw, gy+ch);
			}
		}
	}
}

class Linegraph extends AbstractPaintable {

	public BarchartSet chart;
	
	public Linegraph(BarchartSet c) { 
		chart = c;
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2-x1, h = y2-y1;
		
		g.setColor(Color.black);
		g.drawRect(x1, y1, w, h);

		int N = 0;
		Double min = 0.0, max = 0.0;

		for(LineSet lines : chart.sets) {
			min = Math.min(lines.getMinValue(), min);
			max = Math.max(lines.getMaxValue(), max);
			N += lines.size();
		}

		if(min == null) { min = -1.0; }
		if(max == null) { max = 1.0; }

		double amin = Math.min(min, -Math.abs(max));
		double amax = Math.max(max, Math.abs(min));
		
		min = amin; 
		max = amax;
		
		PaintableScale scale = new PaintableScale(min, max);
		if(min < 0.0 && max > 0.0) { 
			g.setColor(Color.gray);
			double f = scale.fractionalOffset(0.0);
			int zy = y2 - (int)Math.round((double)h * f);
			g.drawLine(x1+1, zy, x2, zy);
		}
		
		int base = 0;
		int px = -1, py = -1;
		int sx = x1;
		
		for(int k = 0; k < chart.sets.length; k++) { 
			LineSet lines = chart.sets[k];
			Double[] values = lines.values();
			double sf = (double)values.length / (double)Math.max(N, 1);
			int sw = (int)Math.floor(sf * (double)w);
			
			if(k % 2 == 0) { 
				g.setColor(Coloring.clearer(Coloring.clearer(Color.cyan)));
			} else { 
				g.setColor(Coloring.clearer(Coloring.clearer(Color.white)));
			}
			g.fillRect(sx, y1, sw, h);
			
			for(int i = 0; i < values.length; i++, base++) { 
				if(values[i] != null) { 
					double f = scale.fractionalOffset(values[i]);
					double xf = (double)(i + 1) / (double)values.length;
					int vy = y2 - (int)Math.round((double)h * f);
					int vx = sx + (int)Math.round((double)sw * xf);

					g.setColor(Color.red);
					//g.drawOval(vx-2, vy-2, 4, 4);
					if(px != -1 && py != -1) { 
						g.drawLine(px, py, vx, vy);
					}
					px = vx; py = vy;
				}
			}

			sx += sw;
			px = py = -1;
		}
	} 
	
}

class GradientGraph extends Linegraph {

	public BarchartSet chart;
	
	public GradientGraph(BarchartSet c) {
		super(c);
		chart = c;
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2-x1, h = y2-y1;
		
		g.setColor(Color.black);
		g.drawRect(x1, y1, w, h);

		int N = 0;
		Double min = 0.0, max = 0.0;

		for(LineSet lines : chart.sets) {
			min = Math.min(lines.getMinValue(), min);
			max = Math.max(lines.getMaxValue(), max);
			N += lines.size();
		}

		if(min == null) { min = -1.0; }
		if(max == null) { max = 1.0; }
		
		double amin = Math.min(min, -Math.abs(max));
		double amax = Math.max(max, Math.abs(min));
		
		min = amin; 
		max = amax;
		
		PaintableScale scale = new PaintableScale(amin, amax);
		if(min < 0.0 && max > 0.0) { 
			g.setColor(Color.gray);
			double f = scale.fractionalOffset(0.0);
			int zy = y2 - (int)Math.round((double)h * f);
			//g.drawLine(x1+1, zy, x2, zy);
		}
		
		int base = 0;
		int px = -1, py = -1;
		int sx = x1;
		
		for(int k = 0; k < chart.sets.length; k++) { 
			LineSet lines = chart.sets[k];
			Double[] values = lines.values();
			double sf = (double)values.length / (double)Math.max(N, 1);
			int sw = (int)Math.floor(sf * (double)w);
			
			if(k % 2 == 0) { 
				g.setColor(Coloring.clearer(Coloring.clearer(Color.cyan)));
			} else { 
				g.setColor(Coloring.clearer(Coloring.clearer(Color.white)));
			}
			//g.fillRect(sx, y1, sw, h);
			
			for(int i = 0; i < values.length; i++, base++) { 
				if(values[i] != null) { 
					double f = scale.fractionalOffset(values[i]);
					double xf = (double)(i + 1) / (double)values.length;
					int vy = y2 - (int)Math.round((double)h * f);
					int vx = sx + (int)Math.round((double)sw * xf);

					g.setColor(createColor(f));
					g.drawLine(vx, y1, vx, y2);
					px = vx; py = vy;
				}
			}

			sx += sw;
			px = py = -1;
		}
	} 
	
	public Color createColor(double value) { 
		if(value < 0.5) { 
			double f = ((1.0 - value) - 0.5) / 0.5;
			double max = 254.0;
			int c = (int)Math.floor(max * f);
			return new Color(0, 1+c, 0);			
		} else if (value > 0.5) { 
			double f = (value - 0.5) / 0.5;
			double max = 254.0;
			int c = (int)Math.floor(max * f);
			return new Color(1+c, 0, 0);
		} else { 
			return Color.black;
		}
	}
	
}

class BarchartSet extends AbstractPaintable {
	
	public static Color[] defaultColors = new Color[] { 
		Color.pink, Color.blue, Color.lightGray, Color.blue, Color.pink,
	};
	
	public static Map<String,Integer> keyOrder;
	
	static { 
		keyOrder = new TreeMap<String,Integer>();
		keyOrder.put("E", 0);
		keyOrder.put("N", 1);
		keyOrder.put("s288c", 0);
		keyOrder.put("both", 1);
		keyOrder.put("sigma", 2);
		keyOrder.put("s288c-unique", 0);
		keyOrder.put("s288c-diff", 1);
		keyOrder.put("same", 2);
		keyOrder.put("sigma-diff", 3);
		keyOrder.put("sigma-unique", 4);
	}
	
	private class KeyOrderComparator implements Comparator<String> { 
		public int compare(String s1, String s2) { 
			Integer v1 = keyOrder.get(s1);
			Integer v2 = keyOrder.get(s2);
			if(v1 == null || v2 == null) { 
				return s1.compareTo(s2);
			} else { 
				return v1.compareTo(v2);
			}
		}
	}
	
	public String tag;
	public String[] keys;
	public LineSet[] sets;
	public Integer[] sizes; 
	public Integer totalSize;
	public Color[] colors;
	
	public BarchartSet(LineSet total, Integer col, String[] opts) {
		tag = total.getName();
		keys = opts.clone();
		Arrays.sort(keys, new KeyOrderComparator());
		sets = total.subsets(col, keys);
		sizes = new Integer[sets.length];
		totalSize = 0;
		for(int i = 0; i < sets.length; i++) { 
			int s = sets[i].size();
			totalSize += s;
			sizes[i] = s;
		}
		
		colors = new Color[sets.length];
		for(int i = 0; i < colors.length; i++) { 
			colors[i] = defaultColors[i % defaultColors.length];
		}
	}
	
	public int size() { return sets.length; }
	public LineSet getLineSet(int i) { return sets[i]; }
	
	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		int w = x2-x1, h = y2-y1;

		Graphics2D g2 = (Graphics2D)g;
		FontMetrics fm = g2.getFontMetrics();
		int textHeight = fm.getAscent() + fm.getDescent();
		int tagWidth = fm.charsWidth(tag.toCharArray(), 0, tag.length());

		int offset = 0;
		for(int i = 0; i < sets.length; i++) { 
			int size = sets[i].size();
			double f = (double)size / (double)Math.max(totalSize, 1);
			int boxWidth = (int)Math.round(f * (double)w);
			
			g.setColor(colors[i]);
			g.fillRect(x1 + offset, y1, boxWidth, h);
			
			offset += boxWidth;
		}
		
		g.setColor(Color.black);
		g.drawRect(x1, y1, w, h);

		offset = 0;
		
		for(int i = 0; i < sets.length; i++) { 
			int size = sets[i].size();
			double f = (double)size / (double)Math.max(totalSize, 1);
			int boxWidth = (int)Math.round(f * (double)w);
			
			g.setColor(Color.black);
			//g.drawLine(x1 + offset, y1, x1 + offset, y2);

			g.setColor(Color.black);
			String numstr = String.format("%s (%d)", 
					sets[i].getLastName(), size);
			int numwidth = fm.charsWidth(numstr.toCharArray(), 0, numstr.length());

			int numx = x1 + offset + 2; 
			int numy = y1 + textHeight + 2;
			
			if(numwidth > boxWidth) { 
				numx = x1 + offset + boxWidth - numwidth;
				numy = y2 - 2;
			}
			
			g2.drawString(numstr, numx, numy);

			offset += boxWidth;
		}
		
		g.setColor(Color.black);
		g2.drawString(tag, x1 + 2, y1 - 2);
	}
}

class LineSet { 
	
	private String name, lastName;
	private ClassedLine[] lines;
	private Map<Integer,String> values;
	
	public LineSet() {
		name = "";
		lastName = "";
		lines = new ClassedLine[] {};
		values = new TreeMap<Integer,String>();
	}	
	
	public double getMinValue() {
		double m = 00.0;
		for(ClassedLine line : lines) { 
			m = Math.min(m, line.value != null ? line.value : 0.0);
		}
		return m;
	}

	public double getMaxValue() {
		double m = 00.0;
		for(ClassedLine line : lines) { 
			m = Math.max(m, line.value != null ? line.value : 0.0);
		}
		return m;
	}

	public LineSet(File f) throws IOException { 
		this();
		
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		
		ArrayList<ClassedLine> clines = new ArrayList<ClassedLine>();
		
		while((line = br.readLine()) != null) { 
			line = line.trim();
			if(line.length() > 0) { 
				ClassedLine cline = new ClassedLine(line);
				clines.add(cline);
			}
		}
		br.close();
		
		lines = clines.toArray(new ClassedLine[0]);
		Arrays.sort(lines);
	}
	
	public LineSet(Iterator<ClassedLine> ls) {
		this();
		
		ArrayList<ClassedLine> clines = new ArrayList<ClassedLine>();		
		
		while(ls.hasNext()) { 
			clines.add(ls.next());
		}
		
		lines = clines.toArray(new ClassedLine[0]);
		Arrays.sort(lines);
	}
	
	public LineSet(LineSet s, Integer col, String value) {
		lastName = value;
		
		name = s.name.length() > 0 ? s.name + "," : s.name;
		name += String.format("%d:%s", col, value);
		
		ArrayList<ClassedLine> clines = new ArrayList<ClassedLine>();		
		values = new TreeMap<Integer,String>(s.values);
		if(values.containsKey(col)) { throw new IllegalArgumentException(String.format("%d in %s", col, values.keySet().toString())); }
		values.put(col, value);
		for(ClassedLine line : s.lines) { 
			if(line.classes[col].equals(value)) { 
				clines.add(line);
			}
		}
		lines = clines.toArray(new ClassedLine[0]);
		Arrays.sort(lines);
	}
	
	public Double[] values() { 
		Double[] v = new Double[lines.length];
		for(int i = 0; i < v.length; i++) { 
			v[i] = lines[i].value;
		}
		return v;
	}
	
	public String getName() { return name; }
	public String getLastName() { return lastName; }
	
	public int size() { return lines.length; }
	
	public String value(int col) { return values.get(col); }
	
	public Set<String> options(int col) { 
		TreeSet<String> keys = new TreeSet<String>();
		for(ClassedLine line : lines) { 
			keys.add(line.classes[col]);
		}
		return keys;
	}
	
	public int numOptions(int col) { 
		return options(col).size();
	}
	
	public LineSet[] subsets(Integer col) {
		return subsets(col, options(col).toArray(new String[0]));
	}
	
	public LineSet subset(Integer[] cols, String[] opts) { 
		if(cols.length == 0) { return this; }
		LineSet ss = new LineSet(this, cols[0], opts[0]);
		return ss.subset(ArrayUtils.tail(cols), ArrayUtils.tail(opts));
	}
	
	public LineSet[] subsets(Integer col, String[] opts) { 
		ArrayList<LineSet> sets = new ArrayList<LineSet>();
		for(String value : opts) { 
			sets.add(new LineSet(this, col, value));
		}
		return sets.toArray(new LineSet[0]);
	}
}

class ClassedLine implements Comparable<ClassedLine> {
	
	public static Pattern valpatt = Pattern.compile("\\(([\\-\\.0-9]+)\\)");
	
	public String gene;
	public Double value;
	public String[] classes;
	
	public ClassedLine(String l) { 
		String[] a = l.split("\\s+");
		gene = a[0];
		String valstr = a[a.length-1];
		Matcher m = valpatt.matcher(valstr);
		if(m.matches()) { valstr = m.group(1); }
		value = Double.parseDouble(valstr);
		classes = new String[a.length-2];
		for(int i = 0; i < classes.length; i++) { 
			classes[i] = a[i+1];
		}
	}
	
	public int compareTo(ClassedLine cl) { 
		if(value > cl.value) { return -1; }
		if(value < cl.value) { return 1; }
		return gene.compareTo(cl.gene);
	}
	
	public int hashCode() { 
		return gene.hashCode();
	}
	
	public boolean equals(Object o) { 
		if(!(o instanceof ClassedLine)) { return false; }
		ClassedLine l = (ClassedLine)o;
		return l.gene.equals(gene);
	}
}
