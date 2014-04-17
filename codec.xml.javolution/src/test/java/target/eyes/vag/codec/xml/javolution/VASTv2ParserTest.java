package target.eyes.vag.codec.xml.javolution;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import javolution.xml.stream.XMLStreamException;

import org.junit.Ignore;
import org.junit.Test;

import target.eyes.vag.codec.xml.javolution.VASTv2Parser;
import target.eyes.vag.codec.xml.javolution.vast.v2.impl.VAST;

public class VASTv2ParserTest {

	String arg0outv2ext =
			"<VAST version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"vast.xsd\">" +
			"<Ad id=\"5348167055596078337\">" +
			"<InLine>" +
			"<AdSystem version=\"2.0\">adnxs</AdSystem>" +
			"<AdTitle>VideoAd</AdTitle>" +
			"<Impression>" +
			"<![CDATA[http://fra1.ib.adnxs.com/it?enc=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAPA_AAAAAAAAAAAAAAAAAAAAAAFFQOyZgThKIPw5-QvcsFBGpUpTAAAAAL03JgD4BwAA-AcAAAIAAABCztkAOw4FAAAAAQBVU0QAVVNEAAEAAQCllgAAQo0BBAMCAQIAAIoAyBPyvAAAAAA.&cnd=%21QxzRSgiF3d0BEMKc5wYYACC7nBQwBDilrRZABEj4D1C975gBWABgwAFoAHAAeACAAQCIAQCQAQGYAQGgARKoAQOwAQC5AQAAAAAAAAAAwQEAAAAAAAAAAMkB6KRLB6cFAUDZAQAAAAAAAPA_4AEA9QEAAAAA&ccd=%21Swb_OwiF3d0BEMKc5wYYu5wUIAQ.&udj=uf%28%27a%27%2C+307401%2C+1397400902%29%3Buf%28%27r%27%2C+14274114%2C+1397400902%29%3B&vpid=962&custom_macro=CP_ID%5E3632773&media_subtypes=1&ct=17&dlo=1]]>" +
			"</Impression>" +
			"<Impression>" +
			"</Impression>" +
			"<Impression>" +
			"</Impression>" +
			"<Creatives><Creative id=\"14274114\" sequence=\"0\" AdID=\"5348167055596078337\">" +
			"<Linear><Duration>00:00:30</Duration>" +
			"<TrackingEvents>" +
			"<Tracking event=\"start\">" +
			"</Tracking>" +
			"<Tracking event=\"start\">" +
			"</Tracking>" +
			"<Tracking event=\"firstQuartile\">" +
			"</Tracking>" +
			"<Tracking event=\"firstQuartile\">" +
			"</Tracking>" +
			"<Tracking event=\"midpoint\">" +
			"</Tracking>" +
			"<Tracking event=\"midpoint\">" +
			"</Tracking>" +
			"<Tracking event=\"thirdQuartile\">" +
			"</Tracking>" +
			"<Tracking event=\"thirdQuartile\">" +
			"</Tracking>" +
			"<Tracking event=\"complete\">" +
			"</Tracking>" +
			"<Tracking event=\"complete\">" +
			"</Tracking>" +
			"</TrackingEvents>" +
			"<VideoClicks>" +
			"<ClickThrough>" +
			"<![CDATA[http://fra1.ib.adnxs.com/click?AAAAAAAAAAAAAAAAAAAAAAAAAAAAAPA_AAAAAAAAAAAAAAAAAAAAAAFFQOyZgThKIPw5-QvcsFBGpUpTAAAAAL03JgD4BwAA-AcAAAIAAABCztkAOw4FAAAAAQBVU0QAVVNEAAEAAQCllgAAQo0BBAMCAQIAAIoAyBPyvAAAAAA./cnd=%21Swb_OwiF3d0BEMKc5wYYu5wUIAQ./clickenc=http%3A%2F%2Fvi.ru]]>" +
			"</ClickThrough>" +
			"</VideoClicks>" +
			"<MediaFiles>" +
			"<MediaFile delivery=\"progressive\" type=\"video/mp4\" height=\"400\" width=\"720\" bitrate=\"1640\">" +
			"http://cdn.vidigital.ru/media/m/VGTRK/Wargaming/0404/imho_WOT_WGL_Finals_2014.mp4?___vi_and=14274114/3632773/ " +
			"</MediaFile>" +
			"</MediaFiles>" +
			"</Linear>" +
			"</Creative>" +
			"</Creatives>" +
			"<Extensions>" +
			"<Extension type=\"Urlmod\" scope=\"block\" method=\"append\" final=\"false\">" +
			"<urlpart>" +
			"<![CDATA[&k=14274114]]>" +
			"</urlpart>" +
			"</Extension>" +				
			"</Extensions>" +
			"</InLine>" +
			"</Ad>" +
			"</VAST>";
	
	
	@Test
	public void test() throws XMLStreamException {
		VAST v = VASTv2Parser.parse(arg0outv2ext);
		System.out.println(v.getAds().get(0).getInLine().getExtensions().getExtensions().size());
		//v.setXmlns(xmlns);
	}

	
/*	private static final String[] tests = { "adnxs.vast2.1", "adnxs.vast2.2", "adnxs.vast2.3", "adnxs.vast2.4" };
	
	@Ignore
	@Test
	public void test3() throws Exception {
		
		VASTv2Parser p = new VASTv2Parser();

		VASTv2 s = new VASTv2();
		
		for(String f: tests) {
			
			ApplicationResult r = ApplicationResult.createContent("", 
					ByteBuffer.wrap(ByteStreams.toByteArray(getClass().getResourceAsStream(f + ".in.xml"))));
			
			AdCollection ac =  p.apply(r);
			
			String result = ac.toString();
		
			System.out.println(result);
			
			String expected = new String(ByteStreams.toByteArray(getClass().getResourceAsStream(f + ".out.txt")), "UTF-8").trim();

			assertEquals(expected , result );
			
			String result2 = Charset.forName("UTF-8").decode(s.apply(ac).getData()).toString();
			
			System.out.println(result2);
			
			String expected2 = new String(ByteStreams.toByteArray(getClass().getResourceAsStream(f + ".out.xml")), "UTF-8").trim();

			assertEquals(expected2,result2);
		}
		
		
	}
	

	private static final String[] wrapperTests = { "wrapper.vast2.1", "wrapper.vast2.2" };
	
	
	@Ignore
	@Test
	public void wrapperTest() throws Exception {
		VASTv2Parser p = new VASTv2Parser();
		
		VASTv2 s = new VASTv2();
		
		for(String f: wrapperTests) {
			
			ApplicationResult r = ApplicationResult.createContent("", 
					ByteBuffer.wrap(ByteStreams.toByteArray(getClass().getResourceAsStream(f + ".in.xml"))));
			
			AdCollection ac =  p.apply(r);
			
			String result = ac.toString();
		
			System.out.println(result);
			
			String expected = new String(ByteStreams.toByteArray(getClass().getResourceAsStream(f + ".out.txt")), "UTF-8").trim();
			
			assertEquals(expected, result);
			
			String result2 = Charset.forName("UTF-8").decode(s.apply(ac).getData()).toString();
			
			System.out.println(result2);
			
			String expected2 = new String(ByteStreams.toByteArray(getClass().getResourceAsStream(f + ".out.xml")), "UTF-8").trim();

            assertEquals(expected2,result2);
		}
		
	}
*/}
