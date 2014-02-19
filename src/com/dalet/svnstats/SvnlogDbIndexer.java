package com.dalet.svnstats;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

/**
 * User: Moisei Rabinovich
 * Date: 2/16/14
 * Time: 4:43 PM
 */
public class SvnlogDbIndexer {

    public static final int MAX_MSG_LENGTH = 1024 * 8;
    public static final int MAX_PATH_LENGTH = 2048;
    private SVNRepository svnRepository;
    private Connection sqlConnection;
    private File dbfolder;

    public SvnlogDbIndexer(String svnUrl) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SVNException, SQLException, IOException {
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
        dbfolder = new File(".svnlogDB/db").getCanonicalFile();
        String url = "jdbc:hsqldb:" + dbfolder.getPath();
        System.out.println(url);
        sqlConnection = DriverManager.getConnection(url, props);
        sqlConnection.setAutoCommit(false);
        executeStatementIgnoreExisiting("CREATE TABLE COMMITS(Revision BIGINT, Date DATE, Author VARCHAR(100), ChangedFilesCount INTEGER, Message VARCHAR(" + MAX_MSG_LENGTH + "))");
        executeStatementIgnoreExisiting("CREATE UNIQUE INDEX  commits_revision ON commits (revision)");
        executeStatementIgnoreExisiting("CREATE TABLE FILES(Revision BIGINT, Type VARCHAR(10), Kind VARCHAR(10), File VARCHAR(" + MAX_PATH_LENGTH + "))");
        executeStatementIgnoreExisiting("CREATE UNIQUE INDEX  files_revision_file ON files (revision,file)");
        executeStatementIgnoreExisiting("CREATE TABLE DATE_TIME(Revision BIGINT, Date DATE, Time TIME, DateTime DATETIME, Year INTEGER, Month INTEGER, Day INTEGER, Week INTEGER, DayOfWeek INTEGER, Hour INTEGER, Minutes INTEGER, Sec INTEGER)");
        executeStatementIgnoreExisiting("CREATE UNIQUE INDEX  date_time_revision ON date_time (revision)");
        executeStatementIgnoreExisiting("CREATE TABLE VERSION(Revision BIGINT, Product VARCHAR(32), Type VARCHAR(10), Branch VARCHAR(1024), ProductVersion VARCHAR(100), FullVersion VARCHAR(100))");
        executeStatementIgnoreExisiting("CREATE UNIQUE INDEX  version_revision ON version (revision)");
        executeStatementIgnoreExisiting("CREATE TABLE ISSUES(Revision BIGINT, Type VARCHAR(10), Reference VARCHAR(100), NotesClientUrl VARCHAR(1024), NotesWebUrl VARCHAR(1024))");
        executeStatementIgnoreExisiting("CREATE UNIQUE INDEX  issues_revision_type_reference ON issues (revision, type, reference)");
    }

    void buildIndex(long startRevision, long endRevision) throws SVNException, SQLException {
        try (SvnLogEntryHandler svnLogEntryHandler = new SvnLogEntryHandler(sqlConnection)) {
            svnRepository.log(new String[]{"/"}, startRevision, endRevision, true, false, svnLogEntryHandler);
            sqlConnection.commit();
        }
        printReport();
    }

    void rebuildIndex(long startRevision, long endRevision) throws SVNException, SQLException, InterruptedException, IOException {
        Runtime.getRuntime().exec(new String[]{"cmd", "/k", "rmdir", "/s", "/q", dbfolder.getPath()});
        Thread.sleep(1000);
        if (dbfolder.exists()) {
            throw new IOException("Can't delete " + dbfolder);
        }
        buildIndex(startRevision, endRevision);
    }

    public void updateIndex() throws SQLException, SVNException {
        try (Statement statement = sqlConnection.createStatement()) {
            statement.execute("SELECT MAX(REVISION) FROM COMMITS");
            try (ResultSet resultSet = statement.getResultSet()) {
                if (!resultSet.next()) {
                    throw new SQLException("Can't update empty database. It must be indexed at least omne time first");
                }
                long maxRevision = resultSet.getLong(1);
                buildIndex(maxRevision + 1, SVNRepository.INVALID_REVISION);
            }
        }
    }

    public void close() {
        try {
            svnRepository.closeSession();
        } catch (Exception ignore) {
        }
        try {
            sqlConnection.close();
        } catch (Exception ignore) {
        }
        try {
            DriverManager.getConnection("jdbc:Hsql:;shutdown=true");
        } catch (Exception ignore) {
        }
    }

    private void executeStatementIgnoreExisiting(String sql) throws SQLException {
        try {
            executeStatement(sql);
        } catch (SQLException e) {
            if (!e.getMessage().startsWith("object name already exists") && !e.getMessage().contains("unique constraint")) {
                throw e;
            }
            System.out.println("executeStatementIgnoreExisiting: " + e.getMessage());
        }
    }

    private void executeStatement(String sql) throws SQLException {
        try (Statement statement = sqlConnection.createStatement()) {
            statement.execute(sql);
            if (!sqlConnection.getAutoCommit()) {
                sqlConnection.commit();
            }
        }
    }

    private void printReport() throws SQLException {
        try (Statement statement = sqlConnection.createStatement()) {
            statement.execute("SELECT COUNT(1) as lines_cnt, min(revision) as min_rev, max(revision) as max_rev, max(revision)-min(revision) as max_min FROM COMMITS");
            try (ResultSet resultSet = statement.getResultSet()) {
                int columnCount = resultSet.getMetaData().getColumnCount();
                resultSet.next();
                for (int i = 1; i < columnCount + 1; i++) {
                    System.out.print(resultSet.getMetaData().getColumnName(i) + " " + resultSet.getInt(i) + ", ");
                }
                System.out.println();
            }
        }
    }
}
