package com.dalet.lotus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Moisei Rabinovich
 * Date: 2/18/14
 * Time: 4:38 PM
 */
public class LotusNotesIssueReferenceUtils {
    private static List<String> extractIssues(String log, String pattern, int prefixLength) {
        HashSet<String> issues = new HashSet<>();
        Pattern p = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher m = p.matcher(log);
        while (m.find()) {
            String group = m.group();
            if (group.startsWith("#")) {
                issues.add(group);
            } else {
                issues.add(group.substring(prefixLength));
            }
        }
        return new ArrayList<>(issues);
    }

    public static List<String> extractReferencedBugs(String log) {
        return extractIssues(log, "(^|[^\\:])\\#\\d\\d\\d\\d\\d\\s-\\s([A-Z][A-Z])", 1);
    }

    public static List<String> extractReferencedFeatures(String log) {
        return extractIssues(log, "(^|[^\\:])\\#\\d\\d\\d\\d\\s-\\s([A-Z][A-Z])", 1);
    }

}
