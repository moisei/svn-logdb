package com.dalet.svnstats;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static com.dalet.svnstats.SvnAuthors.AUTHORS_GROUP;

/**
 * User: Moisei Rabinovich
 * Date: 2/16/14
 * Time: 4:43 PM
 */
public class SvnlogDbIndexer implements Closeable {
    public static final int MAX_MSG_LENGTH = 1024 * 8;
    public static final int MAX_PATH_LENGTH = 2048;
    public static final Path dbFile = FileSystems.getDefault().getPath(".svnlogDB", "db");

    private SVNRepository svnRepository;
    private Connection sqlConnection;

    public SvnlogDbIndexer(String svnUrl) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SVNException, SQLException, IOException {
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
        String url = "jdbc:hsqldb:" + dbFile.toAbsolutePath().toString();
        System.out.println(url);
        Files.createDirectories(dbFile.getParent());
        sqlConnection = DriverManager.getConnection(url, props);
        sqlConnection.setAutoCommit(false);
        updateIgnoreExisiting("CREATE TABLE COMMITS(Revision BIGINT, Date DATE, Author VARCHAR(100), Team VARCHAR(100), ChangedFilesCount INTEGER, Message VARCHAR(" + MAX_MSG_LENGTH + "))");
        updateIgnoreExisiting("CREATE UNIQUE INDEX  commits_revision ON commits (revision)");
        updateIgnoreExisiting("CREATE TABLE FILES(Revision BIGINT, Type VARCHAR(10), Kind VARCHAR(10), File VARCHAR(" + MAX_PATH_LENGTH + "))");
        updateIgnoreExisiting("CREATE UNIQUE INDEX  files_revision_file ON files (revision,file)");
        updateIgnoreExisiting("CREATE TABLE DATE_TIME(Revision BIGINT, Date DATE, Time TIME, DateTime DATETIME, Year INTEGER, Month INTEGER, Day INTEGER, Week INTEGER, DayOfWeek INTEGER, Hour INTEGER, Minutes INTEGER, Sec INTEGER)");
        updateIgnoreExisiting("CREATE UNIQUE INDEX  date_time_revision ON date_time (revision)");
        updateIgnoreExisiting("CREATE TABLE VERSION(Revision BIGINT, Product VARCHAR(32), Type VARCHAR(10), Branch VARCHAR(1024), ProductVersion VARCHAR(100), FullVersion VARCHAR(100))");
        updateIgnoreExisiting("CREATE UNIQUE INDEX  version_revision ON version (revision)");
        updateIgnoreExisiting("CREATE TABLE ISSUES(Revision BIGINT, Type VARCHAR(10), Reference VARCHAR(100), NotesClientUrl VARCHAR(1024), NotesWebUrl VARCHAR(1024))");
        updateIgnoreExisiting("CREATE UNIQUE INDEX  issues_revision_type_reference ON issues (revision, type, reference)");
        updateIgnoreExisiting("CREATE TABLE AUTHORS(Auhtor VARCHAR(100), Team VARCHAR(100))");
        updateIgnoreExisiting("CREATE UNIQUE INDEX  auhtor ON authors (auhtor)");
    }

    @Override
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

    private void buildIndex(long startRevision, long endRevision) throws SVNException, SQLException, ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
        System.out.println("buildIndex: from " + startRevision + " to " + endRevision);
        try (SvnLogEntryHandler svnLogEntryHandler = new SvnLogEntryHandler(sqlConnection)) {
            svnRepository.log(new String[]{"/"}, startRevision, endRevision, true, false, svnLogEntryHandler);
            sqlConnection.commit();
        }
        fillAuhtors();
        printReport();
    }

    public static void deleteIndex() throws IOException, InterruptedException {
        System.out.println("delete index");
        Path dbParentFolder = dbFile.getParent();
        Runtime.getRuntime().exec(new String[]{"cmd", "/k", "rmdir", "/s", "/q", dbParentFolder.toString()});
        Thread.sleep(1000);
        if (Files.exists(dbParentFolder)) {
            throw new IOException("Can't delete " + dbParentFolder);
        }
    }

    public void updateIndex(long endRevision) throws SQLException, SVNException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        System.out.println("updateIndex: to " + endRevision);
        initHsqldb();
        long maxRevision = getMaxRevision();
        if (SVNRepository.INVALID_REVISION == maxRevision) {
            throw new SQLException("Can't update empty database. It must be indexed at least one time first");
        }
        buildIndex(maxRevision + 1, endRevision);
    }

    public void updateOrRebuildIndex(long startRevision, long endRevision) throws SQLException, SVNException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        initHsqldb();
        long maxRevision = getMaxRevision();
        if (SVNRepository.INVALID_REVISION == maxRevision || maxRevision < startRevision ) {
            buildIndex(startRevision, endRevision);
        } else {
            buildIndex(maxRevision + 1, endRevision);
        }

    }

    public void forceRebuildIndex(long startRevision, long endRevision) throws SQLException, SVNException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, InterruptedException {
        deleteIndex();
        initHsqldb();
        buildIndex(startRevision, endRevision);
    }

    private long getMaxRevision() throws SQLException {
        try (Statement statement = sqlConnection.createStatement()) {
            statement.execute("SELECT MAX(REVISION) FROM COMMITS");
            try (ResultSet resultSet = statement.getResultSet()) {
                if (!resultSet.next()) {
                    return SVNRepository.INVALID_REVISION;
                }
                return resultSet.getLong(1);
            }
        }
    }

    private void updateIgnoreExisiting(String sql) throws SQLException {
        try (Statement statement = sqlConnection.createStatement()) {
            statement.executeUpdate(sql);
            if (!sqlConnection.getAutoCommit()) {
                sqlConnection.commit();
            }
            System.out.println("updateIgnoreExisiting: " + sql);
        } catch (SQLException e) {
            if (!e.getMessage().startsWith("object name already exists") && !e.getMessage().contains("unique constraint")) {
                throw e;
            }
            System.out.println("updateIgnoreExisiting: " + e.getMessage());
        }
    }

    private void fillAuhtors() throws SQLException {
        updateIgnoreExisiting("DELETE FROM AUTHORS");
        InsertStatement insertAuhtors = new InsertStatement("AUTHORS", sqlConnection);
        List<String> authors = new ArrayList<>(AUTHORS_GROUP.keySet());
        Collections.sort(authors);
        for (String author : authors) {
            insertAuhtors.addrow(author, AUTHORS_GROUP.get(author));
        }
        insertAuhtors.close();
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
