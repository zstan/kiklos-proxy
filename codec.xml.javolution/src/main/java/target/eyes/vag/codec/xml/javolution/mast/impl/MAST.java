package target.eyes.vag.codec.xml.javolution.mast.impl;

import java.util.ArrayList;
import java.util.List;

import target.eyes.vag.codec.xml.javolution.vast.v2.impl.Creatives;
import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class MAST implements XMLSerializable {

	private static final long serialVersionUID = -6528228775574106484L;
	
	private String schemaLocation;
	private String xmlns;
	private String xsi;	
	private Triggers triggers = new Triggers();

	protected static final XMLFormat<MAST> VIDEO_ADSERVING_TEMPLATE_XML = new XMLFormat<MAST>(
			MAST.class) {
		@Override
		public void write(MAST t, OutputElement xml) throws XMLStreamException {
			xml.setAttribute("xmlns", t.xmlns);
			xml.setAttribute("xmlns:xsi", t.xsi);
			xml.setAttribute("xsi:schemaLocation", t.schemaLocation);			
			xml.add(t.triggers, "triggers", Triggers.class);
		}

		@Override
		public void read(InputElement xml, MAST t) throws XMLStreamException {
			t.setSchemaLocation(xml.getAttribute("xsi:schemaLocation", t.schemaLocation));
			t.setXmlns(xml.getAttribute("xmlns", t.xmlns));
			t.setXsi(xml.getAttribute("xmlns:xsi", t.xsi));			
			t.setTriggers(xml.get("triggers", Triggers.class));
		}
	};

	public String getXmlns() {
		return xmlns;
	}

	public void setXmlns(String xmlns) {
		this.xmlns = xmlns;
	}

	public String getXsi() {
		return xsi;
	}

	public void setXsi(String xsi) {
		this.xsi = xsi;
	}
	
	public String getSchemaLocation() {
		return schemaLocation;
	}

	public void setSchemaLocation(String schemaLocation) {
		this.schemaLocation = schemaLocation;
	}
	
	public void setTriggers(Triggers tr) {
		this.triggers = tr;
	}
	
	public Triggers getTriggers() {
		return triggers;
	}
	
}
