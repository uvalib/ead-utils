package edu.virginia.lib.ead.visitors;

import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.EADNodeVisitor;

public class NodeCountingNodeVisitor implements EADNodeVisitor {

    int count = 0;

    @Override
    public void init() {
    }

    @Override
    public void visit(EADNode component) {
        count ++;
    }

    @Override
    public void finish() {
    }

    public int getCount() {
        return count;
    }
}
