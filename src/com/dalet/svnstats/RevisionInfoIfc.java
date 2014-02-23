package com.dalet.svnstats;

import java.util.List;
import java.util.Set;

/**
 * User: Moisei Rabinovich
 * Date: 7/16/12
 * Time: 4:17 PM
 */
public interface RevisionInfoIfc {
    String getBranch();

    long getRevision();

    String getUser();

    String getDate();

    String getMsg();

    List<String> getFiles();

    Set<String> getReviewers();
}
