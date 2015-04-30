package com.dalet.svnstats;

import org.apache.log4j.BasicConfigurator;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User: Moisei Rabinovich
 * Date: 2/12/14
 * Time: 5:51 PM
 */
public class Main {
    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        long startRevision = initArg(args, 0, 82994);
        long endRevision = initArg(args, 1, SVNRepository.INVALID_REVISION);
        String svnUrl = initArg(args, 3, "svn://gfn-svn:3692");
        rebuildDatabase(startRevision, endRevision, svnUrl);
        Runtime.getRuntime().exec(new String[]{"cmd", "/k", "bin/svnlogdb.bat"});
    }

    private static String initArg(String[] args, int index, String defaultValue) {
        if (args.length < (1 + index)) {
            return defaultValue;
        }
        return args[index];
    }

    private static long initArg(String[] args, int index, long defaultValue) {
        if (args.length < (1 + index)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(args[index]);
        } catch (NumberFormatException e) {
            usage();
            throw e;
        }
    }

    private static void usage() {
        System.out.println("java -jar dalet-svn-stats.jar [<start revision> default 0] [<end revision> default HEAD]");
        System.out.println("Index Dalet svn repo history to SQL database");
    }

    private static void rebuildDatabase(long startRevision, long endRevision, String svnUrl) throws IOException, InterruptedException, ClassNotFoundException, IllegalAccessException, InstantiationException, SVNException, SQLException {
        SvnlogDbIndexer.deleteIndex();
        SvnlogDbIndexer logDbBuilder = new SvnlogDbIndexer(svnUrl);
        logDbBuilder.buildIndex(startRevision, endRevision);
        logDbBuilder.close();
    }
}
