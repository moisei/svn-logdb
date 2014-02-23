package com.dalet.svnstats;

import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * User: Moisei Rabinovich
 * Date: 2/12/14
 * Time: 5:51 PM
 */
public class Main {
    public static void main(String[] args) throws Exception {
        SvnlogDbIndexer.deleteIndex();
        SvnlogDbIndexer logDbBuilder = new SvnlogDbIndexer("svn://gfn-svn:3692");
//        logDbBuilder.buildIndex(82994, SVNRepository.INVALID_REVISION);
//        logDbBuilder.buildIndex(152000, SVNRepository.INVALID_REVISION);
//        logDbBuilder.buildIndex(126991, SVNRepository.INVALID_REVISION);
//        logDbBuilder.buildIndex(82994, 126990);
        logDbBuilder.buildIndex(82994, SVNRepository.INVALID_REVISION);
        logDbBuilder.close();
        Runtime.getRuntime().exec(new String[]{"cmd", "/k", "svnlogdb.bat"});
    }
}
