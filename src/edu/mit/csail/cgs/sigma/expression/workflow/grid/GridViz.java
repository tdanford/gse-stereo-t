/*
 * Author: tdanford
 * Date: May 20, 2009
 */
package edu.mit.csail.cgs.sigma.expression.workflow.grid;

import java.awt.*;
import java.util.*;
import java.io.*;

import edu.mit.csail.cgs.viz.paintable.*;
import edu.mit.csail.cgs.sigma.expression.workflow.WorkflowProperties;

public class GridViz extends AbstractPaintable {

    public static void main(String[] args) { 
        WorkflowProperties props = new WorkflowProperties();
        double pvalue = 0.01;

        try {
            GridLoader loader = new GridLoader(props, "txns288c");
            Grid grid = loader.loadGrid();
            GridViz viz = new GridViz(grid, pvalue);

            PaintableFrame pf = new PaintableFrame("grid", viz);
            
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
	
	private Grid grid;

	private String[] rowLabels, colLabels;
	private Boolean[][] array;
	private double pvalue;
	
	public GridViz(Grid g, double pv) {
		pvalue = pv;
		grid = g;
		rebuildArray();
	}
	
	public void update() { 
		rebuildArray();
		dispatchChangedEvent();
	}
	
	private void rebuildArray() { 
		array = grid.enrichedGrid(pvalue);
		
		rowLabels = new String[grid.numRows()];
		colLabels = new String[grid.numCols()];
		
		for(int i = 0; i < rowLabels.length; i++) { 
			rowLabels[i] = grid.rowName(i);
		}
		for(int i = 0; i < colLabels.length; i++) { 
			colLabels[i] = grid.colName(i);
		}		
	}

	public void paintItem(Graphics g, int x1, int y1, int x2, int y2) {
		Graphics2D g2 = (Graphics2D)g;
		int w = x2-x1, h = y2-y1;
		
		// Draw the background. 
		g.setColor(Color.white);
		g.fillRect(x1, y1, w, h);
		
		// Print the labels along the top and sides. 
		Font oldFont = g2.getFont();
		FontMetrics fm = g2.getFontMetrics();
		int fontHeight = fm.getAscent() + fm.getDescent() + 1;
		int maxRowLabelWidth = 1;
		int maxColLabelWidth = 1;
	
		// First, we measure the size of all the row and column labels.
		for(int i = 0; i < rowLabels.length; i++) { 
			int lblw = fm.charsWidth(rowLabels[i].toCharArray(), 0, rowLabels[i].length());
			maxRowLabelWidth = Math.max(maxRowLabelWidth, lblw+1);
		}
		for(int i = 0; i < colLabels.length; i++) { 
			int lblw = fm.charsWidth(colLabels[i].toCharArray(), 0, colLabels[i].length());
			maxColLabelWidth = Math.max(maxColLabelWidth, lblw+1);
		}
		
		// Next, we print them -- rows on the left, columns on the top. 
		for(int i = 0; i < rowLabels.length; i++) {
			int lblw = fm.charsWidth(rowLabels[i].toCharArray(), 0, rowLabels[i].length());
			int lx = x1 + maxRowLabelWidth - lblw - 1;
			int ly = y1 + fontHeight * (i+1);
			g.drawString(rowLabels[i], lx, ly);
		}
		
		g2.translate(x1, y1);
		g2.rotate(-Math.PI/2.0);
		for(int i = 0; i < colLabels.length; i++) { 
			int lblw = fm.charsWidth(colLabels[i].toCharArray(), 0, colLabels[i].length());
			int lx = -maxColLabelWidth + 1;
			int ly = (i+1) * fontHeight;
			g.drawString(colLabels[i], lx, ly);
		}
		g2.rotate(Math.PI/2.0);
		g2.translate(-x1, -y1);
		
		// -- Central Grid Drawing Code ------------------------------
		
		// build dimensions.
		int arrayWidth = w - maxRowLabelWidth;
		int arrayHeight = h - maxColLabelWidth;
		int colWidth = (int)Math.ceil((double)arrayWidth / (double)Math.max(colLabels.length, 1));
		int rowHeight = (int)Math.ceil((double)arrayHeight / (double)Math.max(rowLabels.length, 1));
		
		// make sure all the grid elements are at least 4x4 pixels, and are square.
		int dim = Math.max(4, Math.min(colWidth, rowHeight));
		rowHeight = colWidth = dim;

		// red circles indicate enriched elements.
		for(int i = 0; i < array.length; i++) {
			int ax = maxRowLabelWidth + i * colWidth;
            
            StringBuilder rowString = new StringBuilder();
            int trueCount = 0;
			
			for(int j = 0; j < array[i].length; j++) {
				int ay = maxColLabelWidth + i * rowHeight;
				
				if(array[i][j]) { 
					g2.setColor(Color.red);
					g2.fillOval(ax, ay, colWidth, rowHeight);
                    rowString.append("*");
                    trueCount += 1;
				} else { 
                    rowString.append(".");
                }
			}

            rowString.append(" : " + rowLabels[i]);
            if(trueCount > 0) {
                System.out.println(rowString.toString());
            }
		}
		
		// -- Cleanup ------------------------------------------------
		g2.setFont(oldFont);
	}
}
