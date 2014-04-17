package target.eyes.vag.codec.xml.javolution.vast.impl;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class SingleValueTag implements XMLSerializable {
	private static final long serialVersionUID = -2940378703258364977L;

	private String text = null;

	private final String attrName;

	private String attribute;

	public SingleValueTag(String attrName) {
		this.attrName = attrName;
	}

	public SingleValueTag() {
		this(null);
	}

	protected static final XMLFormat<SingleValueTag> SINGLE_VALUE_TAG_XML = new XMLFormat<SingleValueTag>(
			SingleValueTag.class) {

		@Override
		public void write(SingleValueTag obj, OutputElement xml)
				throws XMLStreamException {
			if (obj.attrName != null && obj.attribute != null)
				xml.setAttribute(obj.attrName, obj.attribute);

			xml.addText(obj.text);
		}

		@Override
		public void read(InputElement xml, SingleValueTag obj)
				throws XMLStreamException {
			if (obj.attrName != null)
				obj.attribute = xml.getAttribute(obj.attrName, obj.attribute);

			obj.text = xml.getText().toString();
		}

	};

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

}
