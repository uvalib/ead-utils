package edu.virginia.lib.ead.ingesters;

import edu.virginia.lib.ead.EADIngester;
import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.Fedora3DataStore;
import edu.virginia.lib.ead.HoldingsInfo;
import edu.virginia.lib.ead.ImageMapper;
import edu.virginia.lib.ead.PidFilter;
import edu.virginia.lib.indexing.SolrIndexer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Unfinished program to ingest/update the Reed collection in a
 * Fedora 3 repository and index it to a solr server.
 *
 * In order for this program to run, conf/fedora.properties, conf/solr.properties
 * and viuh00010.xml must exist on the classpath.
 */
public class ReedIngester extends EADIngester {

    public static void main(String [] args) throws Exception {
        ReedIngester r = new ReedIngester(new Fedora3DataStore());

        // TODO: update this method to suit your ingest needs...
        r.ingest(true);
    }

    public ReedIngester(Fedora3DataStore fedora3DataStore) throws Exception {
        super(fedora3DataStore);
    }

    @Override
    protected InputStream getFindingAid() {
        return getClass().getClassLoader().getResourceAsStream("viuh00010.xml");
    }

    @Override
    protected String getFindingAidBriefName() {
        return "reed";
    }

    @Override
    protected List<HoldingsInfo> getHoldings() {
        return Arrays.asList(new HoldingsInfo[]{new HoldingsInfo() {
            @Override
            public boolean shouldBeLinkedToNode(EADNode node) {
                return "collection".equalsIgnoreCase(node.getLevel());
            }

            @Override
            public String getMetadataForHolding() {
                return "<container>\n" +
                        "    <callNumber>MS-1</callNumber>\n" +
                        "    <catalogKey>3653257</catalogKey>\n" +
                        "    <type>manuscript</type>\n" +
                        "    <barCode>3653257-1001</barCode>\n" +
                        "</container>";
            }

            @Override
            public String getId() {
                return "MS-1";
            }
        }});
    }

    @Override
    protected ImageMapper getImageMapper() throws Exception {
        // no image mapping... yet.
        return null;
    }
}
