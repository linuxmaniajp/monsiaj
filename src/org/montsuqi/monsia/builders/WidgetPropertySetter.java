/*      PANDA -- a simple transaction monitor
                                                                                
Copyright (C) 1998-1999 Ogochan.
			  2000-2003 Ogochan & JMA (Japan Medical Association).
                                                                                
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

package org.montsuqi.monsia.builders;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;

import org.montsuqi.monsia.Interface;
import org.montsuqi.monsia.WidgetInfo;
import org.montsuqi.util.Logger;
import org.montsuqi.util.ParameterConverter;
import org.montsuqi.widgets.NumberEntry;
import org.montsuqi.widgets.PandaEntry;
import org.montsuqi.widgets.PandaHTML;
import org.montsuqi.widgets.PandaTimer;

abstract class WidgetPropertySetter {

	abstract void set(Interface xml, Container parent, Component widget, String value);

	static Logger logger;
	private static Map propertyMap;

	static void setProperties(Interface xml, Container parent, Component widget, WidgetInfo info) {
		Iterator i = info.getProperties().entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry ent = (Map.Entry)i.next();
			String name = (String)ent.getKey();
			Class clazz = widget.getClass();
			try {
				WidgetPropertySetter setter = getSetter(clazz, name);
				if (setter == null) {
					//logger.debug("setter not found");
					continue;
				}
				setter.set(xml, parent, widget, (String)ent.getValue());
			} catch (ClassCastException e) {
				String message = Messages.getString("WidgetPropertySetter.invalid_widget_type"); //$NON-NLS-1$
				message = MessageFormat.format(message, new Object[] { clazz });	
				throw new IllegalArgumentException(message);
			}
		}
	}

	static WidgetPropertySetter getSetter(Class clazz, String name) {
		for (/**/; clazz != null; clazz = clazz.getSuperclass()) {
			Map map = (Map)propertyMap.get(clazz);
			if (map == null || ! map.containsKey(name)) {
				continue;
			}
			WidgetPropertySetter setter = (WidgetPropertySetter)map.get(name);
			if (setter != null) {
				return setter;
			}
		}
		return null;
	}

	private static void registerProperty(Class clazz, String propertyName, WidgetPropertySetter setter) {
		if ( ! propertyMap.containsKey(clazz)) {
			propertyMap.put(clazz, new HashMap());
		}
		Map map = (Map)propertyMap.get(clazz);
		map.put(propertyName, setter);
	}

	static {
		logger = Logger.getLogger(WidgetPropertySetter.class);
		propertyMap = new HashMap();

		registerProperty(AbstractButton.class, "label", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				AbstractButton button = (AbstractButton)widget;
				button.setText(value);
			}
		});

		registerProperty(Component.class, "width_request", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				Dimension size = widget.getSize();
				try {
					size.width = Integer.parseInt(value);
					widget.setSize(size);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("not a number"); //$NON-NLS-1$
				}
			} 
		});
		registerProperty(Component.class, "width", getSetter(Component.class, "width_request")); //$NON-NLS-1$ //$NON-NLS-2$

		registerProperty(Component.class, "height_request", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				Dimension size = widget.getSize();
				try {
					size.height = Integer.parseInt(value);
					widget.setSize(size);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(Messages.getString("WidgetPropertySetter.not_a_number")); //$NON-NLS-1$
				}
			}
		});
		registerProperty(Component.class, "height", getSetter(Component.class, "height_request")); //$NON-NLS-1$ //$NON-NLS-2$

		registerProperty(Component.class, "visible", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				boolean visible = ParameterConverter.toBoolean(value);
				widget.setVisible(visible);
			}
		});

		registerProperty(Component.class, "tooltip", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JComponent c = (JComponent)widget;
				c.setToolTipText(value);
			}
		});

		registerProperty(Component.class, "has_default", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				if (ParameterConverter.toBoolean(value)) {
					xml.setDefaultWidget(widget);
				}
			}
		});

		registerProperty(Component.class, "has_focus", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				if (ParameterConverter.toBoolean(value)) {
					xml.setFocusWidget(widget);
				}
			}
		});

		registerProperty(java.awt.Frame.class, "title", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				java.awt.Frame frame = (java.awt.Frame)widget;
				frame.setTitle(value);
			}
		});

		registerProperty(JLabel.class, "label", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JLabel label = (JLabel)widget;
				label.setText(value.indexOf("\n") >= 0 ? makeHTML(value) : value); //$NON-NLS-1$
			}

			// convert multi-line label value into HTML
			private String makeHTML(String value) {
				StringBuffer buf = new StringBuffer("<html>"); //$NON-NLS-1$
				StringTokenizer tokens = new StringTokenizer(value, "\n\"<>&", true); //$NON-NLS-1$
				while (tokens.hasMoreTokens()) {
					String token = tokens.nextToken();
					if ("\n".equals(token)) { //$NON-NLS-1$
						buf.append("<br>"); //$NON-NLS-1$
					} else if ("\"".equals(token)) { //$NON-NLS-1$
						buf.append("&dquot;"); //$NON-NLS-1$
					} else if ("<".equals(token)) { //$NON-NLS-1$
						buf.append("&lt;"); //$NON-NLS-1$
					} else if (">".equals(token)) { //$NON-NLS-1$
						buf.append("&gt;"); //$NON-NLS-1$
					} else if ("&".equals(token)) { //$NON-NLS-1$
						buf.append("&amp;"); //$NON-NLS-1$
					} else {
						buf.append(token);
					}
				}
				buf.append("</html>"); //$NON-NLS-1$
				value = buf.toString();
				return value;
			}
		});

		registerProperty(JLabel.class, "xalign", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JLabel label = (JLabel)widget;
				double align = Double.parseDouble(value);
				if (align < 0.5) {
					label.setHorizontalAlignment(SwingConstants.LEFT);
				} else if (0.5 < align) {
					label.setHorizontalAlignment(SwingConstants.RIGHT);
				} else {
					label.setHorizontalAlignment(SwingConstants.CENTER);
				}
			}
		});

		registerProperty(JLabel.class, "yalign", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JLabel label = (JLabel)widget;
				double align = Double.parseDouble(value);
				if (align < 0.5) {
					label.setVerticalAlignment(SwingConstants.TOP);
				} else if (0.5 < align) {
					label.setVerticalAlignment(SwingConstants.BOTTOM);
				} else {
					label.setVerticalAlignment(SwingConstants.CENTER);
				}
			}
		});

		registerProperty(JList.class, "selection_mode", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JList list = (JList)widget;
				value = normalize(value, "SELECTION_"); //$NON-NLS-1$
				if ("SINGLE".equals(value)) { //$NON-NLS-1$
					list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				} else if ("MULTIPLE".equals(value)) { //$NON-NLS-1$
					list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				} else if ("EXTENDED".equals(value)) { //$NON-NLS-1$
					logger.warn(Messages.getString("WidgetPropertySetter.not_supported"), value); //$NON-NLS-1$
				} else if ("BROWSE".equals(value)) { //$NON-NLS-1$
					logger.warn(Messages.getString("WidgetPropertySetter.not_supported"), value); //$NON-NLS-1$
				} else {
					throw new IllegalArgumentException("invalid selection mode"); //$NON-NLS-1$
				}
			}
		});

		registerProperty(JTextComponent.class, "editable", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JTextComponent text = (JTextComponent)widget;
				text.setEditable(ParameterConverter.toBoolean(value));
			}
		});

		registerProperty(JTextComponent.class, "text", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JTextComponent text = (JTextComponent)widget;
				text.setText(value);
			}
		});

		registerProperty(JTextArea.class, "text", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JTextArea text = (JTextArea)widget;
				text.setText(value);
			}
		});

		registerProperty(JTextField.class, "justify", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JTextField text = (JTextField)widget;
				value = normalize(value, "JUSTIFY_"); //$NON-NLS-1$
				if ("CENTER".equals(value)) { //$NON-NLS-1$
					text.setHorizontalAlignment(SwingConstants.CENTER);
				} else if ("LEFT".equals(value)) { //$NON-NLS-1$
					text.setHorizontalAlignment(SwingConstants.LEFT);
				} else if ("RIGHT".equals(value)) { //$NON-NLS-1$
					text.setHorizontalAlignment(SwingConstants.RIGHT);
				} else {
					logger.warn(Messages.getString("WidgetPropertySetter.not_supported"), value); //$NON-NLS-1$
				}
			}
		});

		registerProperty(JPasswordField.class, "invisible_char", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JPasswordField password = (JPasswordField)widget;
				password.setEchoChar(value.charAt(0));
			}
		});

		registerProperty(NumberEntry.class, "format", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				NumberEntry entry = (NumberEntry)widget;
				entry.setFormat(value);		
			}
		});

		registerProperty(PandaEntry.class, "input_mode", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				PandaEntry entry = (PandaEntry)widget;
				if (value.equals("ASCII")) { //$NON-NLS-1$
					entry.setInputMode(PandaEntry.ASCII);
				} else if (value.equals("KANA")) { //$NON-NLS-1$
					entry.setInputMode(PandaEntry.KANA);
				} else if (value.equals("XIM")) { //$NON-NLS-1$
					entry.setInputMode(PandaEntry.XIM);
				} else {
					throw new IllegalArgumentException(Messages.getString("WidgetPropertySetter.invalid_input_mode")); //$NON-NLS-1$
				}
			}
		});

		registerProperty(PandaEntry.class, "xim_enabled", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				PandaEntry entry = (PandaEntry)widget;
				entry.setXIMEnabled(ParameterConverter.toBoolean(value));
			}
		});

		registerProperty(JProgressBar.class, "lower", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JProgressBar progress = (JProgressBar)widget;
				BoundedRangeModel model = progress.getModel();
				model.setMinimum(ParameterConverter.toInteger(value));
			}
		});

		registerProperty(JProgressBar.class, "upper", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JProgressBar progress = (JProgressBar)widget;
				BoundedRangeModel model = progress.getModel();
				model.setMaximum(ParameterConverter.toInteger(value));
			}
		});

		registerProperty(JProgressBar.class, "value", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JProgressBar progress = (JProgressBar)widget;
				BoundedRangeModel model = progress.getModel();
				model.setValue(ParameterConverter.toInteger(value));
			}
		});

		registerProperty(JProgressBar.class, "orientation", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JProgressBar progress = (JProgressBar)widget;
				value = normalize(value, "PROGRESS_"); //$NON-NLS-1$
				if ("LEFT_TO_RIGHT".equals(value)) { //$NON-NLS-1$
					progress.setOrientation(SwingConstants.HORIZONTAL);
				} else if ("RIGHT_TO_LEFT".equals(value)) { //$NON-NLS-1$
					progress.setOrientation(SwingConstants.HORIZONTAL);
				} else if ("TOP_TO_BOTTOM".equals(value)) { //$NON-NLS-1$
					progress.setOrientation(SwingConstants.VERTICAL);
				} else if ("BOTTOM_TO_TOP".equals(value)) { //$NON-NLS-1$
					progress.setOrientation(SwingConstants.VERTICAL);
				}
			}
		});

		registerProperty(JProgressBar.class, "show_text", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JProgressBar progress = (JProgressBar)widget;
				progress.setStringPainted(ParameterConverter.toBoolean(value));
			}
		});

		registerProperty(JTable.class, "columns", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JTable table = (JTable)widget;
				TableColumnModel model = table.getColumnModel();
				int columns = ParameterConverter.toInteger(value);
				while (model.getColumnCount() < columns) {
					model.addColumn(new TableColumn());
				}
			}
		});

		registerProperty(JTable.class, "column_widths", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JTable table = (JTable)widget;
				TableColumnModel model = table.getColumnModel();				
				StringTokenizer tokens = new StringTokenizer(value, String.valueOf(','));
				int columns = tokens.countTokens();
				if (model.getColumnCount() < columns) {
					WidgetPropertySetter setter = getSetter(JTable.class, "columns"); //$NON-NLS-1$
					setter.set(xml, parent, widget, String.valueOf(columns));
				}
				int totalWidth = 0;
				for (int i = 0; tokens.hasMoreTokens(); i++) {
					TableColumn column = model.getColumn(i);
					int width = ParameterConverter.toInteger(tokens.nextToken());
					column.setPreferredWidth(width);
					column.setMinWidth(width);
					totalWidth += width;
				}
				if (parent instanceof JScrollPane) {
					JScrollPane scroll = (JScrollPane)parent;
					int parentWidth = scroll.getWidth();
					Insets insets = scroll.getInsets();
					parentWidth -= insets.left + insets.right;
					JScrollBar vScrollBar = scroll.getVerticalScrollBar();
					if (vScrollBar != null) {
						ComponentUI ui = vScrollBar.getUI();
						parentWidth -= ui.getPreferredSize(vScrollBar).getWidth();
					}
					JViewport columnHeader = scroll.getColumnHeader();
					if (columnHeader != null) {
						ComponentUI ui = columnHeader.getUI();
						parentWidth -= ui.getPreferredSize(columnHeader).getWidth();
					}
					if (totalWidth < parentWidth) {
						int diff = parentWidth - totalWidth;
						int delta = diff / columns;
						for (int i = 0; i < columns; i++) {
							TableColumn column = model.getColumn(i);
							int width = column.getPreferredWidth();
							width += delta;
							column.setPreferredWidth(width);
							column.setMinWidth(width);
						}
					}
				}
			}
		});

		registerProperty(JTable.class, "selection_mode", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JTable table = (JTable)widget;
				value = normalize(value, "SELECTION_"); //$NON-NLS-1$
				if ("SINGLE".equals(value)) { //$NON-NLS-1$
					table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				} else if ("MULTIPLE".equals(value)) { //$NON-NLS-1$
					table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				} else if ("EXTENDED".equals(value)) { //$NON-NLS-1$
					logger.info(Messages.getString("WidgetPropertySetter.not_supported")); //$NON-NLS-1$
				} else if ("BROWSE".equals(value)) { //$NON-NLS-1$
					logger.info(Messages.getString("WidgetPropertySetter.not_supported")); //$NON-NLS-1$
				} else {
					throw new IllegalArgumentException(value);
				}
			}
		});

		registerProperty(JTable.class, "show_titles", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JTable table = (JTable)widget;
				JTableHeader header = table.getTableHeader();
				header.setVisible(ParameterConverter.toBoolean(value));
			}
		});

		registerProperty(PandaHTML.class, "uri", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				PandaHTML pane = (PandaHTML)widget;
				URL uri;
				try {
					uri = new URL(value);
					pane.setURI(uri);
				} catch (MalformedURLException e) {
					pane.setText(e.toString());
				}
			}

		});

		registerProperty(PandaTimer.class, "duration", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				PandaTimer timer = (PandaTimer)widget;
				timer.setDuration(ParameterConverter.toInteger(value));
			}
		});

		registerProperty(org.montsuqi.widgets.Frame.class, "label", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				org.montsuqi.widgets.Frame frame = (org.montsuqi.widgets.Frame)widget;
				frame.setBorder(BorderFactory.createTitledBorder(value));
			}
		});

		registerProperty(JScrollPane.class, "hscrollbar_policy", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JScrollPane scroll = (JScrollPane)widget;
				value = normalize(value, "POLICY_"); //$NON-NLS-1$
				if ("ALWAYS".equals(value)) { //$NON-NLS-1$
					scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
				} else if ("AUTOMATIC".equals(value)) { //$NON-NLS-1$
					scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				} else if ("NEVER".equals(value)) { //$NON-NLS-1$
					scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				} else {
					throw new IllegalArgumentException(value);
				}
			}
		});

		registerProperty(JScrollPane.class, "vscrollbar_policy", new WidgetPropertySetter() { //$NON-NLS-1$
			public void set(Interface xml, Container parent, Component widget, String value) {
				JScrollPane scroll = (JScrollPane)widget;
				value = normalize(value, "POLICY_"); //$NON-NLS-1$
				if ("ALWAYS".equals(value)) { //$NON-NLS-1$
					scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				} else if ("AUTOMATIC".equals(value)) { //$NON-NLS-1$
					scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
				} else if ("NEVER".equals(value)) { //$NON-NLS-1$
					scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
				} else {
					throw new IllegalArgumentException(value);
				}
			}
		});
	}

	static String normalize(String value, String prefixToRemove) {
		if (value.startsWith("GDK_")) { //$NON-NLS-1$
			value = value.substring("GDK_".length()); //$NON-NLS-1$
		}
		if (value.startsWith("GTK_")) { //$NON-NLS-1$
			value = value.substring("GTK_".length()); //$NON-NLS-1$
		}
		if (value.startsWith(prefixToRemove)) {
			value = value.substring(prefixToRemove.length());
		}
		return value;
	}
}
