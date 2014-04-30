package edu.virginia.lib.ead.visitors;

import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.EADNodeVisitor;

public class StdOutNodeVisitor implements EADNodeVisitor {

    private int i;

    @Override
    public void init() {
        i = 1;
    }

    @Override
    public void visit(EADNode component) {
        System.out.println(i ++);
        System.out.println(component.getXMLFragment());
    }

    @Override
    public void finish() {
    }
}
