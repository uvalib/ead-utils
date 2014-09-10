package edu.virginia.lib.ead;

import edu.virginia.lib.ead.visitors.FedoraSynchronizingNodeVisitor;
import edu.virginia.lib.ead.visitors.NodeCountingNodeVisitor;
import edu.virginia.lib.ead.visitors.SolrIndexingNodeVisitor;
import edu.virginia.lib.ead.visitors.TimingNodeVisitor;
import edu.virginia.lib.indexing.SolrIndexer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * A class that ingests (or updates) the Reed finding aid
 * into Fedora.
 *
 * This class and program is written to support:
 *
 * a) repeated running with support for interruptability
 *    Since the program may take a while, it should be able
 *    to pick up where it left off.
 * b) running in report-only (dry-run) mode that makes no
 *    changes
 * c) adding references to the EAD (or having some external
 *    system) for nodes that don't have pids already
 *    assigned.
 * d) changes to the finding aid should result in a complete
 *    flush of components created by this script... no other
 *    approach is worth the effort, so this should be known
 *    and built around from the beginning. (NOT YET
 *    IMPLEMENTED)
 */
public abstract class EADIngester {

    private XMLEADProcessor p;

    protected DataStore dataStore;

    private ExternalPidResolver pids;

    public EADIngester(DataStore ds) throws Exception {
        p = new XMLEADProcessor();
        p.addVisitor(new TimingNodeVisitor(countNodes()));
        this.dataStore = ds;

        final URI uri = ds.getURI();
        final File pidCacheFile
                = new File( new File("pid-caches"), getFindingAidBriefName() + "-" + uri.getHost() + "-" + uri.getPort() + "-pid-cache.txt");
        pids = new ExternalPidResolver(pidCacheFile);
    }

    public List<String> getAllPids() {
        return new ArrayList<String>(pids.getAllPids());
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    private int countNodes() throws Exception {
        System.out.print("Counting nodes...");
        final XMLEADProcessor processor = new XMLEADProcessor();
        final NodeCountingNodeVisitor counter = new NodeCountingNodeVisitor();
        processor.addVisitor(counter);
        processor.processEADXML(getFindingAid());
        System.out.println(counter.getCount());
        return counter.getCount();
    }

    public void ingest(boolean dryRun) throws Exception {
        ingest(dryRun, null);
    }

    public void ingest(boolean dryRun, PidFilter filter) throws Exception {
        final FedoraSynchronizingNodeVisitor v = new FedoraSynchronizingNodeVisitor(dataStore, pids, dryRun);
        v.setFilter(filter);
        v.setHoldings(getHoldings());
        v.setImageMapper(getImageMapper());
        p.addVisitor(v);
        p.processEADXML(getFindingAid());
        p.removeVisitor(v);
    }

    public void index(SolrIndexer indexer, boolean clearCache) throws Exception {
        index(indexer, clearCache, null);
    }

    public void index(SolrIndexer indexer, boolean clearCache, PidFilter filter) throws Exception {
        final SolrIndexingNodeVisitor s = new SolrIndexingNodeVisitor(indexer, pids, clearCache);
        s.setFilter(filter);
        p.addVisitor(s);
        p.processEADXML(getFindingAid());
        p.removeVisitor(s);
    }

    protected abstract InputStream getFindingAid();

    protected abstract String getFindingAidBriefName();

    protected List<HoldingsInfo> getHoldings() {
        return Collections.emptyList();
    }

    protected abstract ImageMapper getImageMapper() throws Exception;

    /**
     * Gets the solr update url from the solr.properties file on the
     * class loader classpath.
     */
    protected static String getDefaultSolrUpdateUrl() throws IOException {
        Properties p = new Properties();
        p.load(EADIngester.class.getClassLoader().getResourceAsStream("conf/solr.properties"));
        return p.getProperty("solr-update-url");
    }

    protected static void replaceBrokenPids(final EADIngester i, final List<String> brokenPids, boolean reindex) throws Exception {
        for (String brokenPid : brokenPids) {
            i.getDataStore().purge(brokenPid);
        }
        PidFilter filter = new PidFilter() {
            @Override
            public boolean includePid(String pid) {
                return brokenPids.contains(pid);
            }
        };
        if (!brokenPids.isEmpty()) {
            i.ingest(false, filter);
        }

        if (reindex) {
            reindex(i, true, filter);
        }
    }

    protected static void reindex(final EADIngester i, boolean clearCache, PidFilter filter) throws Exception {
        final SolrIndexer indexer = new SolrIndexer(((Fedora3DataStore) i.getDataStore()).getFedoraClient(),
                getDefaultSolrUpdateUrl());
        i.index(indexer, clearCache, filter);
    }

}
