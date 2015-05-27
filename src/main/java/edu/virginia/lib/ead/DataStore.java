package edu.virginia.lib.ead;

import java.io.File;
import java.net.URI;
import java.util.List;

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
    public String mintId(String hint) throws Exception;

    /**
     * Adds or replaces the EAD fragment stored for the
     * given ID.
     */
    public void addOrReplaceEADFragment(String id, EADNode node) throws Exception;

    public void addOrReplaceHolding(String pid, HoldingsInfo holdings) throws Exception;

    public void addOrReplaceTEI(String pid, File teiFile, String parentPid) throws Exception;

    public void clearRelationships(String pid) throws Exception;

    public void setChildRelationship(String pid, String childPid) throws Exception;

    public void setHasImageRelationship(String pid, String imagePid) throws Exception;

    public void setHasExemplarImageRelationship(String pid, String exemplarPid) throws Exception;

    public void setSequenceRelationship(String previousPid, String childPid) throws Exception;

    public void setVisibility(String pid, String value) throws Exception;

    public void setHoldingsRelationship(String componentPid, String holdingsPid) throws Exception;

    public List<String> getOrderedImagePids(String pid) throws Exception;

    public List<String> getImagePids(String pid) throws Exception;

    public void purge(String id) throws Exception;

    public boolean exists(String id) throws Exception;

    public void startTransaction() throws Exception;

    public void commitTransaction() throws Exception;

    public void rollBackTransaction() throws Exception;

    public String sanitizePid(String pid);

}
