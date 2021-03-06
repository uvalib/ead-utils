package edu.virginia.lib.ead;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraClientException;
import com.yourmediashelf.fedora.client.FedoraCredentials;
import com.yourmediashelf.fedora.generated.access.DatastreamType;
import edu.virginia.lib.indexing.FedoraRiSearcher;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Properties;

public class Fedora3DataStore implements DataStore {

    private static final String COLLECTION_CONTENT_MODEL_PID = "uva-lib:eadCollectionCModel";
    private static final String COMPONENT_CONTENT_MODEL_PID = "uva-lib:eadComponentCModel";
    private static final String ITEM_CONTENT_MODEL_PID = "uva-lib:eadItemCModel";

    private static final String EAD_FRAGMENT_CONTENT_MODEL_PID = "uva-lib:eadMetadataFragmentCModel";

    private static final String HOLDING_CONTENT_MODEL_PID = "uva-lib:containerCModel";
    private static final String TEI_CONTENT_MODEL_PID = "uva-lib:teiCModel";

    private static final String HAS_MODEL_PREDICATE = "info:fedora/fedora-system:def/model#hasModel";

    private static final String EAD_FRAGMENT_DS_ID = "descMetadata";
    private static final String HOLDING_DS_ID = "descMetadata";
    private static final String TEI_DS_ID = "encodedText";

    private FedoraClient fc;

    /**
     * A constructor that instantiates a Fedora3DataStore using properties
     * found on the classpath in "fedora.properties".
     */
    public Fedora3DataStore() throws IOException {
        Properties p = new Properties();
        p.load(Fedora3DataStore.class.getClassLoader().getResourceAsStream("conf/fedora.properties"));
        fc = new FedoraClient(new FedoraCredentials(p.getProperty("fedora-url"),
                p.getProperty("fedora-username"), p.getProperty("fedora-password")));
    }

    public FedoraClient getFedoraClient() {
        return this.fc;
    }

