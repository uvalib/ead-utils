package edu.virginia.lib.ead.visitors;

import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.EADNodeVisitor;
import edu.virginia.lib.ead.PidFilter;
import edu.virginia.lib.ead.ExternalPidResolver;
import edu.virginia.lib.indexing.IndexableObject;
import edu.virginia.lib.indexing.SolrIndexer;

import java.io.IOException;

public class SolrIndexingNodeVisitor implements EADNodeVisitor {

    private ExternalPidResolver pids;

    private SolrIndexer indexer;

    private boolean clearCache;

    private PidFilter filter;

    public SolrIndexingNodeVisitor(SolrIndexer indexer, ExternalPidResolver pids, boolean clearCache) {
        this.indexer = indexer;
        this.pids = pids;
        this.clearCache = clearCache;
    }

    public void setFilter(PidFilter f) {
        this.filter = f;
    }

    @Override
    public void init() {
    }

    @Override
    public void visit(EADNode component) throws IOException {
        final String pid = pids.getPidForNode(component);
        if (pid == null) {
            System.err.println("No PID for node " + component.getReferenceId() + "!");
        } else if (filter != null && !filter.includePid(pid, component)) {
            // skip this one
            System.out.println("Skipping " + pid + ".");
        } else {
            System.out.println("Indexing " + pid + ".");
            final IndexableObject o = indexer.getSolrIndexableObject(pid);
            if (this.clearCache || !o.hasCachedAddDoc()) {
                o.updateCachedAddDoc();
            }
            try {
                o.writeCachedRecordToSolr();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Attempt 2 in 5 seconds...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
                o.writeCachedRecordToSolr();
            }

        }
    }

    @Override
    public void finish() throws IOException {
        // commit
        indexer.commit();

        // optimize
        //indexer.optimize();
    }
}
