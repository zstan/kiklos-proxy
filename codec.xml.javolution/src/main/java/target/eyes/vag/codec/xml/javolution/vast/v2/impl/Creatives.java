package target.eyes.vag.codec.xml.javolution.vast.v2.impl;

import java.util.ArrayList;
import java.util.List;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class Creatives implements XMLSerializable {
	private static final long serialVersionUID = 9206160796025670453L;

	private List<Creative> creatives = new ArrayList<Creative>();

	protected static final XMLFormat<Creatives> CREATIVES_XML = new XMLFormat<Creatives>(
			Creatives.class) {

		@Override
		public void write(Creatives obj, OutputElement xml)
				throws XMLStreamException {
			for (Creative c : obj.getCreatives()) {
				xml.add(c, "Creative", Creative.class);
			}
		}

		@Override
		public void read(XMLFormat.InputElement xml, Creatives obj)
				throws XMLStreamException {
			while (xml.hasNext()) {
				obj.creatives.add((Creative) xml
						.get("Creative", Creative.class));
			}

		}
	};

	public List<Creative> getCreatives() {
		return creatives;
	}
}
