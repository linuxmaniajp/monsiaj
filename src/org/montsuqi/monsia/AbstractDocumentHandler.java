package org.montsuqi.monsia;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montsuqi.client.Protocol;
import org.montsuqi.util.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

abstract class AbstractDocumentHandler extends DefaultHandler {

	protected Logger logger;
	protected final StringBuffer content;
	protected final Map widgets;
	protected final List topLevels;
	protected final List properties;
	protected final List signals;
	protected final List accels;

	protected ParserState state;
	protected ParserState prevState;
	protected ParserState startState;

	protected int unknownDepth;
	protected int widgetDepth;
	protected WidgetInfo widget;
	protected String propertyName;
	protected PropertyType propertyType = PropertyType.NONE;

	public AbstractDocumentHandler() {
		super();
		logger = Logger.getLogger(AbstractDocumentHandler.class);
		content = new StringBuffer();
		widgets = new HashMap();
		topLevels = new ArrayList();
		properties = new ArrayList();
		signals = new ArrayList();
		accels = new ArrayList();
		
	}

	protected abstract boolean shouldAppendCharactersToContent();

	protected boolean isFinished() {
		return state == FINISH;
	}

	protected void clearContent() {
		content.delete(0, content.length());
	}

	protected void warnNotZero(String name, int actual) {
		logger.warn(Messages.getString("MonsiaHandler.not_zero"), new Object[] { name, new Integer(actual) }); //$NON-NLS-1$
	}

	protected void warnUnknownAttribute(String element, String attr) {
		logger.warn(Messages.getString("MonsiaHandler.unknown_attribute"), new Object[] { attr, element }); //$NON-NLS-1$
	}

	protected void warnMissingAttribute(String element) {
		logger.warn(Messages.getString("MonsiaHandler.missing_required_attribute"), element); //$NON-NLS-1$
	}

	protected void warnUnexpectedElement(String outer, String inner) {
		logger.warn(Messages.getString("MonsiaHandler.unexpected_element"), new Object[] { outer, inner }); //$NON-NLS-1$
	}

	protected Interface getInterface(Protocol protocol) {
		if (isFinished()) {
			return new Interface(widgets, topLevels, protocol);
		} else {
			throw new IllegalStateException(Messages.getString("MonsiaHandler.parsing_is_not_finished_yet")); //$NON-NLS-1$
		}
	}

	public void startDocument() throws SAXException {
		state = startState;
		unknownDepth = 0;
		prevState = UNKNOWN;
	
		widgetDepth = 0;
		clearContent();
		widget = null;
		propertyName = null;
	}

	public void endDocument() throws SAXException {
		if (unknownDepth != 0) {
			warnNotZero("unknownDepth", unknownDepth); //$NON-NLS-1$
		}
		if (widgetDepth != 0) {
			warnNotZero("widgetDepth", widgetDepth); //$NON-NLS-1$
		}
	}

