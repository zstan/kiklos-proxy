package target.eyes.vag.codec.xml.javolution.vast.impl;

import java.util.ArrayList;
import java.util.List;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

public class MediaFiles implements XMLSerializable {

	private static final long serialVersionUID = -8904071186622934576L;

	protected static final XMLFormat<MediaFiles> MEDIA_FILES_XML = new XMLFormat<MediaFiles>(
			MediaFiles.class) {
		@Override
		public void write(MediaFiles obj, OutputElement xml)
				throws XMLStreamException {
			for (MediaFile mf : obj.mediaFiles) {
				xml.add(mf, "MediaFile", MediaFile.class);
			}
		}

		@Override
		public void read(InputElement xml, MediaFiles obj)
				throws XMLStreamException {
		}

	};

	private List<MediaFile> mediaFiles = new ArrayList<MediaFile>();

	public static class MediaFile implements XMLSerializable {

		private static final long serialVersionUID = 2461059389204500331L;

		private String delivery = null;

		private String type = null;

		private int height = 100;

		private int width = 100;

		private int bitrate = 500;

		private SingleValueTag URL = new SingleValueTag();

		protected static final XMLFormat<MediaFile> MEDIA_FILE_XML = new XMLFormat<MediaFile>(
				MediaFile.class) {
			@Override
			public void write(MediaFile obj, OutputElement xml)
					throws XMLStreamException {
				xml.setAttribute("delivery", obj.getDelivery().toString());
				xml.setAttribute("type", obj.getType().toString());
				xml.setAttribute("height", obj.getHeight());
				xml.setAttribute("width", obj.getWidth());
				xml.setAttribute("bitrate", obj.getBitrate());
				xml.add(obj.URL, "URL", SingleValueTag.class);
			}

			@Override
			public void read(InputElement xml, MediaFile obj)
					throws XMLStreamException {
			}

		};

		public String getDelivery() {
			return delivery;
		}

		public void setDelivery(String delivery) {
			this.delivery = delivery;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getBitrate() {
			return bitrate;
		}

		public void setBitrate(int bitrate) {
			this.bitrate = bitrate;
		}

		public CharSequence getURL() {
			return URL.getText();
		}

		public void setURL(String URL) {
			this.URL.setText(URL);
		}
	}

	public List<MediaFile> getMediaFiles() {
		return mediaFiles;
	}

	public void setMedaiFile(ArrayList<MediaFile> mediaFile) {
		this.mediaFiles = mediaFile;
	}
}