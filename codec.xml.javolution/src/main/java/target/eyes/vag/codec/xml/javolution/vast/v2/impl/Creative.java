package target.eyes.vag.codec.xml.javolution.vast.v2.impl;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class Creative implements XMLSerializable {

	private String id;

	private int sequence; // can be 0 or null...

	private String AdID;

	private static final long serialVersionUID = 233568178706342793L;

	private Linear linear;

	protected static final XMLFormat<Creative> CREATIVES_XML = new XMLFormat<Creative>(
			Creative.class) {

		@Override
		public void write(Creative obj, OutputElement xml)
				throws XMLStreamException {
			if (obj.id != null)
				xml.setAttribute("id", obj.id);
			if (obj.sequence != 0)
				xml.setAttribute("sequence", obj.sequence);
			if (obj.AdID != null)
				xml.setAttribute("AdID", obj.AdID);
			xml.add(obj.linear, "Linear", Linear.class);
		}

		@Override
		public void read(XMLFormat.InputElement xml, Creative obj)
				throws XMLStreamException {
			obj.setId(xml.getAttribute("id", obj.id));

			// obj.setSequence(xml.getAttribute("sequence", obj.sequence));

			// Work around OpenX bug -- sequence=""
			String seq = xml.getAttribute("sequence", "");
			if (seq != null && !seq.isEmpty())
				obj.sequence = Integer.parseInt(seq);

			obj.setAdID(xml.getAttribute("AdID", obj.AdID));
			obj.setLinear(xml.get("Linear", Linear.class));
		}
	};

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public String getAdID() {
		return AdID;
	}

	public void setAdID(String adID) {
		AdID = adID;
	}

	public Linear getLinear() {
		return linear;
	}

	public void setLinear(Linear linear) {
		this.linear = linear;
	}
}