    @Override
    public URI getURI() {
        try {
            return new URI(FedoraClient.describeRepository().execute(fc).getRepositoryInfo().getRepositoryBaseURL());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String mintId(String hint) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(FedoraClient.getNextPID().execute(fc).getEntityInputStream(), baos);
            final String response = new String(baos.toByteArray());
            final String pid = response.substring(response.indexOf("<pid>") + 5, response.indexOf("</pid>"));
            //System.out.println(response);
            //System.out.println("New pid: \"" + pid + "\"");
            return pid;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createIfMissing(String pid) throws Exception {
        if (!exists(pid)) {
            // create new object if it didn't
            final String createdPid = FedoraClient.ingest(pid).execute(fc).getPid();
            if (!createdPid.equals(pid)) {
                throw new RuntimeException("Unexpected pid \"" + createdPid + "\" created!");
            }
        }
    }

    private void updateIfDifferent(String pid, String dsId, String content) throws Exception {
        boolean unchanged = false;
        boolean exists = false;
        for (DatastreamType ds : FedoraClient.listDatastreams(pid).execute(fc).getDatastreams()) {
            if (ds.getDsid().equals(dsId)) {
                exists = true;
                // found, see if it changed
                if (DigestUtils.md5Hex(content).equalsIgnoreCase(FedoraClient.getDatastream(pid, dsId).execute(fc).getDatastreamProfile().getDsChecksum())) {
                    // no need to replace
                    unchanged = true;
                    System.out.println(pid + " was unchanged.");
                } else {
                    System.out.println(DigestUtils.md5Hex(content) + " != " + FedoraClient.getDatastream(pid, dsId).execute(fc).getDatastreamProfile().getDsChecksum());
                }
                break;
            }
        }
        if (exists && unchanged) {
            // no need to update...
        } else if (exists && !unchanged) {
            // replace existing with new copy
            FedoraClient.addDatastream(pid, dsId).content(content).checksumType("md5").mimeType("text/xml").controlGroup("M").execute(fc);
        } else {
            // doesn't exist, create new
            FedoraClient.addDatastream(pid, dsId).content(content).checksumType("md5").mimeType("text/xml").controlGroup("M").execute(fc);
        }
    }

    @Override
    public void addOrReplaceEADFragment(String id, EADNode node) throws Exception {
        // check if the item exists
        createIfMissing(id);

        // check if the datastream doesn't exist, or is different
        updateIfDifferent(id, EAD_FRAGMENT_DS_ID, node.getXMLFragment());

        // set the content models
        FedoraClient.addRelationship(id).object("info:fedora/" + EAD_FRAGMENT_CONTENT_MODEL_PID).predicate(HAS_MODEL_PREDICATE).execute(fc);
        if ("collection".equals(node.getLevel())) {
            FedoraClient.addRelationship(id).object("info:fedora/" + COLLECTION_CONTENT_MODEL_PID).predicate(HAS_MODEL_PREDICATE).execute(fc);
        } else if ("item".equals(node.getLevel()) || "file".equals(node.getLevel())) {
            FedoraClient.addRelationship(id).object("info:fedora/" + ITEM_CONTENT_MODEL_PID).predicate(HAS_MODEL_PREDICATE).execute(fc);
        } else {
            FedoraClient.addRelationship(id).object("info:fedora/" + COMPONENT_CONTENT_MODEL_PID).predicate(HAS_MODEL_PREDICATE).execute(fc);
        }

    }

    @Override
    public void addOrReplaceHolding(String pid, HoldingsInfo holdings) throws Exception {
        // check if the item exists
        createIfMissing(pid);

        // check if the datastream doesn't exist, or is different
        updateIfDifferent(pid, HOLDING_DS_ID, holdings.getMetadataForHolding());

        // set the content models
        FedoraClient.addRelationship(pid).object("info:fedora/" + HOLDING_CONTENT_MODEL_PID).predicate(HAS_MODEL_PREDICATE).execute(fc);
    }

    @Override
    public void addOrReplaceTEI(String pid, File teiFile, String parentPid) throws Exception {
        // check if the item exists
        createIfMissing(pid);

        // check if the datastream doesn't exist, or is different
        updateIfDifferent(pid, TEI_DS_ID, FileUtils.readFileToString(teiFile));

        // set the content models
        FedoraClient.addRelationship(pid).object("info:fedora/" + TEI_CONTENT_MODEL_PID).predicate(HAS_MODEL_PREDICATE).execute(fc);

        FedoraClient.addRelationship(pid).object("info:fedora/" + parentPid).predicate(RDFConstants.IS_ENCODED_TEXT_FOR).execute(fc);

    }

    @Override
    public void clearRelationships(String pid) throws Exception {
        if (hasDatastream(pid, "RELS-EXT")) {
            final String emptyRelsExt = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<rdf:RDF xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:rel=\"info:fedora/fedora-system:def/relations-external#\" xmlns:uva=\"http://fedora.lib.virginia.edu/relationships#\">\n" +
                    "  <rdf:Description rdf:about=\"info:fedora/" + pid + "\">\n" +
                    "  </rdf:Description>\n" +
                    "</rdf:RDF>";
            FedoraClient.modifyDatastream(pid, "RELS-EXT").content(emptyRelsExt).mimeType("text/xml").execute(fc);
        }
    }

    @Override
    public void setChildRelationship(String pid, String childPid) throws FedoraClientException {
        FedoraClient.addRelationship(childPid).object("info:fedora/" + pid.toString()).predicate(RDFConstants.IS_PART_OF_PREDICATE).execute(fc);
        System.out.println(childPid + " -> isPartOf -> " + pid);
    }

    @Override
    public void setHasImageRelationship(String pid, String imagePid) throws Exception {
        FedoraClient.addRelationship(imagePid).object("info:fedora/" + pid.toString()).predicate(RDFConstants.IS_DIGITAL_REPRESENTATION_OF_PREDICATE).execute(fc);
        FedoraClient.addRelationship(pid).object("info:fedora/" + imagePid.toString()).predicate(RDFConstants.HAS_DIGITAL_REPRESENTATION_PREDICATE).execute(fc);
    }

    @Override
    public void setHasExemplarImageRelationship(String pid, String exemplarPid) throws Exception {
        if (pid == null || exemplarPid == null) {
            throw new NullPointerException();
        }
        FedoraClient.addRelationship(pid).object("info:fedora/" + exemplarPid.toString()).predicate(RDFConstants.HAS_EXEMPLAR_PREDICATE).execute(fc);
    }

    @Override
    public void setSequenceRelationship(String previousPid, String pid) throws FedoraClientException {
        FedoraClient.addRelationship(pid).object("info:fedora/" + previousPid.toString()).predicate(RDFConstants.FOLLOWS_PREDICATE).execute(fc);
    }

    @Override
    public void setVisibility(String pid, String value) throws Exception {
        if (value.equals("HIDDEN") || value.equals("VISIBLE") || value.equals("UNDISCOVERABLE")) {
            FedoraClient.addRelationship(pid).object(value, true).predicate(RDFConstants.VISIBILITY_PREDICATE).execute(fc);
        } else {
            throw new IllegalArgumentException("Unknown visibility \"" + value + "\"!");
        }
    }

    @Override
    public void setHoldingsRelationship(String componentPid, String holdingsPid) throws Exception {
        FedoraClient.addRelationship(componentPid).object("info:fedora/" + holdingsPid.toString()).predicate(RDFConstants.IS_CONTAINED_WITHIN_PREDICATE).execute(fc);
    }

    @Override
    public List<String> getOrderedImagePids(String pid) throws Exception {
        return FedoraRiSearcher.getOrderedPartsUsingParentsListing(fc, pid, RDFConstants.HAS_DIGITAL_REPRESENTATION_PREDICATE, RDFConstants.FOLLOWS_PAGE_PREDICATE);
    }

    @Override
    public List<String> getImagePids(String pid) throws Exception {
        return FedoraRiSearcher.getObjects(fc, pid, RDFConstants.HAS_DIGITAL_REPRESENTATION_PREDICATE);
    }

    @Override
    public void purge(String id) throws Exception {
        FedoraClient.purgeObject(id).execute(fc);
    }

    @Override
    public boolean exists(String id) {
        try {
            FedoraClient.getObjectProfile(id).execute(fc);
            return true;
        } catch (FedoraClientException e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                return false;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean hasDatastream(String pid, String dsId) {
        try {
            for (DatastreamType ds : FedoraClient.listDatastreams(pid).execute(fc).getDatastreams()) {
                if (ds.getDsid().equals(dsId)) {
                    return true;
                }
            }
            return false;
        } catch (FedoraClientException e) {
            if (e.getMessage().indexOf("404") != -1) {
                return false;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void startTransaction() throws Exception {

    }

    @Override
    public void commitTransaction() throws Exception {

    }

    @Override
    public void rollBackTransaction() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public String sanitizePid(String pid) {
        return pid;
    }

}
