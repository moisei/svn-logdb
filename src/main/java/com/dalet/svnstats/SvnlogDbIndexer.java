package com.dalet.svnstats;

import org.hsqldb.lib.StopWatch;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
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
import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.tmatesoft.svn.core.io.SVNRepository.INVALID_REVISION;

/**
 * User: Moisei Rabinovich
 * Date: 2/16/14
 * Time: 4:43 PM
 */
public class SvnlogDbIndexer implements Closeable {
    public static final int MAX_MSG_LENGTH = 1024 * 100;
    public static final int MAX_PATH_LENGTH = 2048;
    public static final Path dbFile = FileSystems.getDefault().getPath(".svnlogDB", "db");
    public static final int MAX_MSG_ISSUES = 1000;
    private final DiffsGenerator diffsGenerator;

    private SVNRepository svnRepository;
    private Connection sqlConnection;
    private long maxRevisionInRepo;

    public SvnlogDbIndexer(String svnUrl) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SVNException, SQLException, IOException {
        SVNURL url = SVNURL.parseURIEncoded(svnUrl);
        initSvn(url);
        diffsGenerator = new DiffsGenerator(url, 10, "diffs");
    }

    private void initSvn(SVNURL svnUrl) throws SVNException {
        DAVRepositoryFactory.setup();
        svnRepository = SVNRepositoryFactory.create(svnUrl);
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager("svc", "gfn");
        svnRepository.setAuthenticationManager(authManager);
        maxRevisionInRepo = svnRepository.info(".", -1).getRevision();
    }

    private void initHsqldb() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException {
        initSqlConnection();
        createTables();
    }

    private void createTables() throws SQLException {
        updateIgnoreExisiting("CREATE TABLE COMMITS(Revision BIGINT, Date DATE, Author VARCHAR(100), Team VARCHAR(100), Role VARCHAR(10), ChangedFilesCount INTEGER, DiffSize BIGINT, Message VARCHAR(" + MAX_MSG_LENGTH + "))");
        updateIgnoreExisiting("CREATE UNIQUE INDEX  commits_revision ON commits (revision)");
        updateIgnoreExisiting("CREATE TABLE FILES(Revision BIGINT, Type VARCHAR(10), Kind VARCHAR(10), File VARCHAR(" + MAX_PATH_LENGTH + "))");
        updateIgnoreExisiting("CREATE UNIQUE INDEX  files_revision_file ON files (revision,file)");
        updateIgnoreExisiting("CREATE UNIQUE INDEX  files_file_revision ON files (file, revision)");
        updateIgnoreExisiting("CREATE INDEX  files_file ON files (file)");
        updateIgnoreExisiting("CREATE TABLE DATE_TIME(Revision BIGINT, Date DATE, Time TIME, DateTime DATETIME, Year INTEGER, Month INTEGER, Day INTEGER, Week INTEGER, DayOfWeek INTEGER, Hour INTEGER, Minutes INTEGER, Sec INTEGER)");
        updateIgnoreExisiting("CREATE UNIQUE INDEX  date_time_revision ON date_time (revision)");
        updateIgnoreExisiting("CREATE TABLE VERSION(Revision BIGINT, Product VARCHAR(32), Type VARCHAR(10), Branch VARCHAR(1024), ProductVersion VARCHAR(100), FullVersion VARCHAR(100))");
        updateIgnoreExisiting("CREATE UNIQUE INDEX  version_revision ON version (revision)");
        updateIgnoreExisiting("CREATE TABLE ISSUES(Revision BIGINT, Type VARCHAR(10), Reference VARCHAR(100), NotesClientUrl VARCHAR(1024), NotesWebUrl VARCHAR(1024))");
        updateIgnoreExisiting("CREATE UNIQUE INDEX  issues_revision_type_reference ON issues (revision, type, reference)");
    }

    private void initSqlConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException {
        Class.forName("org.hsqldb.jdbcDriver").newInstance();
        Properties props = new Properties();
        props.put("user", "sa");
        props.put("password", "");
        props.put("jdbc.strict_md", "false");
        props.put("jdbc.get_column_name", "false");
        String url = "jdbc:hsqldb:" + dbFile.toAbsolutePath().toString();
        System.out.println("Connecting to db: " + url);
        Files.createDirectories(dbFile.getParent());
        sqlConnection = DriverManager.getConnection(url, props);
        sqlConnection.setAutoCommit(false);
        System.out.println("Connected to db");
    }

    @Override
    public void close() {
        closeSvn();
        closeSql();
        diffsGenerator.close();
    }

    private void closeSql() {
        try {
            sqlConnection.commit();
        } catch (Exception ignore) {
        }
        try {
            sqlConnection.close();
        } catch (Exception ignore) {
        }
    }

    private void closeSvn() {
        try {
            svnRepository.closeSession();
        } catch (Exception ignore) {
        }
    }

