package edu.virginia.lib.ead.ingesters;

import edu.virginia.lib.ead.DataStore;
import edu.virginia.lib.ead.EADIngester;
import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.EncodedTextMapper;
import edu.virginia.lib.ead.Fedora3DataStore;
import edu.virginia.lib.ead.HoldingsInfo;
import edu.virginia.lib.ead.ImageMapper;
import edu.virginia.lib.ead.VisibilityAssignment;
import edu.virginia.lib.ead.imagemappers.RubyHashIdBasedImageMapper;
import edu.virginia.lib.indexing.SolrIndexer;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unfinished program to ingest/update the Stewart Baldwin Collection
 * in a Fedora 3 repository and index it to a solr server.
 *
 * In order for this program to run, conf/fedora.properties, conf/solr.properties,
 * viu00193-ammended.xml must exist on the classpath.
 */
public class StuartBaldwin extends EADIngester {

    public static void main(String [] args) throws Exception {
        final Fedora3DataStore datastore = new Fedora3DataStore();
        StuartBaldwin c = new StuartBaldwin(datastore);

        replaceBrokenPids(c, Arrays.asList(new String[]{ }), false);

        //c.ingest(true);
        c.index(new SolrIndexer(datastore.getFedoraClient(), getDefaultSolrUpdateUrl()), false);
    }

    public StuartBaldwin(DataStore ds) throws Exception {
        super(ds);
    }

    @Override
    protected VisibilityAssignment getVisibilityAssignment() {
        return VisibilityAssignment.COLLECTION_ONLY;
    }

    @Override
    protected InputStream getFindingAid() {
        return getClass().getClassLoader().getResourceAsStream("viu00193-ammended.xml");
    }

    @Override
    protected String getFindingAidBriefName() {
        return "stuart-baldwin";
    }

    @Override
    protected ImageMapper getImageMapper() throws Exception {
        // no image mapping... yet.
        return null;
    }

    @Override
    protected EncodedTextMapper getTextMapper() throws Exception {
        return null;
    }

    @Override
    protected List<HoldingsInfo> getHoldings() {
        List<HoldingsInfo> holdings = new ArrayList<HoldingsInfo>();
        holdings.add(new BoxHoldingsInfo("MSS 228", 1, "2791023", "X004934688"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 2, "2791023", "X004934689"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 3, "2791023", "X004934690"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 4, "2791023", "X004934691"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 5, "2791023", "X004934692"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 6, "2791023", "X004934693"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 7, "2791023", "X004934694"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 8, "2791023", "X004934695"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 9, "2791023", "X004934696"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 10, "2791023", "X004934697"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 11, "2791023", "X004934698"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 12, "2791023", "X004934699"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 13, "2791023", "X004934700"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 14, "2791023", "X004934701"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 15, "2791023", "X004934702"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 16, "2791023", "X004934703"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 17, "2791023", "X004934704"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 18, "2791023", "X004934705"));
        holdings.add(new BoxHoldingsInfo("MSS 228", 19, "2791023", "X004934706"));
        return holdings;
    }

    private static class BoxHoldingsInfo implements HoldingsInfo {

        private String barcode;

        private String catKey;

        private String mssNumber;

        private int boxNumber;

        public BoxHoldingsInfo(String mssNumber, int boxNumber, String catKey, String barcode) {
            this.mssNumber = mssNumber;
            this.boxNumber = boxNumber;
            this.catKey = catKey;
            this.barcode = barcode;
        }

        @Override
        public boolean shouldBeLinkedToNode(EADNode node) {
            //if (node.getXMLFragment().contains("<unittitle label=\"Series\">Series I:")) {
                // box 1-8 in series 1
            //    return boxNumber >=0 && boxNumber <= 8;
            //} else if (node.getXMLFragment().contains("<unittitle label=\"Series\">Series II:")) {
                // box 9-19 in series 2
            //    return boxNumber >=9 && boxNumber <= 19;
            //} else {
                return node.getXMLFragment().contains("<container label=\"Box\" type=\"Box\">" + boxNumber + "</container>");
            //}
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
