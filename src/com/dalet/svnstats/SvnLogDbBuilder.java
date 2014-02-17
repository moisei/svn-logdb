package com.dalet.svnstats;

import org.tmatesoft.svn.core.*;
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
public class SvnLogDbBuilder {

    private static final int MAX_MSG_LENGTH = 1024 * 8;
    private static final int MAX_PATH_LENGTH = 2048;
    private SVNRepository svnRepository;
    private Connection sqlConnection;

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
        sqlConnection = DriverManager.getConnection(url, props);
        sqlConnection.setAutoCommit(false);
        executeStatementIgnoreExisiting("CREATE TABLE COMMITS(Revision BIGINT, Date DATE, Author VARCHAR(100), Message VARCHAR(" + MAX_MSG_LENGTH + "))");
        executeStatementIgnoreExisiting("CREATE TABLE FILES(Revision BIGINT, Type VARCHAR(10), Kind VARCHAR(10), File VARCHAR(" + MAX_PATH_LENGTH + "))");
        executeStatementIgnoreExisiting("CREATE TABLE DATE_TIME(Revision BIGINT, Date DATE, Year INTEGER, Month INTEGER, Day INTEGER, Week INTEGER, DayOfWeek INTEGER, Hour INTEGER, Minutes INTEGER, Sec INTEGER)");
        executeStatementIgnoreExisiting("CREATE TABLE VERSION(Revision BIGINT, Product VARCHAR(32), Type VARCHAR(10), Branch VARCHAR(1024), ProductVersion VARCHAR(100), FullVersion VARCHAR(100), ChangedFiles INTEGER)");
        executeStatementIgnoreExisiting("CREATE TABLE ISSUES(Revision BIGINT, Type VARCHAR(10), Reference VARCHAR(100), NotesClientUrl VARCHAR(1024), NotesWebUrl VARCHAR(1024))");
    }

    void svnlog2db(int startRevision, int endRevision) throws SVNException, SQLException {
        try (SvnLogEntryHandler svnLogEntryHandler = new SvnLogEntryHandler(sqlConnection)) {
            svnRepository.log(new String[]{"/"}, startRevision, endRevision, true, false, svnLogEntryHandler);
            sqlConnection.commit();
        }
        printReport();
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
            if (!e.getMessage().startsWith("object name already exists")) {
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
            statement.execute("SELECT COUNT(1) FROM COMMITS");
            try (ResultSet resultSet = statement.getResultSet()) {
                resultSet.next();
                System.out.println("Number of rows in COMMIITS table is: " + resultSet.getLong(1));
            }
        }
    }

}
