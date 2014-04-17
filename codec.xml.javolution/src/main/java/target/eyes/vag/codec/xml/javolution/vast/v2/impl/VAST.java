package target.eyes.vag.codec.xml.javolution.vast.v2.impl;

import java.util.ArrayList;
import java.util.List;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class VAST implements XMLSerializable {

	private static final long serialVersionUID = -2528967495131097233L;

	private String version;

	private String xmlns;

	private String xsi;

	private List<Ad> ads = new ArrayList<Ad>();

	protected static final XMLFormat<VAST> VIDEO_ADSERVING_TEMPLATE_XML = new XMLFormat<VAST>(
			VAST.class) {
		@Override
		public void write(VAST t, OutputElement xml) throws XMLStreamException {
			xml.setAttribute("version", t.version);
			xml.setAttribute("xmlns:xsi", t.xmlns);
			xml.setAttribute("xsi:noNamespaceSchemaLocation", t.xsi);
			for (Ad ad : t.getAds()) {
				xml.add(ad, "Ad", Ad.class);
			}
		}

		@Override
		public void read(InputElement xml, VAST t) throws XMLStreamException {
			t.setVersion(xml.getAttribute("version", t.version));
			t.setXmlns(xml.getAttribute("xmlns:xsi", t.xmlns));
			t.setXsi(xml.getAttribute("xsi:noNamespaceSchemaLocation", t.xsi));
			while (xml.hasNext()) {
				t.ads.add((Ad) xml.get("Ad", Ad.class));
			}

		}
	};

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

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

	public List<Ad> getAds() {
		return ads;
	}
}
