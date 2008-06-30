/*      PANDA -- a simple transaction monitor

Copyright (C) 1998-1999 Ogochan.
              2000-2003 Ogochan & JMA (Japan Medical Association).
              2002-2006 OZAWA Sakuro.

This module is part of PANDA.

		PANDA is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY.  No author or distributor accepts responsibility
to anyone for the consequences of using it or for whether it serves
any particular purpose or works at all, unless he says so in writing.
Refer to the GNU General Public License for full details.

		Everyone is granted permission to copy, modify and redistribute
PANDA, but only under the conditions described in the GNU General
Public License.  A copy of this license is supposed to have been given
to you along with PANDA so you can know your rights and
responsibilities.  It should be in a file named COPYING.  Among other
things, the copyright notice and this notice must be preserved on all
copies.
*/

package org.montsuqi.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.SystemColor;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.Date;

import org.montsuqi.util.SystemEnvironment;
import org.montsuqi.util.Logger;

/** <p>A class that simulates Gtk+'s Calendar widget.</p>
 * 
 * <p>When the date selection is changed, ChangeEvent will be fired.</p>
 */
public class Calendar extends JComponent {

	Date date;
	private JComponent caption;
	private JButton[][] dateCells;
	private CalendarSpinner monthSpinner;
	private CalendarSpinner yearSpinner;
	private Logger logger;
	Date[][] cellDates;
	java.util.Calendar cal;

