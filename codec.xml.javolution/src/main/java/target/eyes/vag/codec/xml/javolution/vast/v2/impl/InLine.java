package target.eyes.vag.codec.xml.javolution.vast.v2.impl;

import java.util.ArrayList;
import java.util.List;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;
import target.eyes.vag.codec.xml.javolution.vast.impl.SingleValueTag;
import target.eyes.vag.codec.xml.javolution.vast.v2.impl.Impression;

public class InLine implements XMLSerializable {

	private static final long serialVersionUID = 3289937092011915149L;

	private SingleValueTag adSystem = new SingleValueTag("version");

	private SingleValueTag adTitle = new SingleValueTag();
	
	private SingleValueTag description = new SingleValueTag();
	
	private SingleValueTag error = new SingleValueTag();

	private List<Impression> impressions = new ArrayList<Impression>();

	private Creatives creatives = new Creatives();

	private Extensions extensions;

	protected static final XMLFormat<InLine> IN_LINE_XML = new XMLFormat<InLine>(
			InLine.class) {

		@Override
		public void write(InLine obj, OutputElement xml)
				throws XMLStreamException {
			xml.add(obj.adSystem, "AdSystem", SingleValueTag.class);
			xml.add(obj.description, "Description", SingleValueTag.class);
			xml.add(obj.error, "Error", SingleValueTag.class);
			xml.add(obj.adTitle, "AdTitle", SingleValueTag.class);
			for (Impression i : obj.getImpressions()) {
				xml.add(i, "Impression", Impression.class);
			}
			xml.add(obj.creatives, "Creatives", Creatives.class);
			xml.add(obj.extensions, "Extensions", Extensions.class);
		}

		@Override
		public void read(InputElement xml, InLine obj)
				throws XMLStreamException {
			obj.setAdSystem(xml.get("AdSystem", SingleValueTag.class).getText()
					.toString());
			obj.setAdTitle(xml.get("AdTitle", SingleValueTag.class).getText()
					.toString());
			obj.setDescription(xml.get("Description", SingleValueTag.class).getText()
					.toString());
			obj.setError(xml.get("Error", SingleValueTag.class).getText()
					.toString());			

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

	public String getAdTitle() {
		return adTitle.getText();
	}
	
	public String getDescription() {
		return description.getText();
	}	

	public String getError() {
		return error.getText();
	}		
	
	public void setAdSystemVersion(String version) {
		this.adSystem.setAttribute(version);
	}

	public void setAdTitle(String adTitle) {
		this.adTitle.setText(adTitle);
	}
	
	public void setDescription(String desc) {
		this.description.setText(desc);
	}

	public void setError(String desc) {
		this.error.setText(desc);
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
