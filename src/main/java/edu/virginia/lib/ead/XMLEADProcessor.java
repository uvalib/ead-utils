package edu.virginia.lib.ead;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * A class that processes an EAD, allowing any number
 * of registered EADNodeVisitor implementations to be
 * triggered for each node in an XML finding aid.
 */
public class XMLEADProcessor {

    private List<EADNodeVisitor> visitors;

    public XMLEADProcessor() {
        this.visitors = new ArrayList<EADNodeVisitor>();
    }

    public void addVisitor(EADNodeVisitor visitor) {
        this.visitors.add(visitor);
    }

    public void processEADXML(InputStream xmlInputStream) throws Exception {
        for (EADNodeVisitor v : visitors) {
            v.init();
        }
        Stack<StartEADNode> path = new Stack<StartEADNode>();
        int i = 0;

        final XMLInputFactory factory = XMLInputFactory.newFactory();
        final XMLEventReader reader = factory.createXMLEventReader(xmlInputStream);
        while (reader.hasNext()) {
            final XMLEvent event = reader.nextEvent();
            if (XMLStreamConstants.START_ELEMENT == event.getEventType()) {
                final StartElement start = event.asStartElement();
                if (isNode(start)) {
                    String referenceId = (path.isEmpty()) ? String.valueOf(i ++) : path.peek().getReferenceId() + "-" + String.valueOf(i ++);
                    if (start.getAttributeByName(new QName("", "id")) != null) {
                        referenceId = start.getAttributeByName(new QName("", "id")).getValue();
                    }
                    final StartEADNode node = new StartEADNode(start, referenceId);
                    if (!path.isEmpty()) {
                        path.peek().appendChildId(node.getReferenceId());
                    }
                    path.push(node);
                    node.copy.add(event);
                } else if (isPid(start)) {
                    path.peek().copy.add(event);
                    path.peek().pid = getNodeValueNode(reader, start, path.peek().copy);
                } else if (isInclude(start)) {
                    include(start, path.peek().copy, reader);
                } else {
                    path.peek().copy.add(event);
                }

            } else if (XMLStreamConstants.END_ELEMENT == event.getEventType()) {
                if (path.peek().start.getName().equals(event.asEndElement().getName())) {
                    path.peek().copy.add(event);
                    path.peek().copy.close();
                    visit(path.pop());
                } else {
                    path.peek().copy.add(event);
                }
            } else {
                if (!path.isEmpty()) {
                    path.peek().copy.add(event);
                }
            }
        }
        for (EADNodeVisitor v : visitors) {
            v.finish();
        }

    }

    private boolean isInclude(StartElement start) {
        return (start.getName().getNamespaceURI().equals("http://www.w3.org/2001/XInclude") && start.getName().getLocalPart().equals("include"));
    }

    private void include(StartElement includeStartElement, XMLEventWriter writer, XMLEventReader originalReader) throws IOException, XMLStreamException {
        String url = includeStartElement.getAttributeByName(new QName("href")).getValue();
        final XMLInputFactory factory = XMLInputFactory.newFactory();
        //factory.set
        final XMLEventReader reader = factory.createXMLEventReader(new URL(url).openStream());
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (!(event.isStartDocument() || event.isEndDocument())) {
                writer.add(event);
            }
        }
        reader.close();
        while (!originalReader.nextEvent().isEndElement()) {
            System.out.println("Skipping over include statement...");
        }
    }

    private static boolean isPid(final StartElement start) {
        final Attribute label =  start.getAttributeByName(QName.valueOf("label"));
        return label != null && label.getValue().equals("Digital Repository PID");
    }

    private static String getNodeValueNode(final XMLEventReader reader, final StartElement start, final XMLEventWriter copy) throws XMLStreamException {
        final StringBuffer text = new StringBuffer();
        while (reader.hasNext()) {
            final XMLEvent event = reader.nextEvent();
            copy.add(event);
            if (event.getEventType() == XMLStreamConstants.END_ELEMENT) {
                if (event.asEndElement().getName().equals(start.getName())) {
                    break;
                }
            } else if (event.getEventType() == XMLStreamConstants.CHARACTERS) {
                text.append(event.asCharacters().getData());
            }
        }
        return text.toString();
    }

    private void visit(final EADNode node) throws Exception {
        for (EADNodeVisitor visitor : visitors) {
            visitor.visit(node);
        }
    }

    private static boolean isNode(final StartElement el) {
        final String elName = el.getName().getLocalPart();
        return elName.equals("ead") || Pattern.matches("c\\d\\d", elName) || elName.equals("c");
    }

    public void removeVisitor(EADNodeVisitor v) {
        this.visitors.remove(v);
    }

    private static class StartEADNode implements EADNode {

        private StartElement start;

        private String pid;

        private XMLEventWriter copy;

        private ByteArrayOutputStream fragment;

        private String referenceId;

        private List<String> childrenIds;

        public StartEADNode(StartElement start, String id) throws XMLStreamException {
            this.start = start;
            fragment = new ByteArrayOutputStream();
            copy = XMLOutputFactory.newFactory().createXMLEventWriter(fragment);
            referenceId = id;
            childrenIds = new ArrayList<String>();
        }

        private void appendChildId(String id) {
            this.childrenIds.add(id);
        }

        @Override
        public String getTagName() {
            return start.getName().getLocalPart();
        }


        @Override
        public String getLevel() {
            if (start.getName().getLocalPart().equals("ead")) {
                return "collection";
            } else {
                Attribute a = start.getAttributeByName(QName.valueOf("level"));
                if (a != null) {
                    return a.getValue();
                }
            }
            return null;
        }

        @Override
        public String getPid() {
            return pid;
        }

        @Override
        public String getXMLFragment() {
            try {
                return new String(fragment.toByteArray(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getReferenceId() {
            return this.referenceId;
        }

        @Override
        public List<String> getChildReferenceIds() {
            return this.childrenIds;
        }

    }

}
