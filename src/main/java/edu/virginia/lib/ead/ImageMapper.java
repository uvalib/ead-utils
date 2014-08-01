package edu.virginia.lib.ead;

import java.util.List;

public interface ImageMapper {

    public List<String> getImagePids(EADNode node);

    public String getExemplarPid(EADNode node);
}
