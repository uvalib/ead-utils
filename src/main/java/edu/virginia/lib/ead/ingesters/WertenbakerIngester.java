package edu.virginia.lib.ead.ingesters;

import edu.virginia.lib.ead.DataStore;
import edu.virginia.lib.ead.DefaultCollectionLevelHoldingsInfo;
import edu.virginia.lib.ead.EADIngester;
import edu.virginia.lib.ead.Fedora3DataStore;
import edu.virginia.lib.ead.HoldingsInfo;
import edu.virginia.lib.ead.ImageMapper;
import edu.virginia.lib.indexing.SolrIndexer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Unfinished program to ingest/update the Wertenbaker Letterbooks
 * in a Fedora 3 repository and index it to a solr server.
 *
 * In order for this program to run, conf/fedora.properties, conf/solr.properties,
 * viu00195-ammended.xml must exist on the classpath.
 */
public class WertenbakerIngester extends EADIngester {

    public static void main(String [] args) throws Exception {
        final Fedora3DataStore datastore = new Fedora3DataStore();
        WertenbakerIngester c = new WertenbakerIngester(datastore);

        replaceBrokenPids(c, Arrays.asList(new String[]{ }), false);

        c.ingest(true);
        c.index(new SolrIndexer(datastore.getFedoraClient(), getDefaultSolrUpdateUrl()), false);
    }

    public WertenbakerIngester(DataStore ds) throws Exception {
        super(ds);
    }

    @Override
    protected InputStream getFindingAid() {
        return getClass().getClassLoader().getResourceAsStream("viu00195-ammended.xml");
    }

    @Override
    protected String getFindingAidBriefName() {
        return "wertenbaker";
    }

    @Override
    protected ImageMapper getImageMapper() throws Exception {
        // no image mapping... yet.
        return null;
    }

    @Override
    protected List<HoldingsInfo> getHoldings() {
        return Arrays.asList(new HoldingsInfo[] { MSS3619BOX1, MSS3619BOX2, MSS3619MICROFILM });
    }

    private static HoldingsInfo MSS3619BOX1 = new DefaultCollectionLevelHoldingsInfo("MSS 3619 Box 1", "2262162", "manuscript", "X004988576");
    private static HoldingsInfo MSS3619BOX2 = new DefaultCollectionLevelHoldingsInfo("MSS 3619 Box 2", "2262162", "manuscript", "X004988577");
    private static HoldingsInfo MSS3619MICROFILM = new DefaultCollectionLevelHoldingsInfo("Microfilm 2377-2378", "2262162", "microfilm", "2262162-2001");

}
