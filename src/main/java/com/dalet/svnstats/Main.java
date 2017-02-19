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

   private enum Action {
        updateorrebuild, update, forcerebuild;
    }

    // svn://gfn-svn:3692 updateorbuild
    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        if (args.length < 2) {
            usage();
            return;
        }
        String svnUrl = args[0];
        Action action = initAction(args);
        long startRevision = initArg(args, 2, DEFAULT_START_REVISION);
        long endRevision = initArg(args, 3, DEFAULT_END_REVISION);
        try (SvnlogDbIndexer svnlogDbIndexer = new SvnlogDbIndexer(svnUrl)) {
            switch (action) {
                case updateorrebuild: {
                    svnlogDbIndexer.updateOrRebuildIndex(startRevision, endRevision);
                    break;
                }
                case update: {
                    svnlogDbIndexer.updateIndex(endRevision);
                    break;
                }
                case forcerebuild: {
                    svnlogDbIndexer.forceRebuildIndex(startRevision, endRevision);
                    break;
                }
                default: {
                    usage();
                    throw new Exception("Unknown action " + action.toString());
                }
            }
        }
    }

    private static Action initAction(String[] args) throws Exception {
        Action action;
        try {
            action = Action.valueOf(args[1]);
        } catch (IllegalArgumentException e) {
            usage();
            throw new Exception("Unknown action " + args[1]);
        }
        return action;
    }

    private static long initArg(String[] args, int index, long defaultValue) {
        if (args.length < (1 + index)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(args[index]);
        } catch (NumberFormatException e) {
            usage();
            System.out.println("Wrong " + (index+1) + "-th argument " + args[index] + ". Expected number");
            throw e;
        }
    }

    private static void usage() {
        System.out.println("java -jar dalet-svn-stats.jar <svn url> <action:updateOrRebuild|update|forceRebuild> [<start revision> default 1] [<end revision> default HEAD]");
        System.out.println("Index svn repo history to SQL database");
    }
}
