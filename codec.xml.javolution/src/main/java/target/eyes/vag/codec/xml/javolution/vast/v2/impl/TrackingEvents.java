package target.eyes.vag.codec.xml.javolution.vast.v2.impl;

import java.util.ArrayList;
import java.util.List;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class TrackingEvents implements XMLSerializable {

	private static final long serialVersionUID = 4329272557607553045L;

	private List<Tracking> trackings = new ArrayList<Tracking>();

	protected static final XMLFormat<TrackingEvents> TRACKING_EVENTS_XML = new XMLFormat<TrackingEvents>(
			TrackingEvents.class) {
		@Override
		public void write(TrackingEvents obj, OutputElement xml)
				throws XMLStreamException {
			for (TrackingEvents.Tracking t : obj.trackings) {
				xml.add(t, "Tracking", Tracking.class);
			}
		}

		@Override
		public void read(InputElement xml, TrackingEvents obj)
				throws XMLStreamException {
			while (xml.hasNext())
				obj.trackings.add((Tracking) xml
						.get("Tracking", Tracking.class));
		}

	};

	public static class Tracking implements XMLSerializable {

		private static final long serialVersionUID = -3564564661404503777L;

		private String event = null;

		private String url;

		protected static final XMLFormat<Tracking> TRACKINGS_XML = new XMLFormat<Tracking>(
				Tracking.class) {
			@Override
			public void write(TrackingEvents.Tracking obj, OutputElement xml)
					throws XMLStreamException {
				xml.setAttribute("event", obj.event.toString());
				xml.getStreamWriter().writeCData(obj.url);
			}

			@Override
			public void read(InputElement xml, Tracking obj)
					throws XMLStreamException {
				obj.setEvent(xml.getAttribute("event", obj.event));
				obj.setURL(xml.getText().toString());
			}

		};

		public String getEvent() {
			return event;
		}

		public void setEvent(String event) {
			this.event = event;
		}

		public String getURL() {
			return url;
		}

		public void setURL(String URL) {
			this.url = URL;
		}

	}

	public List<Tracking> getTrackings() {
		return trackings;
	}

}