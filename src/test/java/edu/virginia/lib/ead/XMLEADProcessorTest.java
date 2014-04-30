package edu.virginia.lib.ead;

import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XMLEADProcessorTest {

    @Test
    public void testBasicVisitor() throws Exception {
        XMLEADProcessor p = new XMLEADProcessor();
        final TestEADNodeVisitor v = new TestEADNodeVisitor();
        p.addVisitor(v);
        p.processEADXML(getClass().getClassLoader().getResourceAsStream("test.xml"));

        Assert.assertEquals("There should be two parsed nodes.", 2, v.nodesInOrder.size());
        final EADNode first = v.nodesInOrder.get(0);
        Assert.assertEquals("series", first.getLevel());
        Assert.assertEquals("c01", first.getTagName());
        Assert.assertEquals("pid:0", first.getPid());
        Assert.assertEquals("<c01 level=\"series\">\n" +
                "        <unitid label=\"Digital Repository PID\">pid:0</unitid>\n" +
                "    </c01>", first.getXMLFragment());

        final EADNode second = v.nodesInOrder.get(1);
        Assert.assertEquals("collection", second.getLevel());
        Assert.assertEquals("ead", second.getTagName());
        Assert.assertNull(second.getPid());
        Assert.assertEquals("<ead>\n" +
                "    \n" +
                "</ead>", second.getXMLFragment());

    }

    private static class TestEADNodeVisitor implements EADNodeVisitor {

        private List<EADNode> nodesInOrder = new ArrayList<EADNode>();

        @Override
        public void init() throws Exception {
        }

        @Override
        public void visit(EADNode component) {
            nodesInOrder.add(component);
        }

        @Override
        public void finish() throws Exception {
        }
    }
}
