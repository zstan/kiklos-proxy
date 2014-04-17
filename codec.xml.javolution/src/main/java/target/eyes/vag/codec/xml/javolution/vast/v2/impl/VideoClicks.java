package target.eyes.vag.codec.xml.javolution.vast.v2.impl;

import java.util.ArrayList;
import java.util.List;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class VideoClicks implements XMLSerializable {

	private static final long serialVersionUID = -4159476622948686648L;

	private ClickURL clickThrough;

	private List<ClickURL> clickTracking;

	protected static final XMLFormat<VideoClicks> VIDEO_CLICKS_XML = new XMLFormat<VideoClicks>(
			VideoClicks.class) {

		@Override
		public void write(VideoClicks obj, OutputElement xml)
				throws XMLStreamException {
			xml.add(obj.clickThrough, "ClickThrough", ClickURL.class);

			if (obj.clickTracking != null)
				for (ClickURL u : obj.clickTracking)
					xml.add(u, "ClickTracking", ClickURL.class);

		}

		@Override
		public void read(InputElement xml, VideoClicks obj)
				throws XMLStreamException {
			obj.clickThrough = xml.get("ClickThrough", ClickURL.class);

			while (xml.hasNext()) {
				if (obj.clickTracking == null)
					obj.clickTracking = new ArrayList<ClickURL>();

				obj.clickTracking.add(xml.get("ClickTracking", ClickURL.class));

			}
		}

	};

	public static class ClickURL implements XMLSerializable {

		private static final long serialVersionUID = 1099483871475463914L;

		private String url;

		protected static final XMLFormat<ClickURL> CLICK_XML = new XMLFormat<ClickURL>(
				ClickURL.class) {
			@Override
			public void write(ClickURL obj, OutputElement xml)
					throws XMLStreamException {
				xml.getStreamWriter().writeCData(obj.url);
			}

			@Override
			public void read(InputElement xml, ClickURL obj)
					throws XMLStreamException {
				obj.setURL(xml.getText().toString());
			}

		};

		public String getURL() {
			return url;
		}

		public void setURL(String url) {
			this.url = url;
		}

		public ClickURL(String url) {
			this.url = url;
		}

		public ClickURL() {
		}

	}

	public ClickURL getClickThrough() {
		return clickThrough;
	}

	public void setClickThrough(ClickURL clickThrough) {
		this.clickThrough = clickThrough;
	}

	public List<ClickURL> getClickTracking() {
		return clickTracking;
	}

	public void setClickTracking(List<ClickURL> clickTracking) {
		this.clickTracking = clickTracking;
	}

}
