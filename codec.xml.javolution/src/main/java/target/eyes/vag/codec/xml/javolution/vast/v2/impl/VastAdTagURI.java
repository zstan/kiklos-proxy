package target.eyes.vag.codec.xml.javolution.vast.v2.impl;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

public class VastAdTagURI {

	protected static final XMLFormat<VastAdTagURI> URL_XML = new XMLFormat<VastAdTagURI>(
			VastAdTagURI.class) {

		@Override
		public void write(VastAdTagURI obj, OutputElement xml)
				throws XMLStreamException {
			if (obj.uri != null)
				xml.getStreamWriter().writeCData(obj.uri);
		}

		@Override
		public void read(InputElement xml, VastAdTagURI obj)
				throws XMLStreamException {
			obj.setUri(xml.getText().toString());
		}

	};

	private String uri;

	public String getURI() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
}