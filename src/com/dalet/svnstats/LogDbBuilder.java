package com.dalet.svnstats;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.sql.*;
import java.util.Properties;

/**
 * User: Moisei Rabinovich
 * Date: 2/16/14
 * Time: 4:43 PM
 */
public class LogDbBuilder {

    public String framework = "embedded";
    public String driver = "org.hsqldb.jdbcDriver";
    public String protocol = "jdbc:hsqldb:";
    private SVNRepository svnRepository;
    private Connection hsqlConnection;

    public LogDbBuilder(String svnUrl) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SVNException, SQLException {
        initHsqldb();
        initSvn(svnUrl);
    }

    private void initSvn(String svnUrl) throws SVNException {
        DAVRepositoryFactory.setup();
        svnRepository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(svnUrl));
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager("svc", "gfn");
        svnRepository.setAuthenticationManager(authManager);
    }

    private void initHsqldb() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
        Class.forName(driver).newInstance();
        Properties props = new Properties();
        props.put("jdbc.strict_md", "false");
        props.put("jdbc.get_column_name", "false");
        hsqlConnection = DriverManager.getConnection(protocol + "svnLogDb;create=true", props);
        hsqlConnection.setAutoCommit(false);
        executeStatement("CREATE TABLE Commits(Revision int, Date varchar(100), Author varchar(100), Message varchar(1024))");
    }

    private void executeStatement(String sql) throws SQLException {
        try (Statement statement = hsqlConnection.createStatement()) {
            statement.execute(sql);
            hsqlConnection.commit();
        }
    }

    void svnlog2db(int startRevision, int endRevision) throws SVNException {
        ISVNLogEntryHandler handler = new ISVNLogEntryHandler() {
            @Override
            public void handleLogEntry(SVNLogEntry svnLogEntry) throws SVNException {
                try {
                    handleLogEntgry(svnLogEntry);
                } catch (SQLException e) {
                    throw new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e.getMessage()), e);
                }
            }
        };
        svnRepository.log(new String[]{"/"}, startRevision, endRevision, true, false, handler);
    }

    private void handleLogEntgry(SVNLogEntry svnLogEntry) throws SQLException {
//        System.out.println("Handling revision: " + svnLogEntry);
        executeStatement("INSERT INTO COMMITS VALUES(1, '2', '3', '4')");
        try (Statement statement = hsqlConnection.createStatement()) {
            statement.execute("SELECT * FROM COMMITS");
            ResultSet resultSet = statement.getResultSet();
            while (resultSet.next()) {
                System.out.println(resultSet.getString(1));
            }
        }
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
    }
}
