package edu.virginia.lib.ead.imagemappers;

import edu.virginia.lib.ead.EADNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class RubyHashIdBasedImageMapperTest {

    @Test
    public void testBasicReading() throws IOException {
        RubyHashIdBasedImageMapper m = new RubyHashIdBasedImageMapper(RubyHashIdBasedImageMapperTest.class.getClassLoader().getResourceAsStream("test-mapping.txt"));

        Assert.assertEquals("Value should be parsed!", "test:c1", m.getExemplarPid(new DummyNode("c")));
        Assert.assertNull("Value shouldn't exist!", m.getImagePids(new DummyNode("fake")));
        Assert.assertTrue("D mapping should have been parsed!", Arrays.asList(new String[]{ "test:d1", "test:d2", "test:d3", "test:d4"}).equals(m.getImagePids(new DummyNode("d"))));
        Assert.assertTrue("F mapping should have been parsed!", Arrays.asList(new String[]{ "test:f1", "test:f2", "test:f3", "test:f4", "test:f5"}).equals(m.getImagePids(new DummyNode("f"))));
    }

    private static class DummyNode implements EADNode {

        private String refId;

        public DummyNode(String id) {
            this.refId = id;
        }

        @Override
        public String getTagName() {
            return null;
        }

        @Override
        public String getLevel() {
            return null;
        }

        @Override
        public String getPid() {
            return null;
        }

        @Override
        public String getXMLFragment() {
            return null;
        }

        @Override
        public String getReferenceId() {
            return refId;
        }

        @Override
        public List<String> getChildReferenceIds() {
            return null;
        }
    }
}
