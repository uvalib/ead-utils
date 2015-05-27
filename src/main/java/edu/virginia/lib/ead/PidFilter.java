package edu.virginia.lib.ead;

public interface PidFilter {

    public boolean includePid(String pid, EADNode node);
}
