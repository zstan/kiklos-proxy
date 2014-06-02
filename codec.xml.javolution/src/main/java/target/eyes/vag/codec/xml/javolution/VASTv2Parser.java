package target.eyes.vag.codec.xml.javolution;

import javolution.xml.XMLObjectReader;
import javolution.xml.stream.XMLStreamException;
import target.eyes.vag.codec.xml.javolution.mast.impl.MAST;
import target.eyes.vag.codec.xml.javolution.vast.v2.impl.*;
import target.eyes.vag.codec.xml.javolution.vast.v2.impl.MediaFiles.MediaFile;
import target.eyes.vag.codec.xml.javolution.vast.v2.impl.TrackingEvents.Tracking;
import target.eyes.vag.codec.xml.javolution.vast.v3.impl.VAST3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class VASTv2Parser {

	private static Logger logger = LoggerFactory.getLogger(VASTv2Parser.class);

	public static VAST parse(InputStream input) throws XMLStreamException {
		XMLObjectReader reader;
		try {
			reader = XMLObjectReader.newInstance(input);
			VAST template = reader.read("VAST", VAST.class);
			reader.close();
			return template;
		} catch (XMLStreamException e) {
			logger.error(e.getMessage());
			throw new RuntimeException(VASTv2Parser.class.getSimpleName()
					+ ": unable to parse", e);
		}
	}

	public static VAST parse(String s) throws XMLStreamException {
		XMLObjectReader reader;
		try {
			reader = XMLObjectReader.newInstance(new StringReader(s));
			VAST template = reader.read("VAST", VAST.class);
			reader.close();
			return template;
		} catch (XMLStreamException e) {
			logger.error(e.getMessage());
			throw new RuntimeException(VASTv2Parser.class.getSimpleName()
					+ ": unable to parse", e);
		}
	}
	
	public static MAST parseMast(String s) throws XMLStreamException {
		XMLObjectReader reader;
		//try {
			reader = XMLObjectReader.newInstance(new StringReader(s));
			MAST template = reader.read("MAST", MAST.class);
			reader.close();
			return template;
		/*} catch (XMLStreamException e) {
			System.out.println(e.getMessage());
			logger.error(e.getMessage());
			throw new RuntimeException(VASTv2Parser.class.getSimpleName()
					+ ": unable to parse", e);
		}*/
	}
	
	public static VAST3 parseVast3(String s) throws XMLStreamException {
		XMLObjectReader reader;
		//try {
			reader = XMLObjectReader.newInstance(new StringReader(s));
			VAST3 template = reader.read("VAST", VAST3.class);
			reader.close();
			return template;
		/*} catch (XMLStreamException e) {
			System.out.println(e.getMessage());
			logger.error(e.getMessage());
			throw new RuntimeException(VASTv2Parser.class.getSimpleName()
					+ ": unable to parse", e);
		}*/
	}	
	

	private static class Context {
		private VAST vast;

		private Context(VAST vast) {
			this.vast = vast;
		}

		private List<String> impressions(List<Impression> simps) {
			List<String> imps = new ArrayList<String>();

			if (simps != null)
				for (Impression i : simps)
					if (!i.getUrl().isEmpty())
						imps.add(i.getUrl());

			return imps;
		}

		private List<String> clickTracking(List<VideoClicks.ClickURL> ctl) {
			List<String> rv = null;

			if (ctl != null) {
				rv = new ArrayList<String>();
				for (VideoClicks.ClickURL i : ctl)
					if (!i.getURL().isEmpty())
						rv.add(i.getURL());
			}

			return rv;
		}
	}
}
