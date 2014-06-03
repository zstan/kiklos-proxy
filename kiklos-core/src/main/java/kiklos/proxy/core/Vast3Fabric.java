package kiklos.proxy.core;

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
			e.printStackTrace();
		}
		return sw.toString();
	}
		
	public static String Vast2ListToVast3(final List<String> vastList) throws XMLStreamException {
		VAST3 vast3 = new VAST3();
		vast3.setVersion("3.0");
		vast3.setXmlns("http://www.w3.org/2001/XMLSchema-instance");
		vast3.setXsi("data/vast.xsd");
		LOG.debug("Vast2ListToVast3 vastList {}", vastList.size());
		
		List<Ad> adList = vast3.getAds();
		int i = 0;
		for (final String vast: vastList) {
			VAST3 v = VASTv2Parser.parseVast3(vast);
			Ad ad = v.getAds().get(0);
			ad.setSequence(Integer.toString(i++));
			adList.add(ad);			
		}
		return VastToString(vast3);
	}
}
