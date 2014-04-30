package edu.virginia.lib.ead;

import java.net.URI;

/**
 * A layer of abstraction on top of Fedora.  This will make the fedora 3 to fedora 4
 * migration easier.
 */
public interface DataStore {

    /**
     * URI that identifies the storage location.
     */
    public URI getURI();

    /**
     * Creates a new ID for use in the system to store
     * a resource.
     */
    public String mintId() throws Exception;

    /**
     * Adds or replaces the EAD fragment stored for the
     * given ID.
     */
    public void addOrReplaceEADFragment(String id, EADNode node) throws Exception;

    public void addOrReplaceHolding(String pid, HoldingsInfo holdings) throws Exception;

    public void setChildRelationship(String pid, String childPid) throws Exception;

    public void setSequenceRelationship(String previousPid, String childPid) throws Exception;

    public void setHoldingsRelationship(String componentPid, String holdingsPid) throws Exception;

    public void purge(String id) throws Exception;

}
