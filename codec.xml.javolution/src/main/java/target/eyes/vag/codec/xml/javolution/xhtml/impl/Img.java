package target.eyes.vag.codec.xml.javolution.xhtml.impl;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class Img implements XMLSerializable {

	private static final long serialVersionUID = 5083464121354163220L;

	private String src;

	public Img() {
	}

	public Img(String src) {
		this.src = src;
	}

	protected static final XMLFormat<Img> IMG_XML = new XMLFormat<Img>(
			Img.class) {
		@Override
		public void write(Img t, OutputElement xml) throws XMLStreamException {
			xml.setAttribute("src", t.src);
		}

		@Override
		public void read(InputElement xml, Img t) throws XMLStreamException {
			t.src = xml.getAttribute("src", null);
		}
	};

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

}
