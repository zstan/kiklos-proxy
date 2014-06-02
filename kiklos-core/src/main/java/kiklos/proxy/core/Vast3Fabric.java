package kiklos.proxy.core;

import java.io.StringWriter;

import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;
import target.eyes.vag.codec.xml.javolution.VASTv2Parser;
import target.eyes.vag.codec.xml.javolution.vast.v3.impl.VAST3;

public class Vast3Fabric {
	public static String Vast2String(final String vast) {
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
}
