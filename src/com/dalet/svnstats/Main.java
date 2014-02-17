package com.dalet.svnstats;

import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * User: Moisei Rabinovich
 * Date: 2/12/14
 * Time: 5:51 PM
 */
public class Main {
    public static void main(String[] args) throws Exception {
        SvnLogDbBuilder logDbBuilder = new SvnLogDbBuilder("svn://gfn-svn:3692");
        logDbBuilder.svnlog2db(82994, SVNRepository.INVALID_REVISION);
//        logDbBuilder.svnlog2db(152000, SVNRepository.INVALID_REVISION);
//        logDbBuilder.svnlog2db(126991, SVNRepository.INVALID_REVISION);
//        logDbBuilder.svnlog2db(82994, 126990);
        logDbBuilder.close();
    }

}
