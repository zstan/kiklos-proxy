package kiklos.tv.timetable;

import java.io.StringWriter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;
import target.eyes.vag.codec.xml.javolution.VASTv2Parser;
import target.eyes.vag.codec.xml.javolution.vast.v3.impl.Ad;
import target.eyes.vag.codec.xml.javolution.vast.v3.impl.VAST3;

public class Vast3Fabric {
	private static final Logger LOG = LoggerFactory.getLogger(Vast3Fabric.class);
	private static final String emptyFirst5SecVast = "<VAST version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://specs.adfox.ru/uploads/vast.xsd\">"+
			"<Ad id=\"0\">"+
			"<InLine>"+
			"<AdSystem>AdFox.Ru</AdSystem>"+
			"<AdTitle>None</AdTitle>"+
			"<Description>5 sec stub</Description>"+
			"<Error/><Impression/>"+
			"<Creatives>"+
			"<Creative>"+
			"<Linear>"+
			"<Duration>"+
			"00:00:05"+
			"</Duration>"+
			"<TrackingEvents/><AdParameters/><VideoClicks/>"+
			"<MediaFiles>"+
			"<MediaFile id=\"1\" delivery=\"progressive\" type=\"application/x-mpegurl\" bitrate=\"\" maintainAspectRatio=\"false\" scalable=\"true\" width=\"720\" height=\"576\">"+
			"<![CDATA[http://mostmediaonline.com/test/evergreen/408_H0STA05.m3u8?rnd=12345678]]>"+
			"</MediaFile>"+
			"</MediaFiles>"+
			"</Linear>"+
			"</Creative>"+
			"</Creatives>"+
			"<Extensions/>"+
			"</InLine>"+
			"</Ad>"+
			"</VAST>";
	
	public static String VastToString(final String vast) {
		VAST3 v;
		try {
			v = VASTv2Parser.parseVast3(vast);
		} catch (XMLStreamException e1) {
			e1.printStackTrace();
			return "";
		}
		
		StringWriter sw = new StringWriter();
		
		XMLObjectWriter ow = new XMLObjectWriter();
		try {
			ow.setOutput(sw);
			ow.write(v, "VAST", VAST3.class);
			ow.flush();			
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		return sw.toString();
	}
	
	public static String VastToString(final VAST3 v) {
		StringWriter sw = new StringWriter();
		
		XMLObjectWriter ow = new XMLObjectWriter();
		try {
			ow.setOutput(sw);
			ow.write(v, "VAST", VAST3.class);
			ow.flush();			
		} catch (XMLStreamException e) {
			LOG.debug("VastToString " + e.getMessage());
		}
		return sw.toString();
	}
		
	public static String Vast2ListToVast3(final List<String> vastList) {
		VAST3 vast3 = new VAST3();
		vast3.setVersion("3.0");
		vast3.setXmlns("http://www.w3.org/2001/XMLSchema-instance");
		vast3.setXsi("vast.xsd");
		LOG.debug("Vast2ListToVast3 vastList {}", vastList.size());
		
		if (!vastList.isEmpty()) {
			vastList.set(0, emptyFirst5SecVast);
		}
		
		List<Ad> adList = vast3.getAds();
		int i = 0;
		for (final String vast: vastList) {
			VAST3 v;
			Ad ad = null;
			try {
				LOG.debug("parseVast3 begin");

				v = VASTv2Parser.parseVast3(vast);

				if (v == null) 
					throw new XMLStreamException("parseVast3 Exception");

				List<Ad> ads = v.getAds();

				LOG.debug("parseVast3 end, ads size: {}", ads.size());

				if (!ads.isEmpty()) {
					ad = ads.get(0);
					ad.setSequence(Integer.toString(i++));
				}
			} catch (XMLStreamException | IndexOutOfBoundsException e) {
				LOG.debug("Vast2ListToVast3 exception: " + e.getMessage()); //put zaglushka here !!!!
			}
			
			if (ad != null)
				adList.add(ad);			
		}
		LOG.debug("Vast2ListToVast3 adList {}", adList.size());
		LOG.debug("Vast2ListToVast3 raw: {}", VastToString(vast3));
		return VastToString(vast3);
	}
}
