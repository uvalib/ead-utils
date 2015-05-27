package edu.virginia.lib.ead;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;

public class InMemoryPidResolver extends ExternalPidResolver {
    public InMemoryPidResolver() throws IOException {
        super();
    }

    protected void writeCacheToDisk() throws IOException {
        // do nothing.
    }

    protected void loadCacheFromDisk() throws IOException {
        // do nothing
    }
}
