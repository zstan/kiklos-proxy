package target.eyes.vag.codec.xml.javolution.vast.v2.impl;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

public class Impression {

	protected static final XMLFormat<Impression> URL_XML = new XMLFormat<Impression>(
			Impression.class) {

		@Override
		public void write(Impression obj, OutputElement xml)
				throws XMLStreamException {
			if (obj.url != null)
				xml.getStreamWriter().writeCData(obj.url);
		}

		@Override
		public void read(InputElement xml, Impression obj)
				throws XMLStreamException {
			obj.setUrl(xml.getText().toString());
		}

	};

	private String url;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}