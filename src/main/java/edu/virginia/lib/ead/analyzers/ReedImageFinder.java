package edu.virginia.lib.ead.analyzers;

import edu.virginia.lib.ead.DataStore;
import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.EADNodeVisitor;
import edu.virginia.lib.ead.Fedora3DataStore;
import edu.virginia.lib.ead.XMLEADProcessor;
import edu.virginia.lib.ead.visitors.NodeCountingNodeVisitor;
import edu.virginia.lib.ead.visitors.TimingNodeVisitor;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple program to identify all of the images from the REED finding aid
 * that appear in Fedora.
 */
public class ReedImageFinder implements EADNodeVisitor {

    public static void main(String [] args) throws Exception {
        XMLEADProcessor p = new XMLEADProcessor();
        p.addVisitor(new TimingNodeVisitor(countNodes()));
        p.addVisitor(new ReedImageFinder(new Fedora3DataStore()));
        p.processEADXML(getFindingAid());
    }

    private DataStore ds;

    private Map<String, List<String>> cToImagesMap;

    private PrintWriter output;
    private PrintWriter error;
    private PrintWriter missing;

    public ReedImageFinder(DataStore ds) throws FileNotFoundException {
        this.ds = ds;
    }

    @Override
    public void init() throws Exception {
        cToImagesMap = new HashMap<String, List<String>>();
        output = new PrintWriter(new FileOutputStream(ds.getURI().getHost() + "-reed-pid-mapping-alt.txt"));
        error = new PrintWriter(new FileOutputStream(ds.getURI().getHost() + "-reed-pid-mapping-errors-alt.txt"));
        missing = new PrintWriter(new FileOutputStream(ds.getURI().getHost() + "-reed-pid-mapping-missing-alt.txt"));
    }

    @Override
    public void visit(EADNode component) throws Exception {
        final String pid = component.getPid();
        if (pid != null) {
            if (ds.exists(pid)) {
                try {
                    final List<String> pagePids = ds.getOrderedImagePids(pid);
                    cToImagesMap.put(pid, pagePids);
                    System.out.println(pid + " -> " + pagePids);
                } catch (RuntimeException ex) {
                    StringBuffer children = new StringBuffer();
                    for (String childPid : ds.getImagePids(pid)) {
                        if (children.length() > 0) {
                            children.append(", ");
                        }
                        children.append("\"" + childPid + "\"");
                    }
                    error.println("{\"" + pid + "\"=>[" + children + "],");
                    System.err.println(pid + " is broken! " + ex.getMessage());
                }
            } else {
                missing.println(pid);
                System.out.println(pid + " (" + component.getLevel() + ") doesn't exist in Fedora.");
            }
        }
    }

    @Override
    public void finish() throws Exception {
        boolean first = true;
        for (Map.Entry<String, List<String>> entry : this.cToImagesMap.entrySet()) {
            if (first) {
                first = false;
            } else {
                output.println(",");
            }
            output.print("{\"" + entry.getKey() + "\"=>[");
            boolean firstPage = true;
            for (String page : entry.getValue()) {
                if (firstPage) {
                    firstPage = false;
                } else {
                    output.print(", ");
                }
                output.print("\"" + page + "\"");

            }
            output.print("]}");

        }
        output.close();
        error.close();
        missing.close();
    }

    private static int countNodes() throws Exception {
        System.out.print("Counting nodes...");
        final XMLEADProcessor processor = new XMLEADProcessor();
        final NodeCountingNodeVisitor counter = new NodeCountingNodeVisitor();
        processor.addVisitor(counter);
        processor.processEADXML(getFindingAid());
        System.out.println(counter.getCount());
        return counter.getCount();
    }

    protected static InputStream getFindingAid() {
        return ReedImageFinder.class.getClassLoader().getResourceAsStream("viuh00010.xml");
    }
}
