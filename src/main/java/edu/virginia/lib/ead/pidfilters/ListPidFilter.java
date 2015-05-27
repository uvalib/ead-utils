package edu.virginia.lib.ead.pidfilters;

import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.PidFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Created by md5wz on 4/22/15.
 */
public class ListPidFilter implements PidFilter {

    private List<String> pids = Arrays.asList(new String[] { "uva-lib:2222008", "uva-lib:2509122", "uva-lib:2231333"} );

    @Override
    public boolean includePid(String pid, EADNode node) {
        return false;
    }
}
