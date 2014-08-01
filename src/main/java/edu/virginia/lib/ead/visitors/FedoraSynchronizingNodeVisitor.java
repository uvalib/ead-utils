package edu.virginia.lib.ead.visitors;

import edu.virginia.lib.ead.DataStore;
import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.EADNodeVisitor;
import edu.virginia.lib.ead.HoldingsInfo;
import edu.virginia.lib.ead.ImageMapper;
import edu.virginia.lib.ead.PidFilter;
import edu.virginia.lib.ead.ExternalPidResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FedoraSynchronizingNodeVisitor implements EADNodeVisitor {

    private DataStore data;
    
    private boolean dryRun;

    private ExternalPidResolver pids;

    private PidFilter filter;

    private List<HoldingsInfo> holdings;

    private ImageMapper imageMapper;

    public FedoraSynchronizingNodeVisitor(DataStore ds, ExternalPidResolver pids, boolean dryRun) throws IOException {
        this.data = ds;
        this.pids = pids;
        this.dryRun = dryRun;
        this.holdings = new ArrayList<HoldingsInfo>();
    }

    public void setHoldings(List<HoldingsInfo> h) {
        this.holdings = h;
    }

    public void setImageMapper(ImageMapper m) {
        this.imageMapper = m;
    }


    public void setFilter(PidFilter f) {
        this.filter = f;
    }

    @Override
    public void init() throws Exception {
        if (!dryRun) {
            for (HoldingsInfo h : holdings) {
                data.addOrReplaceHolding(getPid(h), h);
            }
        }
    }

    public void visit(EADNode component) {
        try {
            // find or create a PID
            String pid = getPid(component);
            if (!dryRun) {
                // update metadata
                if (updatePid(pid)) {
                    data.addOrReplaceEADFragment(pid, component);
                }

                // update relationships
                String previousPid = null;
                for (String childId : component.getChildReferenceIds()) {
                    final String childPid = pids.getPidForNodeReferenceId(childId);
                    if (updatePid(pid, childPid)) {
                        data.setChildRelationship(pid, childPid);
                    }
                    if (previousPid != null) {
                        if (updatePid(previousPid, childPid)) {
                            data.setSequenceRelationship(previousPid, childPid);
                        }
                    }
                    previousPid = childPid;
                }

                // add holdings if available
                addHoldings(pid, component);

                // add images if available
                linkImages(pid, component);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void linkImages(String pid, EADNode component) throws Exception {
        if (imageMapper == null) {
            return;
        }
        final List<String> imagePids = imageMapper.getImagePids(component);
        if (imagePids != null && !imagePids.isEmpty()) {
            for (String imagePid : imagePids) {
                if (updatePid(pid, imagePid)) {
                    data.setHasImageRelationship(pid, imagePid);
                }
            }
            final String exemplarPid = imageMapper.getExemplarPid(component);
            if (updatePid(pid, exemplarPid)) {
                data.setHasExemplarImageRelationship(pid, exemplarPid);
            }
        }
    }

    private void addHoldings(String pid, EADNode component) throws Exception {
        for (HoldingsInfo h : this.holdings) {
            if (h.shouldBeLinkedToNode(component)) {
                if (updatePid(pid)) {
                    data.setHoldingsRelationship(pid, getPid(h));
                }
            }
        }
    }

    private boolean updatePid(String ... pids) {
        if (filter == null) {
            return true;
        }
        for (String pid : pids) {
            if (filter.includePid(pid)) return true;
        }
        return false;
    }

    @Override
    public void finish() {
    }

    /**
     * Gets a PID for a given component.  The order of precedence is as follows:
     * 1.  pid from within the component
     * 2.  pid from the ExternalPidResolver
     * 3.  newly created pid (or TBD if a dry-run)
     */
    private String getPid(EADNode component) throws Exception {
        final String componentPid = component.getPid();
        if (dryRun) {
            return componentPid != null ? componentPid : "TBD";
        } else if (componentPid != null) {
            // check the component
            pids.storePidForNode(component, componentPid);
            return componentPid;
        } else if (pids.getPidForNode(component) != null) {
            // check the pid cache
            return pids.getPidForNode(component);
        } else {
            // create a new pid and cache it unless we're doing a dry-run
            final String pid = data.mintId();
            pids.storePidForNode(component, pid);
            return pid;
        }
    }

    /**
     * Gets a PID for a given holdings.  The order of precedence is as follows:
     * 1.  pid from the ExternalPidResolver
     * 2.  newly created pid (or TBD if a dry-run)
     */
    private String getPid(HoldingsInfo holdings) throws Exception {
        if (dryRun) {
            return "TBD";
        } else if (pids.getPidForNodeReferenceId(holdings.getId()) != null) {
            // check the pid cache
            return pids.getPidForNodeReferenceId(holdings.getId());
        } else {
            // create a new pid and cache it unless we're doing a dry-run
            final String pid = data.mintId();
            pids.storePidForNodeReferenceId(holdings.getId(), pid);
            return pid;
        }
    }

}
