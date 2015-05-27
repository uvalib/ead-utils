package edu.virginia.lib.ead;


import edu.virginia.lib.TracksysHacker;
import edu.virginia.lib.ead.visitors.TimingNodeVisitor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReedImageAnalyzer {
    
    public static void main(String[] args) throws Exception {
        final Fedora3DataStore datastore = new Fedora3DataStore();
        ReedImageAnalyzer a = new ReedImageAnalyzer(datastore);
    }
    
    private TracksysHacker t;
    
    private Fedora3DataStore f3;
    
    private PrintWriter workAssignment;

    private Set<String> pidsInEAD;
    
    private Set<String> pidsInTracksys;
    
    private Map<String, List<String>> pidToImagePidsMap;
    
    private Map<String, String> eadPidToTypeMap;
    
    public ReedImageAnalyzer(Fedora3DataStore ds) throws Exception {
        workAssignment = new PrintWriter(new OutputStreamWriter(new FileOutputStream("reed-problems-2.csv")));
        workAssignment.println("pid,type,in tracksys,in fedora");
        
        f3 = ds;
        t = new TracksysHacker(TracksysHacker.REED_COLLECTION_COMPONENT_ID);
        pidsInTracksys = new HashSet<String>();
        pidToImagePidsMap = new HashMap<String, List<String>>();
        eadPidToTypeMap = new HashMap<String, String>();
        //eadIdMap = new HashMap<String, List<String>>();
        pidsInEAD = new HashSet<String>();
        getEADPidList();
        System.out.println("ID assignment:");
        populateTracksysMaps(t.getTree());
        
        //System.out.println("\nIngest status:");
        //auditFedoraImages(t.getTree());
        
        try {
            try {
                generateProgressReport();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } finally {
            workAssignment.flush();
            workAssignment.close();
        }
    }

    /**
     * Walk through the pids in the finding aid and report whether they're in Fedora.  
     * For those with digitized images, ensure that those digitized images are in Fedora. 
     */
    private void generateProgressReport() {
        for (String pid : pidsInEAD) {
            reportOnPid(pid, false);
        }
    }
    
    private void reportOnPid(String pid, boolean image) {
        boolean inTracksys = pidsInTracksys.contains(pid);
        String type = image ? "image" : eadPidToTypeMap.get(pid);
        boolean inFedora = f3.exists(pid);
        workAssignment.println(pid + "," +type + "," + inTracksys + "," + inFedora);
        if (pidToImagePidsMap.containsKey(pid)) {
            for (String imagePid : pidToImagePidsMap.get(pid)) {
                reportOnPid(imagePid, true);
            }
        }
    }
    
    private void getEADPidList() throws Exception {
        XMLEADProcessor p = new XMLEADProcessor();
        p.addVisitor(new EADNodeVisitor() {
            @Override
            public void init() throws Exception {

            }

            @Override
            public void visit(EADNode component) throws Exception {
                if (component.getLevel().equalsIgnoreCase("Collection")) {
                    System.out.println("RootId: " + component.getReferenceId());
                }
                if (component.getPid() == null) {
                    System.out.println(component.getLevel() + " has no pid.");
                } else {
                    pidsInEAD.add(component.getPid());
                    eadPidToTypeMap.put(component.getPid(), component.getLevel());
                }
            }

            @Override
            public void finish() throws Exception {

            }
        });
        p.processEADXML(getClass().getClassLoader().getResourceAsStream("viuh00010.xml"));
        
    }

    private void auditFedoraImages(TracksysHacker.Component c) {
        if (c.getChildren() != null) {
            for (TracksysHacker.Component child : c.getChildren()) {
                auditFedoraImages(child);
            }
        }
        if (c.getImages() != null) {
            List<String> imagePids = new ArrayList<String>();
            for (TracksysHacker.MasterFile image : c.getImages()) {
                if (!f3.exists(image.getPid())) {
                    System.err.println("Image " + image.getTracksysId() + " (" + image.getPid() + ") isn't in fedora!");
                }
            }
            if (!f3.exists(c.getPID())) {
                System.out.println("Item " + c.getEADID() + " (" + c.getPID() + ") isn't in fedora!");
            }
        }        
    }

    private void populateTracksysMaps(TracksysHacker.Component c) {
        if (c.getPID() == null) {
            System.err.println(c.getTitle() + " has no pid in tracksys!");
        }
        pidsInTracksys.add(c.getPID());
        if (c.getChildren() != null) {
            for (TracksysHacker.Component child : c.getChildren()) {
                populateTracksysMaps(child);
            }
        }
        if (c.getImages() != null) {
            List<String> imagePids = new ArrayList<String>();
            for (TracksysHacker.MasterFile image : c.getImages()) {
                pidsInTracksys.add(image.getPid());
                imagePids.add(image.getPid());
            }
            pidToImagePidsMap.put(c.getPID(), imagePids);
        }
    }

}
