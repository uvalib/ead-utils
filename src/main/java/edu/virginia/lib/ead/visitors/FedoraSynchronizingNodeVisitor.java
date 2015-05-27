package edu.virginia.lib.ead.visitors;

import edu.virginia.lib.ead.DataStore;
import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.EADNodeVisitor;
import edu.virginia.lib.ead.EncodedTextMapper;
import edu.virginia.lib.ead.HoldingsInfo;
import edu.virginia.lib.ead.ImageMapper;
import edu.virginia.lib.ead.PidFilter;
import edu.virginia.lib.ead.ExternalPidResolver;
import edu.virginia.lib.ead.VisibilityAssignment;

import java.io.File;
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

    private EncodedTextMapper textMapper;

    private VisibilityAssignment visibilityAssignment;

    private TimingNodeVisitor t;

    public FedoraSynchronizingNodeVisitor(DataStore ds, ExternalPidResolver pids, VisibilityAssignment visibility, boolean dryRun) throws IOException {
        this.data = ds;
        this.pids = pids;
        this.dryRun = dryRun;
        this.visibilityAssignment = visibility;
        this.holdings = new ArrayList<HoldingsInfo>();
    }

    public void setTimer(TimingNodeVisitor t) {
        this.t = t;
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

    public void setTextMapper(EncodedTextMapper m) {
        this.textMapper = m;
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
            if (filter != null && t != null && !filter.includePid(pid, component)) {
                t.omitLast();
            }
            if (!dryRun) {
                System.out.println("Considering " + pid + "...");
                // Clear existing relationships
                // for items made using the tracking system some inapplicable RDF
                // would otherwise persist....
                if (updatePid(pid, component) && data.exists(pid)) {
                    data.clearRelationships(pid);
                }

                // update metadata
                if (updatePid(pid, component)) {
                    data.addOrReplaceEADFragment(pid, component);
                }

                // update relationships
                String previousPid = null;
                for (String childId : component.getChildReferenceIds()) {
                    final String childPid = pids.getPidForNodeReferenceId(childId);
                    if (updatePid(pid, component) && updatePid(childPid, component)) {
                        data.setChildRelationship(pid, childPid);
                    }
                    if (previousPid != null) {
                        if (updatePid(previousPid, component) && updatePid(childPid, component)) {
                            data.setSequenceRelationship(previousPid, childPid);
                        }
                    }
                    previousPid = childPid;
                }

                // assign visiblity
                data.setVisibility(pid, visibilityAssignment.getVisibilityForNode(component));

                // add holdings if available
                addHoldings(pid, component);

                // add images if available
                linkImages(pid, component);

                // add text if available
                addText(pid, component);
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
                if (updatePid(pid, component)) {
                    data.setHasImageRelationship(pid, imagePid);
                }
            }
            final String exemplarPid = imageMapper.getExemplarPid(component);
            if (exemplarPid != null) {
                if (updatePid(pid, component)) {
                    data.setHasExemplarImageRelationship(pid, exemplarPid);
                }
            }
        }
    }

    private void addHoldings(String pid, EADNode component) throws Exception {
        for (HoldingsInfo h : this.holdings) {
            if (h.shouldBeLinkedToNode(component)) {
                if (updatePid(pid, component)) {
                    data.setHoldingsRelationship(pid, getPid(h));
                }
            }
        }
    }

    private void addText(String parentPid, EADNode component) throws Exception {
        if (textMapper != null) {
            File tei = textMapper.getTEIForNode(component);
            if (tei != null && tei.exists()) {
                final String teiPid = getTeiPid(component);
                if (updatePid(teiPid, component)) {
                    data.addOrReplaceTEI(teiPid, tei, parentPid);
                }
            }
        }
    }

    private boolean updatePid(String pid, EADNode component) {
        if (filter == null) {
            return true;
        }
        return filter.includePid(pid, component);
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
            final String pid = data.sanitizePid(componentPid);
            // check the component
            pids.storePidForNode(component, pid);
            return pid;
        } else if (pids.getPidForNode(component) != null) {
            // check the pid cache
            return pids.getPidForNode(component);
        } else {
            // create a new pid and cache it unless we're doing a dry-run
            final String pid = data.mintId(component.getReferenceId());
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
            final String pid = data.mintId(holdings.getId());
            pids.storePidForNodeReferenceId(holdings.getId(), pid);
            return pid;
        }
    }

    /**
     * Gets a PID for a given encoded text associated with the provided EADNode.
     * The order of precedence is as follows:
     * 1.  pid from the ExternalPidResolver
     * 2.  newly created pid (or TBD if a dry-run)
     */
    private String getTeiPid(EADNode component) throws Exception {
        if (dryRun) {
            return "TBD";
        } else if (pids.getPidForNodeReferenceId(component.getUnitId()) != null) {
            // check the pid cache
            return pids.getPidForNodeReferenceId(component.getUnitId());
        } else {
            // create a new pid and cache it unless we're doing a dry-run
            final String pid = data.mintId(component.getUnitId());
            pids.storePidForNodeReferenceId(component.getUnitId(), pid);
            return pid;
        }
    }

}
