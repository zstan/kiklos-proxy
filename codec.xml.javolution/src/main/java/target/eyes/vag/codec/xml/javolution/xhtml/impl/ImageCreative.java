package target.eyes.vag.codec.xml.javolution.xhtml.impl;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class ImageCreative implements XMLSerializable {

	private static final long serialVersionUID = 8542703125687612130L;

	private Img img;

	protected static final XMLFormat<ImageCreative> IMG_XML = new XMLFormat<ImageCreative>(
			ImageCreative.class) {
		@Override
		public void write(ImageCreative t, OutputElement xml)
				throws XMLStreamException {
			xml.add(t.img, "img", Img.class);
		}

		@Override
		public void read(InputElement xml, ImageCreative t)
				throws XMLStreamException {
			t.img = xml.get("img", Img.class);
		}
	};

	public String getSrc() {
		return img.getSrc();
	}

	public void setSrc(String src) {
		this.img.setSrc(src);
	}

}
