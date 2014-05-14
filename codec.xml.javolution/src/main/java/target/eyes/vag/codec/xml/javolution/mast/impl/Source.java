package target.eyes.vag.codec.xml.javolution.mast.impl;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class Source implements XMLSerializable {

	private static final long serialVersionUID = -7579906371530035163L;
	
	private String uri, format;

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
	
	protected static final XMLFormat<Source> SOURCE_XML = new XMLFormat<Source>(Source.class) {

		@Override
		public void write(Source obj, OutputElement xml) throws XMLStreamException {
			if (obj.uri != null)
				xml.setAttribute("uri", obj.uri);			
			if (obj.format != null)
				xml.setAttribute("format", obj.format);			
		}

		@Override
		public void read(InputElement xml, Source obj) throws XMLStreamException {
			obj.setUri(xml.getAttribute("uri", obj.uri));
			obj.setFormat(xml.getAttribute("format", obj.format));
		}

	};
	
}