	public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
		state.startElement(uri, localName, qName, attrs);
		clearContent();
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		state.endElement(uri, localName, qName);
	}

	protected final ParserState UNKNOWN = new ParserState("UNKNOWN") { //$NON-NLS-1$
		void startElement(String uri, String localName, String qName, Attributes attrs) {
			unknownDepth++;
		}

		void endElement(String uri, String localName, String qName) {
			unknownDepth--;
			if (unknownDepth == 0) {
				state = prevState;
			}
		}
	};

	final ParserState FINISH = new ParserState("FINISH") { //$NON-NLS-1$
		void startElement(String uri, String localName, String qName, Attributes attrs) {
			logger.warn(Messages.getString("AbstractHandler.there_should_be_no_elements_here"), localName); //$NON-NLS-1$
			prevState = state;
			state = UNKNOWN;
			unknownDepth++;
		}

		void endElement(String uri, String localName, String qName) {
			logger.warn(Messages.getString("AbstractHandler.should_not_be_closing_any_elements_in_this_state")); //$NON-NLS-1$
		}
	};

	public void warning(SAXParseException e) throws SAXException {
		logger.warn(e);
	}

	public void error(SAXParseException e) throws SAXException {
		logger.fatal(e);
	}

	public void fatalError(SAXParseException e) throws SAXException {
		logger.fatal(e);
	}

	protected void flushProperties() {
		if (propertyType == PropertyType.NONE) {
			// do nothing
		} else if (propertyType == PropertyType.WIDGET) {
			if (widget.getPropertiesCount() != 0) {
				logger.warn(Messages.getString("MonsiaHandler.we_already_read_all_the_props_for_this_key")); //$NON-NLS-1$
			}
			widget.setProperties(properties);
			properties.clear();
		} else if (propertyType == PropertyType.ATK) {
			if (widget.getATKPropertiesCount() != 0) {
				logger.warn(Messages.getString("MonsiaHandler.we_already_read_all_the_ATK_props_for_this_key")); //$NON-NLS-1$
			}
			widget.setATKProperties(properties);
			properties.clear();
		} else if (propertyType == PropertyType.CHILD) {
			if (widget.getChildrenCount() == 0) {
				logger.warn(Messages.getString("MonsiaHandler.no_children_but_have_child_properties")); //$NON-NLS-1$
				properties.clear();
			} else {
				ChildInfo info = widget.getLastChild();
				info.setProperties(properties);
				properties.clear();
			}
		} else {
			throw new IllegalStateException(Messages.getString("MonsiaHandler.unknown_property_type"));
		}
	
		propertyType = PropertyType.NONE;
		propertyName = null;
		properties.clear();
	}

	protected void flushSignals() {
		widget.setSignals(signals);
		signals.clear();
	}

	protected void flushAccels() {
		widget.setAccels(accels);
		accels.clear();
	}

	protected String normalizeKeyName(String keyName) {
		if (keyName.startsWith("GDK_")) { //$NON-NLS-1$
			keyName = keyName.substring(4);
		}
		if ( ! keyName.startsWith("VK_")) { //$NON-NLS-1$
			keyName = "VK_" + keyName; //$NON-NLS-1$
		}
		return keyName;
	}

	protected int keyCode(String keyName) {
		final Field[] fields = KeyEvent.class.getFields();
		keyName = normalizeKeyName(keyName);
		for (int i = 0; i < fields.length; i++) {
			if (keyName.equals(fields[i].getName())) {
				try {
					return fields[i].getInt(null);
				} catch (Exception e) {
					logger.warn(e);
					return 0;
				}
			}
		}
		return 0;
	}

	protected int parseModifiers(String modifierValue) {
		Reader reader = new StringReader(modifierValue);
		StreamTokenizer tokens = new StreamTokenizer(reader);
		tokens.ordinaryChars('\u0000', '\uffff');
		tokens.whitespaceChars('|', '|');
		tokens.whitespaceChars(' ', ' ');
	
		int modifiers = 0;
		try {
			while (tokens.nextToken() != StreamTokenizer.TT_EOF) {
				String modifier = tokens.sval;
				if (modifier.startsWith("GTK_")) {
					modifier = modifier.substring(4);
				}
				if (modifier.equals("SHIFT_MASK")) { //$NON-NLS-1$
					modifiers |= KeyEvent.SHIFT_MASK;
				} else if (modifier.equals("LOCK_MASK")) { //$NON-NLS-1$
					logger.warn(Messages.getString("MonsiaHandler.not_supported_in_Java"), "LOCK_MASK"); //$NON-NLS-1$ $NON-NLS-2$
				} else if (modifier.equals("CONTROL_MASK")) { //$NON-NLS-1$
					modifiers |= KeyEvent.CTRL_MASK;
				} else if (modifier.startsWith("MOD_")) { //$NON-NLS-1$
					logger.warn(Messages.getString("MonsiaHandler.not_supported_in_Java"), "MOD_MASK"); //$NON-NLS-1$ $NON-NLS-2$
				} else if (modifier.startsWith("BUTTON") && modifier.length() == 7) { //$NON-NLS-1$
					modifiers |= parseButtonMask(modifier.substring(6));
				} else if (modifier.equals("RELEASE_MASK")) { //$NON-NLS-1$
					logger.warn(Messages.getString("MonsiaHandler.not_supported_in_Java"), "RELEASE_MASK"); //$NON-NLS-1$ $NON-NLS-2$
				}
			}
		} catch (IOException e) {
			logger.warn(e); // no recovery action is needed.
		}
		return modifiers;
	}

	protected int parseButtonMask(String mask) {
		try {
			int value = Integer.parseInt(mask);
			switch (value) {
			case 1:
				return KeyEvent.BUTTON1_MASK;
			case 2:
				return KeyEvent.BUTTON2_MASK;
			case 3:
				return KeyEvent.BUTTON3_MASK;
			default:
				logger.warn(Messages.getString("MonsiaHandler.only_BUTTON1-3_are_supported_in_Java")); //$NON-NLS-1$
				return 0;
			}
		} catch (NumberFormatException e) {
			logger.warn(Messages.getString("MonsiaHandler.unknown_BUTTON__number"), mask); //$NON-NLS-1$
			return 0;
		}
	}

	/** returns name with all dashes converted to underscores. */
	protected String makePropertyName(String name) {
		return name.replace('-', '_');
	}

	public void characters(char[] chars, int start, int length) throws SAXException {
		if (shouldAppendCharactersToContent()) {
			content.append(chars, start, length);
		} else {
			clearContent();
		}
	}
}
