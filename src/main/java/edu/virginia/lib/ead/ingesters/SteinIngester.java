package edu.virginia.lib.ead.ingesters;

import edu.virginia.lib.ead.DataStore;
import edu.virginia.lib.ead.EADIngester;
import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.Fedora3DataStore;
import edu.virginia.lib.ead.HoldingsInfo;
import edu.virginia.lib.ead.ImageMapper;
import edu.virginia.lib.indexing.SolrIndexer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unfinished program to ingest/update the Gertrude Stein papers
 * in a Fedora 3 repository and index it to a solr server.
 *
 * In order for this program to run, conf/fedora.properties, conf/solr.properties,
 * viu00200-ammended.xml must exist on the classpath.
 */
public class SteinIngester extends EADIngester {

    public static void main(String [] args) throws Exception {
        final Fedora3DataStore datastore = new Fedora3DataStore();
        SteinIngester c = new SteinIngester(datastore);

        replaceBrokenPids(c, Arrays.asList(new String[]{ }), false);

        c.ingest(true);
        c.index(new SolrIndexer(datastore.getFedoraClient(), getDefaultSolrUpdateUrl()), false);
    }

    public SteinIngester(DataStore ds) throws Exception {
        super(ds);
    }

    @Override
    protected InputStream getFindingAid() {
        return getClass().getClassLoader().getResourceAsStream("viu00200-ammended.xml");
    }

    @Override
    protected String getFindingAidBriefName() {
        return "stein";
    }

    @Override
    protected ImageMapper getImageMapper() throws Exception {
        // no image mapping... yet.
        return null;
    }

    @Override
    protected List<HoldingsInfo> getHoldings() {
        List<HoldingsInfo> holdings = new ArrayList<HoldingsInfo>();
        holdings.add(new BoxFolderHoldingsInfo("MSS 8259, 8259-a, 8259-b", 1, "1966989", "X004985148"));
        holdings.add(new BoxFolderHoldingsInfo("MSS 8259, 8259-a, 8259-b", 2, "1966989", "X004985149"));
        return holdings;
    }

    private static class BoxFolderHoldingsInfo implements HoldingsInfo {

        private String barcode;

        private String catKey;

        private String mssNumber;

        private int boxNumber;

        public BoxFolderHoldingsInfo(String mssNumber, int boxNumber, String catKey, String barcode) {
            this.mssNumber = mssNumber;
            this.boxNumber = boxNumber;
            this.catKey = catKey;
            this.barcode = barcode;
        }

        @Override
        public boolean shouldBeLinkedToNode(EADNode node) {
            return node.getXMLFragment().contains("<container label=\"Box-folder\" type=\"box-folder\">" + boxNumber + ":");
        }

        @Override
        public String getMetadataForHolding() {
            return "<container>\n" +
                    "    <callNumber>" + getId() + "</callNumber>\n" +
                    "    <catalogKey>" + catKey + "</catalogKey>\n" +
                    "    <type>manuscript</type>\n" +
                    "    <barCode>" + barcode + "</barCode>\n" +
                    "</container>";
        }

        @Override
        public String getId() {
            return mssNumber + " Box " + boxNumber;
        }
    }
}
