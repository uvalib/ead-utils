package edu.virginia.lib.ead;

public interface HoldingsInfo {

    public boolean shouldBeLinkedToNode(EADNode node);

    public String getMetadataForHolding();

    public String getId();

}
