package com.dalet.svnstats;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Map;
import java.util.Properties;

/**
 * User: Moisei Rabinovich
 * Date: 2/16/14
 * Time: 4:43 PM
 */
public class SvnLogDbBuilder {

    private static final int MAX_MSG_LENGTH = 1024 * 8;
    private static final int MAX_PATH_LENGTH = 2048;
    private SVNRepository svnRepository;
    private Connection hsqlConnection;

    public SvnLogDbBuilder(String svnUrl) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SVNException, SQLException, IOException {
        initHsqldb();
        initSvn(svnUrl);
    }

    private void initSvn(String url) throws SVNException {
        DAVRepositoryFactory.setup();
        SVNURL svnUrl = SVNURL.parseURIEncoded(url);
        svnRepository = SVNRepositoryFactory.create(svnUrl);
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager("svc", "gfn");
        svnRepository.setAuthenticationManager(authManager);
    }

    private void initHsqldb() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException {
        Class.forName("org.hsqldb.jdbcDriver").newInstance();
        Properties props = new Properties();
        props.put("user", "sa");
        props.put("password", "");
        props.put("jdbc.strict_md", "false");
        props.put("jdbc.get_column_name", "false");
        String url = "jdbc:hsqldb:" + new File(".svnlogDB/db").getCanonicalPath();
        System.out.println(url);
        hsqlConnection = DriverManager.getConnection(url, props);
        hsqlConnection.setAutoCommit(false);
        executeStatementNoException("CREATE TABLE COMMITS(Revision BIGINT, Date date, Author varchar(100), Message varchar(" + MAX_MSG_LENGTH + "))");
        executeStatementNoException("CREATE TABLE FILES(Revision BIGINT, Type varchar(10), Kind varchar(10), File varchar(" + MAX_PATH_LENGTH + "))");
    }

    void svnlog2db(int startRevision, int endRevision) throws SVNException, SQLException {
        try (SvnLogHandler svnLogHandler = new SvnLogHandler()) {
            svnRepository.log(new String[]{"/"}, startRevision, endRevision, true, false, svnLogHandler);
            hsqlConnection.commit();
        }
        printReport();
    }

    private void fillChangedFiles(SVNLogEntry logEntry, PreparedStatement insertFilesStatement) throws SQLException {
        Map<String, SVNLogEntryPath> changedPaths = logEntry.getChangedPaths();
        for (String path : changedPaths.keySet()) {
            SVNLogEntryPath change = changedPaths.get(path);
            insertFilesStatement.setLong(1, logEntry.getRevision());
            insertFilesStatement.setString(2, String.valueOf(change.getType()));
            insertFilesStatement.setString(3, change.getKind().toString());
            insertFilesStatement.setString(4, change.getPath());
            insertFilesStatement.execute();
        }
    }

    private void fillCommit(SVNLogEntry svnLogEntry, PreparedStatement insertCommitStatement) throws SQLException {
        insertCommitStatement.setLong(1, svnLogEntry.getRevision());
        insertCommitStatement.setDate(2, new Date(svnLogEntry.getDate().getTime()));
        insertCommitStatement.setString(3, svnLogEntry.getAuthor());
        insertCommitStatement.setString(4, svnLogEntry.getMessage());
        insertCommitStatement.execute();
    }

    public void close() {
        try {
            svnRepository.closeSession();
        } catch (Exception ignore) {
        }
        try {
            hsqlConnection.close();
        } catch (Exception ignore) {
        }
        try {
            DriverManager.getConnection("jdbc:Hsql:;shutdown=true");
        } catch (Exception ignore) {
        }
    }

    private void executeStatementNoException(String sql) {
        try {
            executeStatement(sql);
        } catch (SQLException ignore) {
            System.out.println(ignore.getMessage());
            // table already exists
        }
    }

    private void executeStatement(String sql) throws SQLException {
        try (Statement statement = hsqlConnection.createStatement()) {
            statement.execute(sql);
            if (!hsqlConnection.getAutoCommit()) {
                hsqlConnection.commit();
            }
        }
    }

    private void printReport() throws SQLException {
        try (Statement statement = hsqlConnection.createStatement()) {
            statement.execute("SELECT COUNT(1) FROM  COMMITS");
            try (ResultSet resultSet = statement.getResultSet()) {
                resultSet.next();
                System.out.println("Number of rows in COMMIITS table is: " + resultSet.getLong(1));
            }
        }
    }

    private class SvnLogHandler implements ISVNLogEntryHandler, Closeable {
        private final PreparedStatement insertCommitsStatement;
        private final PreparedStatement insertFilesStatement;

        public SvnLogHandler() throws SQLException {
            insertCommitsStatement = hsqlConnection.prepareStatement("INSERT INTO COMMITS VALUES(?, ?, ?, ?)");
            insertFilesStatement = hsqlConnection.prepareStatement("INSERT INTO FILES VALUES(?, ?, ?, ?)");
        }

        @Override
        public void handleLogEntry(SVNLogEntry svnLogEntry) throws SVNException {
            try {
                handleLogEntryUnsafe(svnLogEntry);
            } catch (SQLException e) {
                throw new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e.getMessage()), e);
            }
        }

        private void handleLogEntryUnsafe(SVNLogEntry logEntry) throws SQLException {
            long revision = logEntry.getRevision();
            System.out.println("Handling revision: " + revision);
            fillCommit(logEntry, insertCommitsStatement);
            fillChangedFiles(logEntry, insertFilesStatement);
        }

        @Override
        public void close() {
            try {
                insertCommitsStatement.close();
            } catch (SQLException ignore) {
            }
            try {
                insertFilesStatement.close();
            } catch (SQLException ignore) {
            }
        }
    }
}
