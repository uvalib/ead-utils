package edu.virginia.lib.ead.pidfilters;

import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.EncodedTextMapper;
import edu.virginia.lib.ead.PidFilter;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by md5wz on 4/16/15.
 */
public class HasTeiPidFilter implements PidFilter {

    private EncodedTextMapper m;

    private List<String> includedPids;
    private List<File> includedFiles;

    public HasTeiPidFilter(EncodedTextMapper m) {
        this.m = m;
        includedPids = new ArrayList<String>();
        includedFiles = new ArrayList<File>();
    }

    @Override
    public boolean includePid(String pid, EADNode node) {
        if (node == null) {
            return false;
        }
        try {
            final File tei = m.getTEIForNode(node);
            if (tei != null) {
                includedFiles.add(tei);
                includedPids.add(pid);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void writeReport(OutputStream out) {
        PrintWriter p = new PrintWriter(out);
        p.println("Unused files: ");
        for (File f : m.getAllTeiFiles()) {
            if (!includedFiles.contains(f)) {
                p.println(f.getName());
            }
        }
        p.println("Missing files: ");
        for (String s : m.getAllUnfoundIds()) {
            p.println(s);
        }
        p.flush();

    }
}