    public void updateDiffs(long startRevision, long endRevision) throws Exception {
        initHsqldb();
        if (INVALID_REVISION == endRevision) {
            endRevision = getMaxRevisionInDB();
        }
        System.out.println("updateDiffs: from " + startRevision + " to " + endRevision);
        diffsGenerator.setExpectedDiffsCount(endRevision - startRevision + 1);
        svnRepository.log(new String[]{"/"}, startRevision, endRevision, true, false, svnLogEntry -> {
            try {
                diffsGenerator.submitIfNeeded(svnLogEntry);
            } catch (Exception e) {
                throw new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e.getMessage()), e);
            }
        });
        generateDiffs(startRevision, endRevision);
        printReport();
    }

    private void buildIndex(long startRevision, long endRevision) throws Exception {
        if (INVALID_REVISION == endRevision) {
            endRevision = maxRevisionInRepo;
        }
        System.out.println("buildIndex: from " + startRevision + " to " + endRevision);
        try (SvnLogEntryHandler svnLogEntryHandler = new SvnLogEntryHandler(sqlConnection, diffsGenerator)) {
            svnRepository.log(new String[]{"/"}, startRevision, endRevision, true, false, svnLogEntryHandler);
        }
        System.out.println();
        fillAuhtors();
        System.out.println("revision info is finished. waiting for diff generation");
        generateDiffs(startRevision, endRevision);
        System.out.println("Diff is finished");
        printReport();
    }

    private void fillAuhtors() throws IOException, SQLException {
        Path authorsFile = Paths.get("user_group.sql");
        if (!Files.isRegularFile(authorsFile)) {
            // for debug
            authorsFile = Paths.get("src/client-scripts/user_group.sql");
        }
        String sql = new String(Files.readAllBytes(authorsFile));
        updateSql(sql);
    }

    public void generateDiffs(long fromRev, long toRev) throws Exception {
        diffsGenerator.waitForTermination(8, TimeUnit.HOURS);
        Path diffDir = Paths.get("diffs");
        if (!Files.isDirectory(diffDir)) {
            System.err.println(diffDir.toString() + " doesn't exist!");
            return;
        }
        System.out.println("Updating diff sizes in the datbase from: " + diffDir.toAbsolutePath().toString());
        printDiffs(fromRev, toRev);
        for (long rev = fromRev; rev <= toRev; ++rev) {
            Path diffPath = Paths.get(diffDir.toString(), rev + ".diff");
            long size;
            if (!Files.isRegularFile(diffPath)) {
                size = -1;
                // System.err.println(diffPath.toAbsolutePath().toString() + " doesnt exist");
            } else {
                try {
                    size = Files.size(diffPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    size = -2;
                }
            }
            String sql = String.format("update commits set DiffSize=%d where Revision=%d", size, rev);
            updateSql(sql);
        }
    }

    private void printDiffs(long fromRev, long toRev) throws SQLException {
        System.out.println("Printing diffs for revs " + fromRev + " and " + toRev);
        try (Statement statement = sqlConnection.createStatement()) {
            statement.execute("SELECT * from commits where revision = " + fromRev + " OR " + " revision = " + toRev);
            try (ResultSet resultSet = statement.getResultSet()) {
                while (resultSet.next()) {
                    System.out.println(resultSet.getString(1) + ": " + resultSet.getString(6));
                }
            }
        }
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

    public void updateIndex(long endRevision) throws Exception {
        if (INVALID_REVISION == endRevision) {
            endRevision = maxRevisionInRepo;
        }
        System.out.println("updateIndex: to " + endRevision);
        initHsqldb();
        long maxRevision = getMaxRevisionInDB();
        if (INVALID_REVISION == maxRevision) {
            throw new SQLException("Can't update empty database. It must be indexed at least one time first");
        }
        buildIndex(maxRevision + 1, endRevision);
    }

    public void updateOrRebuildIndex(long startRevision, long endRevision) throws Exception {
        initHsqldb();
        long maxRevisionInDB = getMaxRevisionInDB();
        if (INVALID_REVISION == endRevision) {
            endRevision = maxRevisionInRepo;
        }
        if (INVALID_REVISION == maxRevisionInDB || maxRevisionInDB < startRevision) {
            buildIndex(startRevision, endRevision);
            return;
        }
        if (maxRevisionInDB < maxRevisionInRepo) {
            buildIndex(maxRevisionInDB + 1, endRevision);
            return;
        }
        if (maxRevisionInDB == maxRevisionInRepo) {
            System.out.println("Index is up to date. max revision is " + maxRevisionInDB);
            return;
        }
        System.err.printf("Index is corrupted, or something went wrong: max revision in DB %d is greater than max revision in repo %d%n", maxRevisionInDB, maxRevisionInRepo);

    }

    public void forceRebuildIndex(long startRevision, long endRevision) throws Exception {
        deleteIndex();
        initHsqldb();
        buildIndex(startRevision, endRevision);
    }

    private long getMaxRevisionInDB() throws SQLException {
        try (Statement statement = sqlConnection.createStatement()) {
            statement.execute("SELECT MAX(REVISION) FROM COMMITS");
            try (ResultSet resultSet = statement.getResultSet()) {
                if (!resultSet.next()) {
                    return INVALID_REVISION;
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

    private void updateSql(String sql) throws SQLException {
        try (Statement statement = sqlConnection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void printReport() throws SQLException {
        printDiffs(getMaxRevisionInDB(), maxRevisionInRepo);
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

    public void checkpoint() throws ClassNotFoundException, IOException, InstantiationException, SQLException, IllegalAccessException {
        initSqlConnection();
        try (Statement statement = sqlConnection.createStatement()) {
            System.out.println("Executing checkpoint operation to normalize DB and avoind long delays on first connection to DB.");
            System.out.println("This operation can take very long time. 15-20 minutes on large DB");
            StopWatch sw = new StopWatch(true);
            statement.execute("CHECKPOINT DEFRAG");
            sw.stop();
            System.out.println(sw.currentElapsedTimeToMessage("Finished checkpoint"));
        }
    }
}

