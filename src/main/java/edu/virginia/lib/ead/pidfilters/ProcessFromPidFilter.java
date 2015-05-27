package edu.virginia.lib.ead.pidfilters;

import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.PidFilter;

public class ProcessFromPidFilter implements PidFilter {

    private String firstToInclude;

    private boolean found;

    public ProcessFromPidFilter(String pid) {
        this.firstToInclude = pid;
        found = false;
    }

    @Override
    public boolean includePid(String pid, EADNode node) {
        if (pid.equals(firstToInclude)) {
            found = true;
        }
        return found;
    }
}
