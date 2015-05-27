package edu.virginia.lib.ead.visitors;

import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.EADNodeVisitor;
import edu.virginia.lib.ead.ExternalPidResolver;
import edu.virginia.lib.ead.PidFilter;

public class TimingNodeVisitor implements EADNodeVisitor {

    int total;
    int i;
    long last;
    long start;

    public TimingNodeVisitor() {
    }

    private ExternalPidResolver pidResolver;

    public TimingNodeVisitor(int total) {
        this.total = total;
    }

    public void setPidResolver(ExternalPidResolver r) {
        this.pidResolver = r;
    }

    @Override
    public void init() {
        this.i = 0;
        this.start = System.currentTimeMillis();
        this.last = start;
    }

    @Override
    public void visit(EADNode component) {
        long duration = System.currentTimeMillis() - last;
        long totalTime = System.currentTimeMillis() - start;
        if (total != 0) {
            double msPerNode = (double) totalTime / (double) i;
            int nodesRemaining = total - i;
            long msRemaining = Math.round((double) nodesRemaining * msPerNode);
            System.out.println((component.getPid() == null ? pidResolver != null ? pidResolver.getPidForNodeReferenceId(component.getReferenceId()) : component.getReferenceId() : component.getPid()));
            //System.out.println((component.getPid() == null ? component.getReferenceId() : component.getPid()) + ", "  + i + " of " + total + " nodes processed in " + prettyPrintElapsedMS(duration) + ".  " + prettyPrintElapsedMS(msRemaining) + " remaining.");
        } else {
            System.out.println("Node " + i + " (" + (component.getPid() == null ? component.getReferenceId() : component.getPid()) + ") took " + prettyPrintElapsedMS(duration) + ".");
        }
        last = System.currentTimeMillis();
        i ++;
    }

    public void omitLast() {
        i --;
        total --;
    }

    @Override
    public void finish() {
    }

    private String prettyPrintElapsedMS(long elapsed) {
        if (elapsed > 5400000)
            // hours
        return "about " + Math.round(((double) elapsed / (double) (1000 * 60 * 60))) + " hours";
        if (elapsed > 90000)
            // minutes
            return "about " + Math.round(((double) elapsed / (double) (1000 * 60))) + " minutes";
        if (elapsed > 500) {
            // seconds
            return "about " + Math.round(((double) elapsed / (double) (1000))) + " seconds";
        } else {
            // ms
            return elapsed + "ms";
        }
    }
}
