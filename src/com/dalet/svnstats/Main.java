package com.dalet.svnstats;

import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;
import java.io.IOException;

/**
 * User: Moisei Rabinovich
 * Date: 2/12/14
 * Time: 5:51 PM
 */
public class Main {
    public static void main(String[] args) throws Exception {
        String dbpath = "C:\\Sources\\DaletAddons\\Svn-Statistics\\.svnlogDB";
        File dbfile = new File(dbpath);
        Runtime.getRuntime().exec(new String[]{"cmd", "/k", "rmdir", "/s", "/q", dbfile.getCanonicalPath()});
        Thread.sleep(1000);
        if (dbfile.exists()) {
            throw new IOException("Can't delete " + dbpath);
        }
        SvnlogDbIndexer logDbBuilder = new SvnlogDbIndexer("svn://gfn-svn:3692");
        logDbBuilder.svnlog2db(82994, SVNRepository.INVALID_REVISION);
//        logDbBuilder.svnlog2db(152000, SVNRepository.INVALID_REVISION);
//        logDbBuilder.svnlog2db(126991, SVNRepository.INVALID_REVISION);
//        logDbBuilder.svnlog2db(82994, 126990);
        logDbBuilder.close();
        Runtime.getRuntime().exec(new String[]{"cmd", "/k", "C:\\Sources\\DaletAddons\\Svn-Statistics\\svnlogdb.bat"});
    }
}
