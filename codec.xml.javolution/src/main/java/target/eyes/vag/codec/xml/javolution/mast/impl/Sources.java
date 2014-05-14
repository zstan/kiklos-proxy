package target.eyes.vag.codec.xml.javolution.mast.impl;

import java.util.ArrayList;
import java.util.List;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class Sources implements XMLSerializable {

	private static final long serialVersionUID = 8681162727547159274L;

	private List<Source> sources = new ArrayList<>();
	
	protected static final XMLFormat<Sources> SOURCES_XML = new XMLFormat<Sources>(
			Sources.class) {

		@Override
		public void write(Sources obj, OutputElement xml) throws XMLStreamException {
			for (Source s : obj.getSources()) {
				xml.add(s, "source", Source.class);
			}
		}

		@Override
		public void read(XMLFormat.InputElement xml, Sources obj) throws XMLStreamException {
			while (xml.hasNext()) {	
				obj.sources.add((Source)xml.get("source", Source.class));
			}
		}
	};
	
	public List<Source> getSources() {
		return sources;
	}	
}
