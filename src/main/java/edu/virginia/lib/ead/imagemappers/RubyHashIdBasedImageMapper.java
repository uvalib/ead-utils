package edu.virginia.lib.ead.imagemappers;

import edu.virginia.lib.ead.EADNode;
import edu.virginia.lib.ead.ImageMapper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RubyHashIdBasedImageMapper implements ImageMapper {

    private Map<String, List<String>> map;

    public RubyHashIdBasedImageMapper(InputStream is) throws IOException {
        this(IOUtils.toString(is, "UTF-8"));
    }

    public RubyHashIdBasedImageMapper(String serialization) {
        map = new HashMap<String, List<String>>();

        Pattern groupPattern = Pattern.compile("\\Q{\"\\E([^\\}]*?)\\Q\"=>\\E\\s*\\Q[\\E((\"[^\\}]*?\"(, )?)*)\\Q]}\\E", (Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        Pattern valuePattern = Pattern.compile("\"(.*?)\"");

        Matcher hashes = groupPattern.matcher(serialization);
        while (hashes.find()) {
            String key = hashes.group(1);
            String list = hashes.group(2);
            List<String> valueList = new ArrayList<String>();
            if (list != null) {
                Matcher values = valuePattern.matcher(list);
                while (values.find()) {
                    valueList.add(values.group(1));
                }
            }
            map.put(key,  valueList);
        }
    }

    @Override
    public List<String> getImagePids(EADNode node) {
        return map.get(node.getReferenceId());
    }

    @Override
    public String getExemplarPid(EADNode node) {
        final List<String> images = getImagePids(node);
        if (images != null && !images.isEmpty()) {
            return images.get(0);
        }
        return null;
    }
}
