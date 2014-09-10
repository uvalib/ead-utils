package edu.virginia.lib.ead;

public class DefaultCollectionLevelHoldingsInfo implements HoldingsInfo {

    private String id;
    private String catKey;
    private String type;
    private String barcode;

    public DefaultCollectionLevelHoldingsInfo(String callNumber, String catKey, String type, String barcode) {
        this.id = callNumber;
        this.catKey = catKey;
        this.type = type;
        this.barcode = barcode;
    }

    /**
     * Holdings applies to all items. (the whole collection)
     */
    @Override
    public boolean shouldBeLinkedToNode(EADNode node) {
        return node.getLevel().equalsIgnoreCase("item");
    }

    @Override
    public String getMetadataForHolding() {
        return "<container>\n" +
                "    <callNumber>" + id + "</callNumber>\n" +
                "    <catalogKey>" + catKey + "</catalogKey>\n" +
                "    <type>" + type + "</type>\n" +
                "    <barCode>" + barcode + "</barCode>\n" +
                "</container>";
    }

    @Override
    public String getId() {
        return id;
    }
}
