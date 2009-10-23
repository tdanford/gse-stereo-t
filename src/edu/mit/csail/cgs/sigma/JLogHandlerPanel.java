package edu.mit.csail.cgs.sigma;

import java.util.*;
import java.util.logging.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class JLogHandlerPanel extends JPanel {
	
	public static class StandaloneFrame extends JFrame {
		
		private JLogHandlerPanel handlerPanel;
		
		public StandaloneFrame() { 
			super("Logger");
			handlerPanel = new JLogHandlerPanel();
			
			Container c = (Container)getContentPane();
			c.setLayout(new BorderLayout());
			c.add(handlerPanel, BorderLayout.CENTER);
			
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		}
		
		public JLogHandlerPanel getHandlerPanel() { return handlerPanel; }
		
		public void showFrame() { 
			setVisible(true);
			pack();
		}
	}
	
	private long handlerID;
	private Vector<InternalHandler> handlers;
	private JTable recTable;
	private LogRecordTableModel recModel;

	public JLogHandlerPanel() { 
		super();
		handlerID = (long)0;
		handlers = new Vector<InternalHandler>();
		recModel = new LogRecordTableModel();
		recTable = new JTable(recModel);
		
		setLayout(new BorderLayout());
		add(new JScrollPane(recTable), BorderLayout.CENTER);
	}
	
	public void clearClosedHandlers() {
		synchronized(handlers) { 
			Iterator<InternalHandler> hitr = handlers.iterator();
			while(hitr.hasNext()) { 
				InternalHandler handler = hitr.next();
				if(handler.isClosed) { 
					hitr.remove();
				}
			}
		}
	}
	
	public Handler createHandler() {
		InternalHandler handler = null;
		synchronized(handlers) { 
			handler = new InternalHandler(handlerID++);
			handlers.add(handler);
		}
		return handler;
	}
	
	private static class LogRecordTableModel implements TableModel {
		
		private LinkedList<TableModelListener> listeners;
		private Vector<LogRecord> recs;
		
		public LogRecordTableModel() { 
			listeners = new LinkedList<TableModelListener>();
			recs = new Vector<LogRecord>();
		}
		
		public void addRecord(LogRecord rec) {
			int rnum = -1;
			synchronized(recs) { 
				recs.add(rec);
				rnum = recs.size()-1;
			}
			
			fireEvent(new TableModelEvent(this, rnum, rnum, 
					TableModelEvent.ALL_COLUMNS, 
					TableModelEvent.INSERT));
		}
		
		private void fireEvent(TableModelEvent evt) { 
			for(TableModelListener l : listeners) { 
				l.tableChanged(evt);
			}			
		}

		public void addTableModelListener(TableModelListener l) {
			listeners.add(l);
		}

		public Class<?> getColumnClass(int c) {
			switch(c) { 
			case 0: 
			case 1:
			case 2:
				return String.class;
			default:
				return null;
			}
		}

		public int getColumnCount() {
			return 3;
		}

		public String getColumnName(int c) {
			switch(c) { 
			case 0: return "Logger";
			case 1: return "Level";
			case 2: return "Message";
			default: 
				return null;
			}
		}

		public int getRowCount() {
			return recs.size();
		}

		public Object getValueAt(int r, int c) {
			LogRecord rec = recs.get(r);
			switch(c) { 
			case 0: return rec.getLoggerName();
			case 1: return rec.getLevel().toString();
			case 2: return rec.getMessage();
			default:
				return null;
			}
		}

		public boolean isCellEditable(int arg0, int arg1) {
			return false;
		}

		public void removeTableModelListener(TableModelListener l) {
			listeners.remove(l);
		}

		public void setValueAt(Object arg0, int arg1, int arg2) {
			throw new UnsupportedOperationException();
		} 
		
	}
	
	private class InternalHandler extends Handler {
		
		private long id;
		public boolean isClosed;
		
		public InternalHandler(long _id) {
			id = _id;
			isClosed = false;
		}
		
		public long getID() { return id; }
		
		public int hashCode() { 
			int code = 17;
			code += (int)id; code *= 37;
			code += (int)(id >> 32); code *= 37;
			return code;
		}
		
		public boolean equals(Object o) { 
			if(!(o instanceof InternalHandler)) { return false; }
			InternalHandler ih = (InternalHandler)o;
			return ih.id == id;
		}

		@Override
		public void close() throws SecurityException {
			isClosed = true;
		}

		@Override
		public void flush() {
		}

		@Override
		public void publish(LogRecord rec) {
			if(!(isClosed)) { 
				recModel.addRecord(rec);
			}
		} 
	}
}
