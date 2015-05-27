package edu.virginia.lib.ead;

import java.io.File;
import java.util.List;

public interface EncodedTextMapper {

    public File getTEIForNode(EADNode node) throws Exception;

    public File[] getAllTeiFiles();

    public List<String> getAllUnfoundIds();
}
