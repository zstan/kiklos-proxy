package target.eyes.vag.codec.xml.javolution.mast.impl;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class Trigger implements XMLSerializable {

	private static final long serialVersionUID = 2042852089516259805L;

	private String id, description;
	private StartConditions startConditions;
	private Sources sources;

	public Sources getSources() {
		return sources;
	}

	public void setSources(Sources sources) {
		this.sources = sources;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public StartConditions getStartConditions() {
		return this.startConditions;
	}

	public void setStartConditions(StartConditions startConditions) {
		this.startConditions = startConditions;
	}

	protected static final XMLFormat<Trigger> TRIGGERS_XML = new XMLFormat<Trigger>(Trigger.class) {

		@Override
		public void write(Trigger obj, OutputElement xml) throws XMLStreamException {
			if (obj.id != null)
				xml.setAttribute("id", obj.id);			
			if (obj.description != null)
				xml.setAttribute("description", obj.description);
			xml.add(obj.startConditions, "startConditions", StartConditions.class);
			xml.add(obj.sources, "sources", Sources.class);
		}

		@Override
		public void read(InputElement xml, Trigger obj) throws XMLStreamException {
			obj.setId(xml.getAttribute("id", obj.id));
			obj.setDescription(xml.getAttribute("description", obj.description));
			obj.setStartConditions(xml.get("startConditions", StartConditions.class));
			obj.setSources(xml.get("sources", Sources.class));
		}

	};

}
