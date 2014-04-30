package edu.virginia.lib.ead.ingesters;

import edu.virginia.lib.ead.DataStore;
import edu.virginia.lib.ead.EADIngester;
import edu.virginia.lib.ead.Fedora3DataStore;
import edu.virginia.lib.ead.PidFilter;
import edu.virginia.lib.indexing.SolrIndexer;

import java.io.InputStream;

/**
 * Unfinished program to ingest/update the Church collection in a
 * Fedora 3 repository and index it to a solr server.
 *
 * In order for this program to run, conf/fedora.properties, conf/solr.properties
 * and viu00003-ammended.xml must exist on the classpath.
 */
public class ChurchIngester extends EADIngester {

    public static void main(String [] args) throws Exception {
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
}
