package kiklos.tv.timetable;

import java.io.StringWriter;
import java.util.List;

import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;
import target.eyes.vag.codec.xml.javolution.mast.impl.Condition;
import target.eyes.vag.codec.xml.javolution.mast.impl.MAST;
import target.eyes.vag.codec.xml.javolution.mast.impl.Source;
import target.eyes.vag.codec.xml.javolution.mast.impl.Sources;
import target.eyes.vag.codec.xml.javolution.mast.impl.StartConditions;
import target.eyes.vag.codec.xml.javolution.mast.impl.Trigger;
import target.eyes.vag.codec.xml.javolution.mast.impl.Triggers;

public class MastFabric {
	public static String createMastFromVastList(final List<String> vastList) {
		MAST m1 = new MAST();
		m1.setSchemaLocation("http://openvideoplayer.sf.net/mast");
		m1.setXmlns("http://openvideoplayer.sf.net/mast");
		m1.setXsi("http://www.w3.org/2001/XMLSchema-instance");
		Trigger tr1 = new Trigger();
		tr1.setDescription("test_preroll");
		tr1.setId("preroll");
		Triggers trgs = new Triggers();
		trgs.getTriggers().add(tr1);
		m1.setTriggers(trgs);
		Sources ss = new Sources();
		tr1.setSources(ss);
		StartConditions sCond = tr1.getStartConditions();
		Condition cond = new Condition();
		cond.setName("OnItemStart");
		cond.setType("event");
		sCond.getStartConditions().add(cond);
		
		
		for (final String vast: vastList) {
			Source s1 = new Source();
			s1.setFormat("vast");
			s1.setUri(vast);		
			ss.getSources().add(s1);		
		}
		
		StringWriter sw = new StringWriter();
		
		XMLObjectWriter ow = new XMLObjectWriter();
		try {
			ow.setOutput(sw);
			ow.write(m1, "MAST", MAST.class);
			ow.flush();			
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		return sw.toString();
	}
}
