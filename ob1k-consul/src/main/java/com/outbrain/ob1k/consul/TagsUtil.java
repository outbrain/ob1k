package com.outbrain.ob1k.consul;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * @author Eran Harel
 */
public class TagsUtil {
    public static final String KEY_VALUE_SEPARATOR = "-";

    private TagsUtil() {}
    
    public static String extractTag(final Set<String> tags, final String tagKey) {
        final String tagKeyPrefix = tagKey + KEY_VALUE_SEPARATOR;
        if (null != tags) {
            for (final String serviceTag : tags) {
                if (StringUtils.isNotEmpty(serviceTag) && serviceTag.startsWith(tagKeyPrefix)) {
                    return serviceTag.substring(tagKeyPrefix.length());
                }
            }
        }

        return null;
    }

    public static Integer extractPort(final Set<String> tags, final String portType) {
        final String tag = extractTag(tags, portType + "Port");
        return tag == null ? null : Integer.valueOf(tag);
    }

    public static String extractContextPath(final Set<String> tags) {
        return extractTag(tags, "contextPath");
    }
}
