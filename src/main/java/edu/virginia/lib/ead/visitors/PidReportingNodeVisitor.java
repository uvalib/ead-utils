package edu.virginia.lib.ead.visitors;

import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.EADNodeVisitor;
import edu.virginia.lib.ead.ExternalPidResolver;

import java.io.PrintWriter;

/**
 * Created by md5wz on 4/15/15.
 */
public class PidReportingNodeVisitor implements EADNodeVisitor {

    private ExternalPidResolver p;

    private PrintWriter out;

    public PidReportingNodeVisitor(PrintWriter out, ExternalPidResolver pidResolver) {
        this.out = out;
        this.p = pidResolver;
    }

    @Override
    public void init() throws Exception {

    }

    @Override
    public void visit(EADNode component) throws Exception {
        out.println(p.getPidForNode(component));
    }

    @Override
    public void finish() throws Exception {

    }
}
