package target.eyes.vag.codec.xml.javolution.mast.impl;

import java.util.ArrayList;
import java.util.List;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class Triggers implements XMLSerializable {

	private static final long serialVersionUID = -6554234959252378217L;

	private List<Trigger> triggers = new ArrayList<Trigger>();
	
	protected static final XMLFormat<Triggers> TRIGGERS_XML = new XMLFormat<Triggers>(
			Triggers.class) {

		@Override
		public void write(Triggers obj, OutputElement xml) throws XMLStreamException {
			for (Trigger t : obj.getTriggers()) {
				xml.add(t, "trigger", Trigger.class);
			}
		}

		@Override
		public void read(XMLFormat.InputElement xml, Triggers obj) throws XMLStreamException {
			while (xml.hasNext()) {	
				obj.triggers.add((Trigger)xml.get("trigger", Trigger.class));
			}
		}
	};
	
	public List<Trigger> getTriggers() {
		return triggers;
	}
}
