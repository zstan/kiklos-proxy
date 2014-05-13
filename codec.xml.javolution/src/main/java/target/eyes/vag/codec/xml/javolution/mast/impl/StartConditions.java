package target.eyes.vag.codec.xml.javolution.mast.impl;

import java.util.ArrayList;
import java.util.List;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class StartConditions implements XMLSerializable{

	private static final long serialVersionUID = 3048888389948427354L;
	
	private List<Condition> conditions = new ArrayList<>();

	protected static final XMLFormat<StartConditions> START_CONDITIONS_XML = new XMLFormat<StartConditions>(
			StartConditions.class) {

		@Override
		public void write(StartConditions obj, OutputElement xml) throws XMLStreamException {
			for (Condition c : obj.getStartConditions()) {
				xml.add(c, "condition", Condition.class);
			}			
		}

		@Override
		public void read(XMLFormat.InputElement xml, StartConditions obj) throws XMLStreamException {
			while (xml.hasNext()) {	
				obj.conditions.add((Condition)xml.get("condition", Condition.class));
			}			
		}
	};
	
	public List<Condition> getStartConditions() {
		return conditions;
	}	
}
