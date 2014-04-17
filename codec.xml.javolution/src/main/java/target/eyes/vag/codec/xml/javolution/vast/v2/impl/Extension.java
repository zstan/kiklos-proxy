package target.eyes.vag.codec.xml.javolution.vast.v2.impl;

import javolution.xml.XMLFormat;
import javolution.xml.XMLSerializable;
import javolution.xml.stream.XMLStreamException;

import java.util.LinkedList;
import java.util.List;

/**
 * User: arevkov
 * Date: 4/15/13
 * Time: 2:46 PM
 */
public class Extension implements XMLSerializable {

    private static final long serialVersionUID = -988669439982964328L;

    private ExtensionType type;
    private String scope;
    private String method;
    private String ffinal;
    private List<Urlpart> urlparts = new LinkedList<>();
    private List<Tracking> trackings = new LinkedList<>();

    protected static final XMLFormat<Extension> TRACKING_EVENTS_XML = new XMLFormat<Extension>(Extension.class) {

        @Override
        public void write(Extension obj, OutputElement xml) throws XMLStreamException {
            // Write attributes
            xml.setAttribute("type", obj.type.name());
            xml.setAttribute("scope", obj.scope);
            xml.setAttribute("method", obj.method);
            xml.setAttribute("final", obj.ffinal);

            // Write nested tags
            for (Tracking t : obj.trackings) {
                xml.add(t, "Tracking", Tracking.class);
            }
            for (Urlpart u : obj.urlparts) {
                xml.add(u, "urlpart", Urlpart.class);
            }
        }

        @Override
        public void read(InputElement xml, Extension obj) throws XMLStreamException {
            // Read attributes
            switch (xml.getAttribute("type", null)) {
                case "CustomTracking":
                    obj.type = ExtensionType.CustomTracking; break;
                case "Urlmod":
                    obj.type = ExtensionType.Urlmod; break;
            }
            if (obj.type == null) return;
            obj.scope = xml.getAttribute("scope", null);
            obj.method = xml.getAttribute("method", null);
            obj.ffinal = xml.getAttribute("final", null);

            // Read nested tags
            while (xml.hasNext()) {
                switch (obj.type) {
                    case Urlmod:
                        Urlpart e = xml.get("urlpart", Urlpart.class);
                        // If not <urlpart> -> try to read & skip
                        if (e == null) xml.getNext();
                        else obj.urlparts.add(e);
                        break;
                    case CustomTracking:
                        Tracking e2 = xml.get("Tracking", Tracking.class);
                        // If not <tracking> -> try to read & skip
                        if (e2 == null) xml.getNext();
                        else obj.trackings.add(e2);
                        break;
                }
            }
        }
    };

    public Extension() {
        // do nothing
    }

    public Extension(ExtensionType type) {
        this.type = type;
    }

    public ExtensionType getType() {
        return type;
    }

    public void setType(ExtensionType type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<Urlpart> getUrlparts() {
        return urlparts;
    }

    public List<Tracking> getTrackings() {
        return trackings;
    }

    public static class Urlpart implements XMLSerializable {

        private static final long serialVersionUID = 1032795465170650598L;

        private String url;

        protected static final XMLFormat<Urlpart> URLPARTS_XML = new XMLFormat<Urlpart>(Urlpart.class) {

            @Override
            public void write(Urlpart obj, OutputElement xml)
                    throws XMLStreamException {
                xml.getStreamWriter().writeCData(obj.url);
            }

            @Override
            public void read(InputElement xml, Urlpart obj)
                    throws XMLStreamException {
                obj.setUrl(xml.getText().toString());
            }
        };

        public Urlpart() {
            // do nothing
        }

        public Urlpart(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class Tracking implements XMLSerializable {

        private static final long serialVersionUID = -5533438102380384577L;

        private String event;
        private String url;

        protected static final XMLFormat<Tracking> TRACKINGS_XML = new XMLFormat<Tracking>(Tracking.class) {

            @Override
            public void write(Tracking obj, OutputElement xml)
                    throws XMLStreamException {
                xml.setAttribute("event", obj.event.toString());
                xml.getStreamWriter().writeCData(obj.url);
            }

            @Override
            public void read(InputElement xml, Tracking obj)
                    throws XMLStreamException {
                obj.setEvent(xml.getAttribute("event", obj.event));
                obj.setURL(xml.getText().toString());
            }

        };

        public String getEvent() {
            return event;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public String getURL() {
            return url;
        }

        public void setURL(String URL) {
            this.url = URL;
        }

    }

    public static enum ExtensionType {
        Urlmod,
        CustomTracking
    }
}
