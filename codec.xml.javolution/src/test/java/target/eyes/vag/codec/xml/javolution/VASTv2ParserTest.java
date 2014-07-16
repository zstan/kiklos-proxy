package target.eyes.vag.codec.xml.javolution;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.junit.Ignore;
import org.junit.Test;

import target.eyes.vag.codec.xml.javolution.VASTv2Parser;
import target.eyes.vag.codec.xml.javolution.mast.impl.MAST;
import target.eyes.vag.codec.xml.javolution.mast.impl.Source;
import target.eyes.vag.codec.xml.javolution.mast.impl.Sources;
import target.eyes.vag.codec.xml.javolution.mast.impl.Trigger;
import target.eyes.vag.codec.xml.javolution.mast.impl.Triggers;
import target.eyes.vag.codec.xml.javolution.vast.v2.impl.VAST;
import target.eyes.vag.codec.xml.javolution.vast.v3.impl.VAST3;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

public class VASTv2ParserTest {

	String arg0outv2ext =
			"<VAST version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"vast.xsd\">" +
			"<Ad id=\"5348167055596078337\">" +
			"<InLine>" +
			"<Description>video ad</Description>" +
			"<Error>http://ads.adfox.ru/216891/event?p2=euhw&p1=blhhs&p5=cftnj&pr=cxcxlnm&lts=eohriex&pm=u</Error>" +
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
	
/*	String mastV1 = 	
"<MAST xsi:schemaLocation=\"http://openvideoplayer.sf.net/mast\" xmlns=\"http://openvideoplayer.sf.net/mast\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
  "<triggers>" + 
		 "<trigger id=\"preroll\" description=\"preroll\">"+
		 "</trigger>"+
	"</triggers>" +
	"</MAST>";*/
	
	String mastV1 = 	
"<MAST xsi:schemaLocation=\"http://openvideoplayer.sf.net/mast\" xmlns=\"http://openvideoplayer.sf.net/mast\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
		"<triggers>" + 
		 "<trigger id=\"preroll\" description=\"preroll\">" +
		  "<startConditions>" +
		   "<condition type=\"event1\" name=\"OnItemStart\" />" +
		   "<condition type=\"event2\" name=\"OnItemStart\" />" +
		  "</startConditions>" +
		  "<sources>" +
		    "<source uri=\"http://api.atdmt.com/sf=VAST_PreRoll_XML_V2;\" format=\"vast\">" +
		    "</source>" +
		  "</sources>" +
		 "</trigger>" +
		"</triggers>" +
	"</MAST>";	
	
	
	@Test
	public void test() throws XMLStreamException {
		//VAST v = VASTv2Parser.parse(arg0outv2ext);
		VAST3 v = VASTv2Parser.parseVast3(arg0outv2ext);
		System.out.println(v.getAds().get(0).getInLine().getExtensions().getExtensions().size());
		v.getAds().get(0).setSequence("0");
		System.out.println(v.getAds().get(0).getSequence());
		
		MAST m = VASTv2Parser.parseMast(mastV1);
		System.out.println(m.getTriggers().getTriggers().get(0).getDescription());
		System.out.println(m.getTriggers().getTriggers().get(0).getStartConditions().getStartConditions().get(0).getType());
		System.out.println(m.getTriggers().getTriggers().get(0).getStartConditions().getStartConditions().get(1).getType());
		
		MAST m1 = new MAST();
		m1.setSchemaLocation("http://openvideoplayer.sf.net/mast");
		Trigger tr1 = new Trigger();
		tr1.setDescription("jopa");
		tr1.setId("preroll");
		Triggers trgs = new Triggers();
		trgs.getTriggers().add(tr1);
		m1.setTriggers(trgs);
		Source s1 = new Source();
		s1.setFormat("vast");
		s1.setUri("www.au.ru");
		Sources ss = new Sources();
		ss.getSources().add(s1);
		tr1.setSources(ss);
		
		System.out.println(m1.getTriggers().getTriggers().get(0).getDescription());
		XMLObjectWriter ow = new XMLObjectWriter();
		ow.setOutput(System.out);
		ow.write(m1, "MAST", MAST.class);
		ow.flush();
	}
	
	@Test
	public void testUri() throws URISyntaxException {
		String u = "http://asg.vidigital.ru/1/50006/c/v/2";
		URI uri = new URI(u);
		System.out.println("!!!!" + uri.getPath());
	}

	@Test
	public void testVast3WithSeq() throws URISyntaxException {
		InputStream in = getClass().getResourceAsStream("vast3_with_sequence.xml");
		try {
			String content = CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));
			VASTv2Parser.parseVast3(content);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
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
