package edu.virginia.lib.ead;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.http.HttpResponse;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;
import java.util.UUID;

import static java.lang.Integer.MAX_VALUE;

public class Fedora4DataStore implements DataStore {

    /**
     * The base URL for requests.  This may include a transaction ID if working
     * under a transaction at the time.
     */
    private String baseUrl;

    private String collectionPid;

    private String txId;

    private URI uri;

    public Fedora4DataStore(String url, String collectionPid) throws Exception {
        this.uri = new URI(url);
        this.baseUrl = this.uri.toString();
        this.collectionPid = "";

        // create collection object if missing
        final HttpResponse headResponse = this.getHeaders(collectionPid);
        if (headResponse.getStatusLine().getStatusCode() == 404) {
            createObject(collectionPid);
        }
        this.collectionPid = collectionPid;

    }

    protected static HttpClient client = createClient();

    protected static HttpClient createClient() {
        return HttpClientBuilder.create().setMaxConnPerRoute(MAX_VALUE)
                .setMaxConnTotal(MAX_VALUE).build();
    }

    @Override
    public URI getURI() {
        return uri;
    }

    /**
     * Fedora 4 does not have a concept of minting an ID before using it, so
     * instead this dataStore implementation will do something.
     */
    @Override
    public String mintId(String hint) throws Exception {
        return hint == null ? UUID.randomUUID().toString() : URLEncoder.encode(hint.replace(":", "-"), "UTF-8");
    }

    @Override
    public void addOrReplaceEADFragment(String id, EADNode node) throws Exception {
        final HttpResponse headResponse = this.getHeaders(id);
        if (isNotThereResponse(headResponse)) {
            // create new object and datastream
            createObject(id);
        } else if (headResponse.getStatusLine().getStatusCode() == 200) {
            // fall through and update the object
        } else {
            throw new Exception("Error querying object " + id + "! (" + headResponse.getStatusLine().toString());
        }
        putDatastream(id, "ead-fragment", node.getXMLFragment(), "text/xml");
        addTitle(id, node.getTitle());
    }

    @Override
    public void addOrReplaceHolding(String pid, HoldingsInfo holdings) throws Exception {
        final HttpResponse headResponse = this.getHeaders(pid);
        if (isNotThereResponse(headResponse)) {
            // create new object and datastream
            createObject(pid);
            putDatastream(pid, "holdings", holdings.getMetadataForHolding(), "text/xml");
        } else if (headResponse.getStatusLine().getStatusCode() == 200) {
            // replace the datastream
            putDatastream(pid, "ead-fragment", holdings.getMetadataForHolding(), "text/xml");
        } else {
            throw new Exception("Error querying object " + pid + "! (" + headResponse.getStatusLine().toString());
        }
    }

    @Override
    public void addOrReplaceTEI(String pid, File teiFile, String teiPid) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearRelationships(String pid) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setChildRelationship(String pid, String childPid) throws Exception {
        String parentUri = this.getCollectionBaseUrl() + "/" + pid;
        String childUri = this.getCollectionBaseUrl() + "/" + childPid;
        String sparqlUpdate = "insert { <" + childUri + "> <" + RDFConstants.IS_PART_OF_PREDICATE + "> <" + parentUri + "> . } where {}";
        patchRDF(childPid, sparqlUpdate);
    }

