package edu.virginia.lib.ead;

import javax.xml.parsers.ParserConfigurationException;
import java.util.List;

/**
 * An abstraction representing a node within an EAD (or
 * other hierarchical descriptive structure).  This interface
 * is designed to allow sequential reading of a finding aid
 * where only one node must reside in memory at once.  To do
 * so, implementations may have to visit nodes in a depth-first
 * fashion.
 */
public interface EADNode {

    /**
     * Gets the original tag name if this came from
     * an XML element.
     */
    public String getTagName();

    /**
     * Gets the level ("collection", "series", "item", etc.)
     * for this node.
     */
    public String getLevel();

    /**
     * Gets the persistent identifier for this node.
     */
    public String getPid();

    /**
     * Gets an XML serialization of the fragment of the entire
     * underlying document that represents this node. This may
     * not be a contiguous chunk since nested children will not
     * be included.
     */
    public String getXMLFragment();

    public String getReferenceId();

    public List<String> getChildReferenceIds();

    public String getTitle();

    public String getUnitId();
}
