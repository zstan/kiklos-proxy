package target.eyes.vag.codec.xml.javolution.mast.impl;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class Condition implements XMLSerializable {

	private static final long serialVersionUID = -2403953204304148774L;
	
	private String type, name, value, operator;
	
	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	protected static final XMLFormat<Condition> START_CONDITIONS_XML = new XMLFormat<Condition>(Condition.class) {
		@Override
		public void write(Condition obj, OutputElement xml) throws XMLStreamException {
			if (obj.type != null)
				xml.setAttribute("type", obj.type);			
			if (obj.name != null)
				xml.setAttribute("name", obj.name);
			if (obj.value != null)
				xml.setAttribute("value", obj.value);			
			if (obj.operator != null)
				xml.setAttribute("operator", obj.operator);			
		}
	
		@Override
		public void read(InputElement xml, Condition obj) throws XMLStreamException {
			obj.setType(xml.getAttribute("type", obj.type));
			obj.setName(xml.getAttribute("name", obj.name));			
			obj.setValue(xml.getAttribute("value", obj.value));
			obj.setName(xml.getAttribute("operator", obj.operator));
		}

	};	

}
