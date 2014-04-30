package edu.virginia.lib.ead.visitors;

import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.EADNodeVisitor;
import edu.virginia.lib.ead.XMLEADProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * walk through a finding aid to identify all tags within
 * 1.  The header section
 * 2.  The components.
 */
public class FeatureAnalyzer implements EADNodeVisitor {

    public static void main(String [] args) throws Exception {
        FeatureAnalyzer f = new FeatureAnalyzer();
        XMLEADProcessor p = new XMLEADProcessor();
        p.addVisitor(f);
        p.addVisitor(new TimingNodeVisitor());
        p.processEADXML(FeatureAnalyzer.class.getClassLoader().getResourceAsStream(args[0]));
        f.listTags();
    }

    private void listTags() {
        ArrayList<String> tags = new ArrayList<String>(nodeTags);
        Collections.sort(tags);
        for (String tag : tags) {
            System.out.println(tag);
        }
    }

    private Set<String> nodeTags = new HashSet<String>();

    @Override
    public void init() throws Exception {
    }

    @Override
    public void visit(EADNode component) {
        // identify any tags
        final String xmlString = component.getXMLFragment();
        int index = xmlString.indexOf("<");
        while (index != -1) {
            final int closeIndex = xmlString.indexOf(">", index);
            for (String part : xmlString.substring(index + 1, closeIndex).split("\\b")) {
                System.out.println("\"" + part + "\"");
                if (part.trim().length() > 0) {
                    nodeTags.add(part.trim());
                    break;
                }

            }
            index = xmlString.indexOf("<", index + 1);
        }
    }

    @Override
    public void finish() throws Exception {

    }
}