    /**
     * NEVER CALL, it currently screws up our pid mapping....
     * @param pid
     * @param newParent
     * @throws Exception
     */
    private void move(String pid, String newParent) throws Exception {
        final String destination = getCollectionBaseUrl() + "/" + newParent + (pid.indexOf('/') != -1 ? pid.substring(pid.lastIndexOf('/')) : pid);
        final HttpMove request = new HttpMove(getCollectionBaseUrl() + "/" + pid);
        request.addHeader("Destination", destination);
        try {
            final HttpResponse moveResponse = client.execute(request);
            requireResponse("Failed to move within hierarchy.", moveResponse, 201);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public void setHasImageRelationship(String pid, String imagePid) throws Exception {
        String imageUri = this.getCollectionBaseUrl() + "/" + imagePid;
        String itemUri = this.getCollectionBaseUrl() + "/" + pid;
        String sparqlUpdate = "insert { <" + itemUri + "> <" + RDFConstants.HAS_DIGITAL_REPRESENTATION_PREDICATE + "> <" + imageUri + "> . "
                + "<" + imageUri + "> <" + RDFConstants.IS_DIGITAL_REPRESENTATION_OF_PREDICATE + "> <" + itemUri + "> } where {}";
        patchRDF(pid, sparqlUpdate);
        //move(imagePid, pid); // just for organizational ease, though might cause problems...
    }

    @Override
    public void setHasExemplarImageRelationship(String pid, String exemplarPid) throws Exception {
        String imageUri = this.getCollectionBaseUrl() + "/" + exemplarPid;
        String itemUri = this.getCollectionBaseUrl() + "/" + pid;
        String sparqlUpdate = "insert { <" + itemUri + "> <" + RDFConstants.HAS_EXEMPLAR_PREDICATE + "> <" + imageUri + "> . } where {}";
        patchRDF(pid, sparqlUpdate);
    }

    @Override
    public void setSequenceRelationship(String previousPid, String childPid) throws Exception {
        String prevUri = this.getCollectionBaseUrl() + "/" + previousPid;
        String nextUri = this.getCollectionBaseUrl() + "/" + childPid;
        String sparqlUpdate = "insert { <" + prevUri + "> <" + RDFConstants.FOLLOWS_PREDICATE + "> <" + nextUri + "> . } where {}";
        patchRDF(childPid, sparqlUpdate);
    }

    @Override
    public void setVisibility(String pid, String value) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHoldingsRelationship(String componentPid, String holdingsPid) throws Exception {
        String componentUri = this.getCollectionBaseUrl() + "/" + componentPid;
        String holdingsUri = this.getCollectionBaseUrl() + "/" + holdingsPid;
        String sparqlUpdate = "insert { <" + componentUri + "> <" + RDFConstants.IS_CONTAINED_WITHIN_PREDICATE + "> <" + holdingsUri + "> . } where {}";
        patchRDF(componentPid, sparqlUpdate);
    }

    @Override
    public List<String> getOrderedImagePids(String pid) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getImagePids(String pid) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void purge(String id) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(String id) throws Exception {
        return getHeaders(id).getStatusLine().getStatusCode() == 200;
    }

    @Override
    public void startTransaction() throws Exception {
        final HttpPost createTx = new HttpPost(this.baseUrl +"/" + "fcr:tx");
        try {
            final HttpResponse response = client.execute(createTx);
            requireResponse("Unable to start transaction!", response, 201);
            this.txId = response.getFirstHeader("Location").getValue();
            this.txId = this.txId.substring(this.txId.lastIndexOf('/') + 1);
        } finally {
            createTx.releaseConnection();
        }
    }

    @Override
    public void commitTransaction() throws Exception {
        final HttpPost commitTx =
                new HttpPost(this.baseUrl + "/" + txId + "/fcr:tx/fcr:commit");
        try {
            final HttpResponse response = client.execute(commitTx);
            requireResponse("Unable to commit transaction!", response, 204);
            this.txId = null;
        } finally {
            commitTx.releaseConnection();
        }
    }

    @Override
    public void rollBackTransaction() throws Exception {
        final HttpPost rollbackTx =
                new HttpPost(this.baseUrl + "/" + txId + "/fcr:tx/fcr:rollback");
        try {
            final HttpResponse resp = client.execute(rollbackTx);
            requireResponse("Unable to roll back transaction!", resp, 204);
            this.txId = null;
        } finally {
            rollbackTx.releaseConnection();
        }
    }

    @Override
    public String sanitizePid(String pid) {
        return pid.replace(":", "-");
    }

    private void requireResponse(String message, HttpResponse resp, int code) throws Exception {
        if (code != resp.getStatusLine().getStatusCode()) {
            throw new Exception(message + " (" + resp.getStatusLine() + ")");
        }
    }

    private boolean isNotThereResponse(HttpResponse response) {
        final int code = response.getStatusLine().getStatusCode();
        return (code == 404 || code == 410);
    }

    private HttpResponse getHeaders(String pid) throws IOException {
        final HttpHead headRequest = new HttpHead(getCollectionBaseUrl() + "/" + pid);
        try {
            return client.execute(headRequest);
        } finally {
            headRequest.releaseConnection();
        }
    }

    private HttpResponse createObject(final String pid) throws Exception {
        String base = baseUrl + "/" + (txId != null ? txId : "");
        System.out.println(base);
        final HttpPost httpPost = new HttpPost(base);
        if (pid.length() > 0) {
            httpPost.addHeader("Slug", collectionPid + "/" + pid);
        }
        try {
            final HttpResponse response = client.execute(httpPost);
            requireResponse("Unable to create object!", response, 201);
            return response;
        } finally {
            httpPost.releaseConnection();
        }
    }

    private HttpResponse putDatastream(String pid, String dsId, String content, String mimType) throws Exception {
        final HttpPut put =
                new HttpPut(getCollectionBaseUrl() + "/" + pid + "/" + dsId);

        put.setEntity(new StringEntity(content));
        try {
            final HttpResponse response = client.execute(put);
            requireResponse("Unable to put datastream!", response, 201);
            return response;
        } finally {
            put.releaseConnection();
        }
    }

    private void addTitle(String pid, String title) throws Exception {
        Model m = ModelFactory.createDefaultModel();
        patchRDF(pid, "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "INSERT {   \n" +
                "  <> dc:title \"\"\"" + m.createTypedLiteral(title).getString() + "\"\"\" .\n" +
                "}\n" +
                "WHERE { }");
    }

    private void patchRDF(String pid, String sparql) throws Exception {
        System.out.println(pid + ": " + sparql);
        final HttpPatch patch = new HttpPatch(getCollectionBaseUrl() + "/" + pid);
        patch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(sparql.getBytes()));
        patch.setEntity(e);
        try {
            final HttpResponse response = client.execute(patch);
            requireResponse("Spaql update failed!", response, 204);
        } finally {
            patch.releaseConnection();
        }
    }

    private String getCollectionBaseUrl() {
        if (txId != null) {
            return this.baseUrl + "/" + txId + "/" + collectionPid;
        } else {
            return this.baseUrl + "/" + collectionPid;
        }
    }

    private String getRootUrl() {
        return this.baseUrl;
    }

    @NotThreadSafe // HttpRequestBase is @NotThreadSafe
    private class HttpMove extends HttpRequestBase {

        public final static String METHOD_NAME = "MOVE";


        /**
         * @throws IllegalArgumentException if the uri is invalid.
         */
        public HttpMove(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

    }
}
