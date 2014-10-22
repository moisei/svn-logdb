package com.dalet.svnstats;

import org.jdom.Element;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Moisei Rabinovich
 * Date: 7/16/12
 * Time: 4:00 PM
 */
public class RevisionInfoEx implements RevisionInfoIfc {
    public static final String NOTES_ISSUE_REGEXP = "\\#\\d\\d\\d\\d\\d?\\s*-\\s*[A-Z][A-Z]";
    public static final Pattern NOTES_ISSUE_PATTERN = Pattern.compile(NOTES_ISSUE_REGEXP);
    private Element logentryElement;
    private String branch;
    private List<String> files;
    private HashSet<String> bugs;
    private HashSet<String> featues;

    public RevisionInfoEx(Element logentryElement) {
        this.logentryElement = logentryElement;
        initFiles();
        initBranch();
        initIssues();
    }

    private void initFiles() {
        files = new ArrayList<>(10);
        Element pathsElement = logentryElement.getChild("paths");
        if (null == pathsElement) {
            return;
        }
        @SuppressWarnings("unchecked") List<Element> paths = pathsElement.getChildren("path");
        if (null == paths) {
            return;
        }
        for (Element path : paths) {
            files.add(path.getTextTrim().trim());
        }
    }

    @Override
    public String getBranch() {
        return branch;
    }

    public long getRevision() {
        return Long.parseLong(logentryElement.getAttributeValue("revision"));
    }

    @Override
    public String getUser() {
        return logentryElement.getChildTextTrim("author");
    }

    @Override
    public String getDate() {
        return logentryElement.getChildTextTrim("date");
    }

    @Override
    public String getMsg() {
        return logentryElement.getChildTextTrim("msg");
    }

    @Override
    public List<String> getFiles() {
        return files;
    }

    @Override
    public Set<String> getReviewers() {
        return null;
    }

    public Collection<String> getBugs() {
        return bugs;
    }

    public Collection<String> getFeatures() {
        return featues;
    }

    private void initBranch() {
        branch = null;
        if (files.size() < 1) {
            return;
        }
        String firstFile = files.get(0);
        if (firstFile.startsWith("/trunk/DaletNews/Ver 1.4")) {
            branch = "1.4";
        } else if (firstFile.startsWith("/trunk/DaletNews/Ver 1.5")) {
            branch = "1.5";
        } else if (firstFile.startsWith("/trunk/DaletNews/java")) {
            branch = "java";
        } else if (firstFile.startsWith("/branches/builds")) {
            String[] tmp = firstFile.split("/");
            if (tmp.length > 2) {
                branch = tmp[3];
            }
        } else if (firstFile.startsWith("/branches/hotfixes")) {
            String[] tmp = firstFile.split("/");
            if (tmp.length > 3) {
                branch = "HF-" + tmp[3];
            }
        } else if (firstFile.startsWith("/branches/tnt")) {
            branch = "BRIO-";
        //        } else if (firstFile.startsWith("/branches")) {
        //            String[] tmp = firstFile.split("/");
        //            if (tmp.length > 3) {
        //                branch = tmp [2].toUpperCase()+ "-" + tmp[3];
        //            }
        } else {
            branch = null;
        }
    }

    private void initIssues() {
        bugs = new HashSet<>();
        featues = new HashSet<>();
        Matcher m = NOTES_ISSUE_PATTERN.matcher(getMsg());
        while (m.find()) {
            String issue = formatIssue(m.group());
            if (isBug(issue)) {
                bugs.add(issue);
            } else {
                featues.add(issue);
            }
        }
    }

    private boolean isBug(String issue) {
        return issue.length() == "#12345 - AB".length();
    }

    private String formatIssue(String issue) {
        String[] tokens = issue.toUpperCase().split("-");
        return tokens[0].trim() + " - " + tokens[1].trim();
    }

    @Override
    public String toString() {
        return "r" + getRevision();
    }

    public Element toElement() {
        return (Element) logentryElement.clone();
//        Element element = new Element("RevisionInfo");
//        element.setAttribute("date", getDate());
//        element.setAttribute("branch", (getBranch() == null) ? ":" : getBranch());
//        element.setAttribute("revision", String.valueOf(getRevision()));
//        element.setAttribute("author", getUser());
//        Element msgElement = new Element("msg");
//        msgElement.setText(getMsg());
//        element.addContent(msgElement);
//        return element;
    }
}
