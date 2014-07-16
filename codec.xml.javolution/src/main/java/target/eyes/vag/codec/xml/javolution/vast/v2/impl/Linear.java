package target.eyes.vag.codec.xml.javolution.vast.v2.impl;

import javax.xml.stream.XMLStreamException;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import target.eyes.vag.codec.xml.javolution.vast.impl.SingleValueTag;
import target.eyes.vag.codec.xml.javolution.vast.v2.impl.MediaFiles;
import target.eyes.vag.codec.xml.javolution.vast.v2.impl.TrackingEvents;

public class Linear implements XMLSerializable {

	private SingleValueTag duration;
	
	private SingleValueTag adParameters;

	private TrackingEvents trackingEvents;

	private MediaFiles mediaFiles;

	private VideoClicks videoClicks;

	private static final long serialVersionUID = 1608498863436881757L;

	protected static final XMLFormat<Linear> LINEAR_XML = new XMLFormat<Linear>(
			Linear.class) {
		@Override
		public void write(Linear obj, XMLFormat.OutputElement xml)
				throws javolution.xml.stream.XMLStreamException {
			if (obj.duration != null)
				xml.add(obj.duration, "Duration", SingleValueTag.class);
			xml.add(obj.trackingEvents, "TrackingEvents", TrackingEvents.class);
			if (obj.adParameters != null)
				xml.add(obj.adParameters, "AdParameters", SingleValueTag.class);						
			xml.add(obj.videoClicks, "VideoClicks", VideoClicks.class);
			xml.add(obj.mediaFiles, "MediaFiles", MediaFiles.class);
		};

		@Override
		public void read(XMLFormat.InputElement xml, Linear obj)
				throws javolution.xml.stream.XMLStreamException {
			obj.setDuration(xml.get("Duration", SingleValueTag.class));

			obj.setTrackingEvents(xml.get("TrackingEvents",
					TrackingEvents.class));
			obj.setAdParameters(xml.get("AdParameters", SingleValueTag.class));			
			obj.setVideoClicks(xml.get("VideoClicks", VideoClicks.class));

			obj.setMediaFiles(xml.get("MediaFiles", MediaFiles.class));

		};
	};

	public String getDuration() {
		return duration.getText();
	}
	
	public String getAdParameters() {
		return adParameters.getText();
	}	

	public void setDuration(String duration) {
		this.duration = new SingleValueTag();
		this.duration.setText(duration);
	}

	public void setDuration(SingleValueTag duration) {
		if (duration != null)
			setDuration(duration.getText().toString());
	}
	
	public void setAdParameters(String adParameters) {
		this.adParameters = new SingleValueTag();
		this.adParameters.setText(adParameters);
	}	

	public void setAdParameters(SingleValueTag adParameters) {
		if (adParameters != null)
			setAdParameters(adParameters.getText().toString());
	}
	
	public TrackingEvents getTrackingEvents() {
		return trackingEvents;
	}

	public void setTrackingEvents(TrackingEvents trackingEvents) {
		this.trackingEvents = trackingEvents;
	}

	public MediaFiles getMediaFiles() {
		return mediaFiles;
	}

	public void setMediaFiles(MediaFiles mediaFiles) {
		this.mediaFiles = mediaFiles;
	}

	public VideoClicks getVideoClicks() {
		return videoClicks;
	}

	public void setVideoClicks(VideoClicks videoClicks) {
		this.videoClicks = videoClicks;
	}

}
