package edu.virginia.lib.ead;

import java.io.IOException;

/**
 * An interface describing a class that has node-specific processing.
 */
public interface EADNodeVisitor {

    public void init() throws Exception;

    public void visit(EADNode component) throws Exception;

    public void finish() throws Exception;

}
