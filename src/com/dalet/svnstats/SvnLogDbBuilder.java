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
import java.util.ArrayList;
import java.util.List;
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
//        executeStatementIgnoreExisiting("CREATE TABLE DATE_TIME(Revision BIGINT, Date date, Year int(5), Month int(2), Day int(2), Week int(2)), Hour int(2), Min int(2), Sec int(2)");
//        executeStatementIgnoreExisiting("CREATE TABLE ISSUES(Revision BIGINT, Type VARCHAR(10), Reference VARCHAR(100), NotesClientUrl VARCHAR(1024), NotesWebUrl VARCHAR(1024)");
//        executeStatementIgnoreExisiting("CREATE TABLE VERSION(Revision BIGINT, Type VARCHAR(10), Branch VARCHAR(1024), ProductVersion VARCHAR(100), FullVersion VARCHAR(100)");
    }

    void svnlog2db(int startRevision, int endRevision) throws SVNException, SQLException {
        try (SvnLogHandler svnLogHandler = new SvnLogHandler()) {
            svnRepository.log(new String[]{"/"}, startRevision, endRevision, true, false, svnLogHandler);
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
            if (! e.getMessage().startsWith("object name already exists")) {
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

    private class SvnLogHandler implements ISVNLogEntryHandler, Closeable {
        private final InsertStatement insertCommitsStatement;
        private final InsertStatement insertFilesStatement;

        public SvnLogHandler() throws SQLException {
            insertCommitsStatement = new InsertStatement("COMMITS", sqlConnection);
            insertFilesStatement = new InsertStatement("FILES", sqlConnection);
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
            System.out.println("Handling revision: " + logEntry.getRevision());
            fillCommits(logEntry);
            fillChangedFiles(logEntry);
        }

        private void fillCommits(SVNLogEntry logEntry) throws SQLException {
            insertCommitsStatement.addrow(logEntry.getRevision(), new Date(logEntry.getDate().getTime()), logEntry.getAuthor(), logEntry.getMessage());
        }

        private void fillChangedFiles(SVNLogEntry logEntry) throws SQLException {
            Map<String, SVNLogEntryPath> changedPaths = logEntry.getChangedPaths();
            for (String path : changedPaths.keySet()) {
                SVNLogEntryPath change = changedPaths.get(path);
                insertFilesStatement.addrow(logEntry.getRevision(), String.valueOf(change.getType()), change.getKind().toString(), change.getPath());
            }
        }

        @Override
        public void close() {
            insertCommitsStatement.close();
            insertFilesStatement.close();
        }
    }

    private class InsertStatement implements Closeable {
        private final PreparedStatement statement;
        private final List<String> types;

        public InsertStatement(String table, Connection sqlConnection) throws SQLException {
            DatabaseMetaData meta = sqlConnection.getMetaData();
            ResultSet rsColumns = meta.getColumns(null, null, table, null);
            types = new ArrayList<>();
            while (rsColumns.next()) {
                types.add(rsColumns.getString("TYPE_NAME").toUpperCase());
            }
            String sql = String.format(String.format("%%0%dd", types.size()), 0).replace("0", "?, ");
            sql = sql.substring(0, sql.length() - 2);
            sql = "INSERT INTO " + table + " VALUES(" + sql + ")";
            statement = sqlConnection.prepareStatement(sql);
        }

        public void addrow(Object... parameters) throws SQLException {
            for (int i = 0; i < parameters.length; ++i) {
                String typeName = types.get(i);
                Object parameter = parameters[i];
                int idx = i + 1;
                switch (typeName) {
                    case "BIGINT":
                        statement.setLong(idx, (Long) parameter);
                        break;
                    case "VARCHAR":
                        statement.setString(idx, (String) parameter);
                        break;
                    case "DATE":
                        statement.setDate(idx, (Date) parameter);
                        break;
                    default:
                        throw new RuntimeException("Unknown column type: " + typeName);
                }
            }
            statement.executeUpdate();
        }

        @Override
        public void close() {
            try {
                statement.close();
            } catch (SQLException ignore) {
            }
        }
    }
}
