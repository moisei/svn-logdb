package com.dalet.lotus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Moisei
 * Date: 8/12/2015
 * Time: 7:26 PM
 */
public class LotusNotesIssueReferenceUtils {

    private static final String ISSUE_PATTERN_STR = "(^|[^\\:])\\#\\d\\d\\d\\d\\d?\\d?\\s-\\s([A-Z][A-Z])";
    public static final Pattern ISSUE_PATTERN = Pattern.compile(ISSUE_PATTERN_STR, Pattern.DOTALL);
    public static final IssueTypeMatcherIfc BUG_TYPE_MATCHER = LotusNotesIssueReferenceUtils::isBug;
    public static final IssueTypeMatcherIfc FEATURE_TYPE_MATCHER = LotusNotesIssueReferenceUtils::isFeature;

    private static List<String> extractIssues(String log, IssueTypeMatcherIfc issueTypeMatcher) {
        HashSet<String> issues = new HashSet<>();
        Matcher issuePatternMatcher = ISSUE_PATTERN.matcher(log);
        while (issuePatternMatcher.find()) {
            String group = issuePatternMatcher.group();
            String issue = group.startsWith("#") ? group : group.substring(1);
            if (issueTypeMatcher.matches(issue)) {
                issues.add(issue);
            }
        }
        return new ArrayList<>(issues);
    }

    public static List<String> extractReferencedBugs(String log) {
        return extractIssues(log, BUG_TYPE_MATCHER);
    }

    public static List<String> extractReferencedFeatures(String log) {
        return extractIssues(log, FEATURE_TYPE_MATCHER);
    }

    public static boolean isBug(String issue) {
        if (issue.length() < "#12345 - AB".length()) {
            return false;
        }
        //noinspection SimplifiableIfStatement
        if (issue.length() > "#12345 - AB".length()) {
            return true;
        }
        return Long.parseLong(issue.substring(1, 6)) > 30000;
    }

    public static boolean isFeature(String issue) {
        return !isBug(issue);
    }

    public interface IssueTypeMatcherIfc {
        boolean matches(String issue);
    }
}
