package com.dalet.svnstats;

/**
 * User: Moisei Rabinovich
 * Date: 2/12/14
 * Time: 5:51 PM
 */
public class Main {
    public static void main(String[] args) throws Exception {
        SvnLogDbBuilder logDbBuilder = new SvnLogDbBuilder("svn://gfn-svn:3692/branches");
        logDbBuilder.svnlog2db(150000, 150100);
        logDbBuilder.close();
    }

}
