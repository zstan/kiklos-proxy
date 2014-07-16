package target.eyes.vag.codec.xml.javolution.vast.v2.impl;

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
			while (xml.hasNext())
				obj.mediaFiles.add((MediaFile) xml.get("MediaFile",
						MediaFile.class));

		}

	};

	private List<MediaFile> mediaFiles = new ArrayList<MediaFile>();

	public static class MediaFile implements XMLSerializable {

		private static final long serialVersionUID = 2461059389204500331L;

		private String id = null;
		
		private String delivery = null;

		private String type = null;
		
		private String bitrate = "500";
		
		private String maintainAspectRatio = "false", scalable = "true";

		private int height = 100;

		private int width = 100;
		

		private String url;

		protected static final XMLFormat<MediaFile> MEDIA_FILE_XML = new XMLFormat<MediaFile>(
				MediaFile.class) {
			@Override
			public void write(MediaFile obj, OutputElement xml)
					throws XMLStreamException {
				xml.setAttribute("id", obj.getId());
				xml.setAttribute("delivery", obj.getDelivery());
				xml.setAttribute("type", obj.getType());
				xml.setAttribute("bitrate", obj.getBitrate());
				xml.setAttribute("maintainAspectRatio", obj.getAsRatio());
				xml.setAttribute("scalable", obj.getScalable());
				
				xml.setAttribute("width", obj.getWidth());
				xml.setAttribute("height", obj.getHeight());								
				xml.getStreamWriter().writeCData(obj.url);
			}

			@Override
			public void read(InputElement xml, MediaFile obj)
					throws XMLStreamException {
				obj.setId(xml.getAttribute("id", obj.id));
				obj.setDelivery(xml.getAttribute("delivery", obj.delivery));
				obj.setType(xml.getAttribute("type", obj.type));
				obj.setBitrate(xml.getAttribute("bitrate", obj.bitrate));				
				obj.setMaintainAspectRatio(xml.getAttribute("maintainAspectRatio", obj.maintainAspectRatio));
				obj.setScalable(xml.getAttribute("scalable", obj.scalable));
				
				obj.setWidth(xml.getAttribute("width", obj.width));		
				obj.setHeight(xml.getAttribute("height", obj.height));
				obj.setURL(xml.getText().toString());
			}

		};

		public String getDelivery() {
			return delivery;
		}
		
		public String getId() {
			return id;
		}	
		
		public String getAsRatio() {
			return maintainAspectRatio;	
		}
		
		public String getScalable() {
			return scalable;	
		}
		
		public void setMaintainAspectRatio(String maintainAspectRatio) {
			this.maintainAspectRatio = maintainAspectRatio;
		}

		public void setScalable(String scalable) {
			this.scalable = scalable;
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
		
		public void setId(String id) {
			this.id = id;
		}
		
		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public String getBitrate() {
			return bitrate;
		}

		public void setBitrate(String bitrate) {
			this.bitrate = bitrate;
		}

		public String getURL() {
			return url;
		}

		public void setURL(String URL) {
			this.url = URL;
		}
	}

	public List<MediaFile> getMediaFiles() {
		return mediaFiles;
	}

	public void setMedaiFile(ArrayList<MediaFile> mediaFile) {
		this.mediaFiles = mediaFile;
	}
}