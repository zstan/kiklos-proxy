package target.eyes.vag.codec.xml.javolution.vast.v2.impl;

import java.util.ArrayList;
import java.util.List;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;
import target.eyes.vag.codec.xml.javolution.vast.impl.SingleValueTag;
import target.eyes.vag.codec.xml.javolution.vast.v2.impl.Impression;

public class Wrapper implements XMLSerializable {

	private static final long serialVersionUID = 3289937092011915149L;

	private SingleValueTag adSystem = new SingleValueTag("version");

	private VastAdTagURI vastAdTagURI = new VastAdTagURI();

	private List<Impression> impressions = new ArrayList<Impression>();

	private Creatives creatives = new Creatives();

	private Extensions extensions;

	protected static final XMLFormat<Wrapper> IN_LINE_XML = new XMLFormat<Wrapper>(
			Wrapper.class) {

		@Override
		public void write(Wrapper obj, OutputElement xml)
				throws XMLStreamException {
			xml.add(obj.adSystem, "AdSystem", SingleValueTag.class);
			/*
			 * if(obj.url != null) xml.getStreamWriter().writeCData(obj.url);
			 */

			xml.add(obj.vastAdTagURI, "VASTAdTagURI", VastAdTagURI.class);

			for (Impression i : obj.getImpressions()) {
				xml.add(i, "Impression", Impression.class);
			}

			xml.add(obj.creatives, "Creatives", Creatives.class);
			xml.add(obj.extensions, "Extensions", Extensions.class);
		}

		@Override
		public void read(InputElement xml, Wrapper obj)
				throws XMLStreamException {
			obj.adSystem = xml.get("AdSystem", SingleValueTag.class);
			obj.vastAdTagURI = xml.get("VASTAdTagURI", VastAdTagURI.class);

			obj.impressions.add(xml.get("Impression", Impression.class)); // at
																			// least
																			// one
																			// is
																			// required

			Impression i;
			while ((i = xml.get("Impression", Impression.class)) != null)
				obj.impressions.add(i);

			obj.setCreatives(xml.get("Creatives", Creatives.class));
			obj.setExtensions(xml.get("Extensions", Extensions.class));

		}

	};

	public String getAdSystem() {
		return adSystem.getText();
	}

	public void setAdSystem(String adSystem) {
		this.adSystem.setText(adSystem);
	}

	public String getVastAdTagURI() {
		return vastAdTagURI.getURI();
	}

	public void setAdSystemVersion(String version) {
		this.adSystem.setAttribute(version);
	}

	public void setVastAdTagURI(String vastAdTagURI) {
		this.vastAdTagURI.setUri(vastAdTagURI);
	}

	public List<Impression> getImpressions() {
		return impressions;
	}

	public Creatives getCreatives() {
		return creatives;
	}

	public void setCreatives(Creatives creatives) {
		this.creatives = creatives;
	}

	public Extensions getExtensions() {
		return extensions;
	}

	public void setExtensions(Extensions extensions) {
		this.extensions = extensions;
	}

}
