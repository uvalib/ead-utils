package edu.virginia.lib.ead.ingesters;

import edu.virginia.lib.ead.DataStore;
import edu.virginia.lib.ead.EADIngester;
import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.Fedora3DataStore;
import edu.virginia.lib.ead.HoldingsInfo;
import edu.virginia.lib.ead.ImageMapper;
import edu.virginia.lib.ead.imagemappers.RubyHashIdBasedImageMapper;
import edu.virginia.lib.indexing.SolrIndexer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unfinished program to ingest/update the Church collection in a
 * Fedora 3 repository and index it to a solr server.
 *
 * In order for this program to run, conf/fedora.properties, conf/solr.properties,
 * viu00003-ammended.xml and viu00003-digitized-item-mapping.txt must exist on the
 * classpath.
 */
public class ChurchIngester extends EADIngester {

    public static void main(String [] args) throws Exception {
        final Fedora3DataStore datastore = new Fedora3DataStore();
        ChurchIngester c = new ChurchIngester(datastore);

        replaceBrokenPids(c, Arrays.asList(new String[]{ }), false);

        c.ingest(true);
        c.index(new SolrIndexer(datastore.getFedoraClient(), getDefaultSolrUpdateUrl()), false);
    }

    public ChurchIngester(DataStore ds) throws Exception {
        super(ds);
    }

    @Override
    protected InputStream getFindingAid() {
        return getClass().getClassLoader().getResourceAsStream("viu00003-ammended.xml");
    }

    @Override
    protected String getFindingAidBriefName() {
        return "church";
    }

    @Override
    protected ImageMapper getImageMapper() throws Exception {
        return new RubyHashIdBasedImageMapper(getClass().getClassLoader().getResourceAsStream("viu00003-digitized-item-mapping.txt"));
    }

    @Override
    protected List<HoldingsInfo> getHoldings() {
        return Arrays.asList(new HoldingsInfo[] { MSS11245, MSS11245A });
    }

    private static HoldingsInfo MSS11245A = new HoldingsInfo() {
        @Override
        public boolean shouldBeLinkedToNode(EADNode node) {
            return node.getXMLFragment().contains("#11245-a");
        }

        @Override
        public String getMetadataForHolding() {
            return "<container>\n" +
                    "    <callNumber>MSS 11245-a</callNumber>\n" +
                    "    <catalogKey>4293731</catalogKey>\n" +
                    "    <type>manuscript</type>\n" +
                    "    <barCode>4293731-1001</barCode>\n" +
                    "</container>";
        }

        @Override
        public String getId() {
            return "MSS 11245-a";
        }
    };

    private static HoldingsInfo MSS11245 = new HoldingsInfo() {
        @Override
        public boolean shouldBeLinkedToNode(EADNode node) {
            return node.getLevel().equals("item") && !node.getXMLFragment().contains("#11245-a");
        }

        @Override
        public String getMetadataForHolding() {
            return "<container>\n" +
                    "    <callNumber>MSS 11245</callNumber>\n" +
                    "    <catalogKey>2525293</catalogKey>\n" +
                    "    <type>manuscript</type>\n" +
                    "    <barCode>X004985086</barCode>\n" +
                    "</container>";
        }

        @Override
        public String getId() {
            return "MSS 11245";
        }
    };
}
