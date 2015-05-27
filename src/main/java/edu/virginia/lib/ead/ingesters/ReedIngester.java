package edu.virginia.lib.ead.ingesters;

import edu.virginia.lib.TracksysHacker;
import edu.virginia.lib.ead.DataStore;
import edu.virginia.lib.ead.EADIngester;
import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.EncodedTextMapper;
import edu.virginia.lib.ead.Fedora3DataStore;
import edu.virginia.lib.ead.Fedora4DataStore;
import edu.virginia.lib.ead.HoldingsInfo;
import edu.virginia.lib.ead.ImageMapper;
import edu.virginia.lib.ead.PidFilter;
import edu.virginia.lib.ead.VisibilityAssignment;
import edu.virginia.lib.ead.imagemappers.TracksysHackerImageMapper;
import edu.virginia.lib.ead.pidfilters.HasTeiPidFilter;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
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
        ingestToF3();
    }

    public static void ingestToF3() throws Exception {
        final Fedora3DataStore datastore = new Fedora3DataStore();
        ReedIngester r = new ReedIngester(datastore);
        r.ingest(true);
        //PrintWriter p = new PrintWriter(new FileOutputStream("reed-pids.txt"));
        //r.outputComponentPids(p);
        //HasTeiPidFilter teiReport = new HasTeiPidFilter(r.getTextMapper());
        //r.ingest(false, teiReport);
        //teiReport.writeReport(System.out);
        //r.ingest(false, new ProcessFromPidFilter("uva-lib:2229577"));
        //r.index(new SolrIndexer(datastore.getFedoraClient(), getDefaultSolrUpdateUrl()), true);
        //r.ingest(false);

    }

    public static void ingestToF4() throws Exception {
        DataStore datastore = new Fedora4DataStore("http://localhost:8080/rest", "reed");
        datastore.startTransaction();
        try {
            ReedIngester c = new ReedIngester(datastore);
            c.ingest(false);
            datastore.commitTransaction();
        } catch (Throwable t) {
            t.printStackTrace();
            datastore.rollBackTransaction();
        }
    }

    private ImageMapper imageMapper;

    private EncodedTextMapper textMapper;

    public ReedIngester(DataStore datastore) throws Exception {
        super(datastore);
        imageMapper = new TracksysHackerImageMapper(TracksysHacker.REED_COLLECTION_COMPONENT_ID);
        final File teiDir = new File("reed-tei");
        textMapper = new EncodedTextMapper() {

            private List<String> missing = new ArrayList<String>();
            @Override
            public File getTEIForNode(EADNode node) throws Exception {
                final String unitId = node.getUnitId();
                if (unitId != null && !unitId.trim().equals("")) {
                    File f = new File(teiDir, unitId + ".xml");
                    if (f.exists()) {
                        return f;
                    } else {
                        f = new File(teiDir, unitId + ".XML");
                        if (f.exists()) {
                            return f;
                        } else {
                            missing.add(unitId);
                            return null;
                        }
                    }
                }
                return null;
            }

            @Override
            public File[] getAllTeiFiles() {
                return teiDir.listFiles();
            }

            @Override
            public List<String> getAllUnfoundIds() {
                return missing;
            }
        };
    }

    @Override
    protected VisibilityAssignment getVisibilityAssignment() {
        return VisibilityAssignment.COLLECTION_ONLY;
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
        return imageMapper;
    }

    @Override
    protected EncodedTextMapper getTextMapper() throws Exception {
        return textMapper;
    }
}
