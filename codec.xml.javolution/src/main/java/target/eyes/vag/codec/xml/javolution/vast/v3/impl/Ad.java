package target.eyes.vag.codec.xml.javolution.vast.v3.impl;

import target.eyes.vag.codec.xml.javolution.vast.v2.impl.InLine;
import target.eyes.vag.codec.xml.javolution.vast.v2.impl.Wrapper;
import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class Ad implements XMLSerializable {

	private static final long serialVersionUID = -5367747036033096645L;
	
	private String id, sequence = "";
	private InLine inLine;
	private Wrapper wrapper;

	protected static final XMLFormat<Ad> AD_XML = new XMLFormat<Ad>(Ad.class) {

		@Override
		public void write(Ad ad, OutputElement xml) throws XMLStreamException {
			xml.setAttribute("id", ad.id);
			xml.setAttribute("sequence", ad.sequence);
			xml.add(ad.inLine, "InLine", InLine.class);
			xml.add(ad.wrapper, "Wrapper", Wrapper.class);
		}

		@Override
		public void read(InputElement xml, Ad obj) throws XMLStreamException {
			obj.setId(xml.getAttribute("id", obj.id));
			obj.setSequence(xml.getAttribute("sequence", obj.sequence));
			obj.setInLine(xml.get("InLine", InLine.class));
			obj.setWrapper(xml.get("Wrapper", Wrapper.class));
		}

	};

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getSequence() {
		return sequence;
	}

	public void setSequence(final String sequence) {
		this.sequence = sequence;
	}

	public InLine getInLine() {
		return inLine;
	}

	public void setInLine(InLine inLine) {
		this.inLine = inLine;
	}

	public Wrapper getWrapper() {
		return wrapper;
	}

	public void setWrapper(Wrapper wrapper) {
		this.wrapper = wrapper;
	}

}

