package com.dalet.svnstats;

import org.apache.log4j.BasicConfigurator;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * User: Moisei Rabinovich
 * Date: 2/12/14
 * Time: 5:51 PM
 */
public class Main {

    private static final int DEFAULT_START_REVISION = 1;
    private static final long DEFAULT_END_REVISION = SVNRepository.INVALID_REVISION;

    // svn://gfn-svn:3692 updateorbuild
    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        String svnUrl = initArg(args, 0, null);
        String action = initArg(args, 1, null);
        long startRevision = initArg(args, 2, DEFAULT_START_REVISION);
        long endRevision = initArg(args, 3, DEFAULT_END_REVISION);
        try (SvnlogDbIndexer logDbBuilder = new SvnlogDbIndexer(svnUrl)) {
            switch (action.toLowerCase()) {
                case "updateorrebuild": {
                    logDbBuilder.updateOrRebuildIndex(startRevision, endRevision);
                    break;
                }
                case "update": {
                    logDbBuilder.updateIndex(endRevision);
                    break;
                }
                case "forcerebuild": {
                    logDbBuilder.forceRebuildIndex(startRevision, endRevision);
                    break;
                }
                default: {
                    usage();
                    throw new Exception("Unknown action " + action);
                }
            }
        }
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

    private static String initArg(String[] args, int index, String defaultValue) {
        return args.length < (1 + index) ? defaultValue : args[index];
    }

    private static void usage() {
        System.out.println("java -jar dalet-svn-stats.jar <svn url> [action:updateOrRebuild|update|forceRebuild default updateorbuild] [<start revision> default 1] [<end revision> default HEAD]");
        System.out.println("Index svn repo history to SQL database");
    }
}
