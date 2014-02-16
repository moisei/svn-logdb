package com.dalet.svnstats;

import org.tmatesoft.svn.core.SVNException;

import java.sql.SQLException;

/**
 * User: Moisei Rabinovich
 * Date: 2/12/14
 * Time: 5:51 PM
 */
public class Main {
    public static void main(String[] args) throws SVNException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        new LogDbBuilder("svn://gfn-svn:3692/branches").svnlog2db(150000, 150000);
    }

}
