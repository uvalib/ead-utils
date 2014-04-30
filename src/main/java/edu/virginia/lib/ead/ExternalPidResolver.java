package edu.virginia.lib.ead;

import edu.virginia.lib.ead.EADNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Not all pids are stored in the EAD finding aid.  When not found there
 * new ones will need to be assigned and then used in subsequent requests.
 * This class manages that process by maintaining an external store that
 * maps EADNodes to PIDs.
 */
public class ExternalPidResolver {

    private File cache;

    private Map<String, String> map;

    public ExternalPidResolver(File cacheFile) throws IOException {
        this.cache = cacheFile;
        cacheFile.getParentFile().mkdirs();
        this.map = new HashMap<String, String>();
        loadCacheFromDisk();
    }

    public String getPidForNodeReferenceId(String referenceId) {
        return map.get(referenceId);
    }

    public String getPidForNode(EADNode node) {
        return map.get(node.getReferenceId());
    }

    public void storePidForNode(EADNode node, String pid) throws IOException {
        storePidForNodeReferenceId(node.getReferenceId(), pid);
    }

    public void storePidForNodeReferenceId(String referenceId, String pid) throws IOException {
        map.put(referenceId, pid);
        writeCacheToDisk();
    }

    public void removePidForNode(EADNode node) throws IOException {
        map.remove(node.getReferenceId());
        writeCacheToDisk();
    }

    private void writeCacheToDisk() throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cache)));
        try {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                writer.println(entry.getKey() + "," + entry.getValue());
            }
        } finally {
            writer.close();
        }
    }

    private void loadCacheFromDisk() throws IOException {
        map.clear();
        try {
            System.out.println(String.valueOf(cache.exists()) + cache.isFile() + cache.length() + cache.getAbsolutePath());
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cache)));
            try {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    map.put(line.substring(0, line.indexOf(',')), line.substring(line.indexOf(',') + 1));
                }
            } finally {
                reader.close();
            }
        } catch (FileNotFoundException ex) {
            System.out.println("No existing cache file found at \"" + cache.getAbsolutePath() + "\".: a new one will be created...");
            // no cache file, no problem...
        }
    }

}
