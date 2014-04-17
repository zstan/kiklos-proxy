package target.eyes.vag.codec.xml.javolution.vast.v2.impl;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

import java.util.LinkedList;
import java.util.List;

public class Extensions implements XMLSerializable {

	private static final long serialVersionUID = -3892400028237826293L;

	private List<Extension> extensions = new LinkedList<>();

	protected static final XMLFormat<Extensions> CREATIVES_XML = new XMLFormat<Extensions>(
			Extensions.class) {

		@Override
		public void write(Extensions obj, OutputElement xml)
				throws XMLStreamException {
			for (Extension c : obj.getExtensions()) {
				xml.add(c, "Extension", Extension.class);
			}
		}

		@Override
		public void read(XMLFormat.InputElement xml, Extensions obj)
				throws XMLStreamException {
			while (xml.hasNext()) {
                Extension e = xml.get("Extension", Extension.class);
                // If not <Extension> -> try to read & skip
                if (e == null) xml.getNext();
                else obj.extensions.add(e);
			}

		}
	};

	public List<Extension> getExtensions() {
		return extensions;
	}
}
