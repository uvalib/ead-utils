package edu.virginia.lib.ead.imagemappers;

import edu.virginia.lib.TracksysHacker;
import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.ImageMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TracksysHackerImageMapper implements ImageMapper {
    
    private TracksysHacker t;

    private Map<String, List<String>> pidToImagePidsMap;
    
    private Map<String, String> pidToExemplarPidMap;
    
    public TracksysHackerImageMapper(int rootId) throws Exception {
        t = new TracksysHacker(rootId);
        pidToImagePidsMap = new HashMap<String, List<String>>();
        pidToExemplarPidMap = new HashMap<String, String>();
        visitComponent(t.getTree());
    }

    private void visitComponent(TracksysHacker.Component component) {
        if (component.getImages() != null) {
            if (component.getExemplarPid() == null) {
                System.out.println(component.getPID() + " has no exemplar! " + component.getExemplar());
            }
            pidToExemplarPidMap.put(component.getPID(), component.getExemplarPid());
            List<String> imagePids = new ArrayList<String>();
            for (TracksysHacker.MasterFile image : component.getImages()) {
                imagePids.add(image.getPid());
            }
            pidToImagePidsMap.put(component.getPID(), imagePids);
        }
        if (component.getChildren() != null) {
            for (TracksysHacker.Component c : component.getChildren()) {
                visitComponent(c);
            }
        }
    }

    @Override
    public List<String> getImagePids(EADNode node) {
        return pidToImagePidsMap.get(node.getPid());
    }

    @Override
    public String getExemplarPid(EADNode node) {
        return pidToExemplarPidMap.get(node.getPid());
    }
}
