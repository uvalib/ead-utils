package edu.virginia.lib.ead;

public interface VisibilityAssignment {

    public String getVisibilityForNode(EADNode node);

    public static VisibilityAssignment COLLECTION_ONLY = new CollectionOnlyVisiblityAssignment();

    static class CollectionOnlyVisiblityAssignment implements VisibilityAssignment {
        @Override
        public String getVisibilityForNode(EADNode node) {
            if (node.getLevel().equals("collection")) {
                return "VISIBLE";
            } else {
                return "UNDISCOVERABLE";
            }
        }
    }
}