	public Calendar() {
		super();
		setLayout(new BorderLayout());
		caption = new JPanel();
		add(caption, BorderLayout.NORTH);
		caption.setLayout(new GridLayout(1, 2));
		
		monthSpinner = new CalendarSpinner(java.util.Calendar.MONTH, Messages.getString("Calendar.month_format"));
		yearSpinner = new CalendarSpinner(java.util.Calendar.YEAR, Messages.getString("Calendar.year_format")); //$NON-NLS-1$
		caption.add(monthSpinner); //$NON-NLS-1$
		caption.add(yearSpinner); //$NON-NLS-1$

		dateCells = new JButton[7][7];
		cellDates = new Date[7][7];
		JComponent dateCellPanel = new JPanel();
		dateCellPanel.setLayout(new GridLayout(7, 7));
		add(dateCellPanel, BorderLayout.CENTER);

		cal = java.util.Calendar.getInstance();
		this.date = cal.getTime();

		SimpleDateFormat df = new SimpleDateFormat();
		DateFormatSymbols symbols = df.getDateFormatSymbols();
		String[] dayOfWeekNames = symbols.getShortWeekdays();
		for (int col = 0; col < 7; col++) {
			JLabel dayOfWeek = new JLabel(dayOfWeekNames[col + 1]);
			dayOfWeek.setHorizontalAlignment(SwingConstants.CENTER);
			dateCellPanel.add(dayOfWeek);
		}
		Insets margin = new Insets(0, 0, 0, 0);
		if (SystemEnvironment.isMacOSX() && SystemEnvironment.isJavaVersionMatch("1.6")) {
		    margin.set(0, -20, 0, -20);
		}
		for (int row = 1; row < 7; row++) {
			for (int col = 0; col < 7; col++) {
				final JButton cell = new JButton();
				cell.setHorizontalAlignment(SwingConstants.RIGHT);

				cell.setMargin(margin);
				final int finalRow = row;
				final int finalCol = col;
				cell.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						Calendar.this.date = cellDates[finalRow][finalCol];
						fireChangeEvent(new ChangeEvent(Calendar.this));
					}
				});
				dateCells[row][col] = cell;
				dateCellPanel.add(cell);
			}
		}
		setCells();
	}
	
	protected void fireChangeEvent(ChangeEvent e) {
		ChangeListener[] listeners = (ChangeListener[])listenerList.getListeners(ChangeListener.class);
		for (int i = 0, n = listeners.length; i < n; i++) {
			ChangeListener l = listeners[i];
			l.stateChanged(e);
		}
		setCells();
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;		
		setCells();
		setSpinners();
	}
	
	void setCells() {
		cal.setTime(date);
		int month = cal.get(java.util.Calendar.MONTH);
		int day = cal.get(java.util.Calendar.DATE);
		for (int row = 1; row < 7; row++) {
			for (int col = 0; col < 7; col++) {
				Date d = computeCellDate(row, col);
				cellDates[row][col] = d;
				cal.setTime(d);
				JButton cell = dateCells[row][col];
				cell.setText(String.valueOf(cal.get(java.util.Calendar.DATE)));
				cell.setEnabled(month == cal.get(java.util.Calendar.MONTH));
				if (month == cal.get(java.util.Calendar.MONTH)) {
					if ( cal.get(java.util.Calendar.DATE) == day) {
						cell.setBackground((Color)SystemColor.controlLtHighlight);
					} else {
						cell.setBackground((Color)SystemColor.controlHighlight);
					}
				} else {
					cell.setBackground((Color)SystemColor.control);
				}
			}
		}
		//monthLabel.setText(df.format(date));
	}
	
	void setSpinners() {
		try {
			monthSpinner.setValue(date);
			yearSpinner.setValue(date);
		} catch (IllegalArgumentException e) {
			logger.warn(e);
		}
	}

	private Date computeCellDate(int row, int col) {
		// compute the first day of the month 
		cal.setTime(date);
		cal.set(java.util.Calendar.DATE, 1);

		// compute the date of top left cell.
		cal.add(java.util.Calendar.DATE, -cal.get(java.util.Calendar.DAY_OF_WEEK));
		cal.add(java.util.Calendar.DATE, cal.getFirstDayOfWeek());

		// advance to the desired row/col
		cal.add(java.util.Calendar.DATE, (row - 1) * 7 + col);

		return cal.getTime();
	}

	public void addChangeListener(ChangeListener l) {
		listenerList.add(ChangeListener.class, l);
	}

	public void removeChangeListener(ChangeListener l) {
		listenerList.remove(ChangeListener.class, l);
	}
	
	class CalendarSpinner extends JSpinner {
		private ChangeListener[] listeners;
		public CalendarSpinner(final int field, String format) {
			super();
			SpinnerDateModel model = new SpinnerDateModel();
			model.setCalendarField(field);
			setModel(model);
			JSpinner.DateEditor editor = new JSpinner.DateEditor(CalendarSpinner.this, format);
			setEditor(editor);
			JTextField textField = editor.getTextField();
			textField.setHorizontalAlignment(SwingConstants.RIGHT);
			addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					int value;
					int oldvalue;
					Date newDate = (Date)getValue();
					cal.setTime(newDate);
					value = cal.get(field);
					cal.setTime(date);
					oldvalue = cal.get(field);
					cal.add(field, value - oldvalue);
					if (value == oldvalue) return;
					Calendar.this.date = cal.getTime();
					fireChangeEvent(new ChangeEvent(Calendar.this));
				}
			});
			listeners = getChangeListeners();
		}
		
		public void addChangeListener() {
			for (int i = 0, n = listeners.length; i < n; i++) {
				addChangeListener(listeners[i]);
			}
		}
		
		public void removeChangeListener() {
			for (int i = 0, n = listeners.length; i < n; i++) {
				removeChangeListener(listeners[i]);
			}
		}
	}

	public static void main(String[] args) {
		final JFrame f = new JFrame("CalendarTest"); //$NON-NLS-1$
		final Calendar cal = new Calendar();
		f.getContentPane().add(cal);
		f.setSize(400, 300);
		f.setVisible(true);
		final DateFormat dayFormat = new SimpleDateFormat("yyyy/MM/dd"); //$NON-NLS-1$
		cal.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				Date date = cal.getDate();
				JOptionPane.showMessageDialog(f, dayFormat.format(date));
			}
		});
	}
}
