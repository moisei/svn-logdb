package com.dalet.svnstats;

import org.tmatesoft.svn.core.*;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.*;

import static com.dalet.lotus.LotusNotesIssueReferenceUtils.extractReferencedBugs;
import static com.dalet.lotus.LotusNotesIssueReferenceUtils.extractReferencedFeatures;

/**
 * User: Moisei Rabinovich
 * Date: 2/17/14
 * Time: 6:22 PM
 */
class SvnLogEntryHandler implements ISVNLogEntryHandler, Closeable {
    private final InsertStatement insertCommitsStatement;
    private final InsertStatement insertFilesStatement;
    private final InsertStatement insertDateTimeStatement;
    private final InsertStatement insertVersionStatement;
    private final InsertStatement insertIssuesStatement;
    private DiffsGenerator diffsGenerator;

    public SvnLogEntryHandler(Connection sqlConnection, DiffsGenerator diffsGenerator) throws SQLException {
        try {
            insertCommitsStatement = new InsertStatement("COMMITS", sqlConnection);
            insertFilesStatement = new InsertStatement("FILES", sqlConnection);
            insertDateTimeStatement = new InsertStatement("DATE_TIME", sqlConnection);
            insertVersionStatement = new InsertStatement("VERSION", sqlConnection);
            insertIssuesStatement = new InsertStatement("ISSUES", sqlConnection);
        } catch (SQLException e) {
            close();
            throw e;
        }
        this.diffsGenerator = diffsGenerator;
    }

    @Override
    public void close() {
        closeStatement(insertCommitsStatement);
        closeStatement(insertFilesStatement);
        closeStatement(insertDateTimeStatement);
        closeStatement(insertDateTimeStatement);
        closeStatement(insertIssuesStatement);
    }

    private void closeStatement(InsertStatement statement) {
        if (null != statement) {
            try {
                statement.close();
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public void handleLogEntry(SVNLogEntry svnLogEntry) throws SVNException {
        if (0 == (svnLogEntry.getRevision() % 1000)) {
            System.out.print(String.format("\rHandling revision: %6d", svnLogEntry.getRevision()));
        }
        try {
            new SvnLogEntryHandlerHelper(svnLogEntry).handle();
        } catch (Exception e) {
            throw new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e.getMessage()), e);
        }
    }

    private class SvnLogEntryHandlerHelper {
        private SVNLogEntry svnLogEntry;

        private SvnLogEntryHandlerHelper(SVNLogEntry svnLogEntry) throws SVNException, SQLException {
            this.svnLogEntry = svnLogEntry;
        }

        private void handle() throws Exception {
            fillCommits();
            fillChangedFiles();
            fillDateTime();
            fillVersion();
            fillIssues();
            submitDiff();
        }

        // Revision BIGINT, Type VARCHAR(10), Reference VARCHAR(100), NotesClientUrl VARCHAR(1024), NotesWebUrl VARCHAR(1024))
        private void fillIssues() throws SQLException {
            String message = svnLogEntry.getMessage() == null ? "" : svnLogEntry.getMessage();
            for (String feature : extractReferencedFeatures(message)) {
                insertIssuesStatement.addrow(svnLogEntry.getRevision(), "F", feature, "", "");
            }
            for (String bug : extractReferencedBugs(message)) {
                insertIssuesStatement.addrow(svnLogEntry.getRevision(), "B", bug, "", "");
            }
        }

        // VERSION(Revision BIGINT, Product VARCHAR(32), Type VARCHAR(10), Branch VARCHAR(1024), ProductVersion VARCHAR(100), FullVersion VARCHAR(100)
        private void fillVersion() throws SQLException {
            String product;
            String type;
            String branch;
            String fullVersion;
            String productVersion;
            if (svnLogEntry.getChangedPaths().isEmpty()) {
                product = "other";
                type = "other";
                branch = "other";
                fullVersion = "other";
                productVersion = "other";
            } else {
                String firstPath = svnLogEntry.getChangedPaths().entrySet().iterator().next().getValue().getPath();
                if (firstPath.startsWith("/branches/builds/")) {
                    product = "Dalet";
                    type = "prod";
                    branch = "builds";
                    fullVersion = firstPath.split("/")[3];
                    productVersion = daletProductVersionByFullVersion(svnLogEntry, fullVersion);
                } else if (firstPath.startsWith("/branches/hotfixes/")) {
                    product = "Dalet";
                    type = "prod";
                    branch = "hotfix";
                    fullVersion = firstPath.split("/")[3];
                    productVersion = daletProductVersionByFullVersion(svnLogEntry, fullVersion);
                } else if (firstPath.startsWith("/branches/tnt")) {
                    product = "Italy";
                    type = "prod";
                    branch = "tnt";
                    fullVersion = "other";
                    productVersion = "other";
                } else if (firstPath.startsWith("/amberfin/products/amberfin/branches/amberfin_")) {
                    product = "Amberfin";
                    type = "prod";
                    branch = "branches";
                    fullVersion = firstPath.substring("/amberfin/products/amberfin/branches/amberfin_".length()).split("/")[0];
                    productVersion = fullVersion.split("_")[0];
                } else if (firstPath.startsWith("/amberfin/products/amberfin/trunk")) {
                    product = "Amberfin";
                    type = "other";
                    branch = "trunk";
                    fullVersion = "other";
                    productVersion = "other";
                } else if (firstPath.startsWith("/amberfin/products/amberfin/branches/amberfinkiosk_v")) {
                    product = "AmberfinKiosk";
                    type = "prod";
                    branch = "branches";
                    fullVersion = firstPath.substring("/amberfin/products/amberfin/branches/amberfinkiosk_v".length()).split("/")[0];
                    productVersion = fullVersion.split("_")[0];
                } else if (firstPath.startsWith("/amberfin")) {
                    product = "Amberfin";
                    type = "prod";
                    branch = "amberfin";
                    fullVersion = "other";
                    productVersion = "other";
                } else {
                    product = "other";
                    type = "other";
                    branch = "other";
                    fullVersion = "other";
                    productVersion = "other";
                }
            }
            insertVersionStatement.addrow(
                    svnLogEntry.getRevision(),
                    product,
                    type,
                    branch,
                    productVersion,
                    fullVersion
            );
        }

        private String daletProductVersionByFullVersion(SVNLogEntry svnLogEntry, String fullVersion) {
            String[] productVersionTokens = fullVersion.split("\\.");
            if (productVersionTokens.length > 1) {
                return productVersionTokens[0] + "." + productVersionTokens[1];
            } else {
                System.out.println("*** Warning. revision: " + svnLogEntry.getRevision() + " fullVersion is too short: " + fullVersion + ": " + Arrays.toString(productVersionTokens));
                return fullVersion;
            }
        }

        // Revision BIGINT, Date DATE, Year INTEGER, Month INTEGER, Day INTEGER, Week INTEGER, DayOfWeek INTEGER, Hour INTEGER, Minutes INTEGER, Sec INTEGER;
        private void fillDateTime() throws SQLException {
            Calendar cal = new GregorianCalendar();
            cal.setTime(svnLogEntry.getDate());
            insertDateTimeStatement.addrow(svnLogEntry.getRevision(),
                    cal.getTime(),
                    cal.getTime(),
                    cal.getTime(),
                    cal.get(Calendar.YEAR),
                    1 + cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.WEEK_OF_YEAR),
                    cal.get(Calendar.DAY_OF_WEEK),
                    cal.get(Calendar.HOUR),
                    cal.get(Calendar.MINUTE),
                    cal.get(Calendar.SECOND)
            );
        }

        private void fillCommits() throws SQLException {
            String msg = svnLogEntry.getMessage();
            int cnt;
            if (msg == null) {
                // System.out.println("*** Warning. revision: " + svnLogEntry.getRevision() + " MESSAGE IS NULL ");
                msg = "";
            } else if (msg.length() >= SvnlogDbIndexer.MAX_MSG_LENGTH) {
                System.out.println("*** Warning. revision: " + svnLogEntry.getRevision() + " message is too long: " + msg.length());
                msg = "this log message was too long: " + msg.length();
            } else if ((cnt = countIssues(msg)) >= SvnlogDbIndexer.MAX_MSG_ISSUES) {
                System.out.println("*** Warning. revision: " + svnLogEntry.getRevision() + " message contains too many issues: " + cnt);
                msg = "this log message contained too many issues: " + msg.length();
            } else {
                // msg is OK
            }
            //updateIgnoreExisiting("CREATE TABLE COMMITS(Revision BIGINT, Date DATE, Author VARCHAR(100), Team VARCHAR(100), ChangedFilesCount INTEGER, DiffSize BIGINT, Message VARCHAR(" + MAX_MSG_LENGTH + "))");
            insertCommitsStatement.addrow(svnLogEntry.getRevision(), new Date(svnLogEntry.getDate().getTime()), svnLogEntry.getAuthor(), "", "", svnLogEntry.getChangedPaths().size(), -1L, msg);
        }

        private int countIssues(String msg) {
            return msg.length() - msg.replace("#", "").length();
        }

        private void fillChangedFiles() throws SQLException {
            Map<String, SVNLogEntryPath> changedPaths = svnLogEntry.getChangedPaths();
            for (String path : changedPaths.keySet()) {
                SVNLogEntryPath change = changedPaths.get(path);
                insertFilesStatement.addrow(svnLogEntry.getRevision(), String.valueOf(change.getType()), change.getKind().toString(), change.getPath());
            }
        }

        private void submitDiff() throws Exception {
            diffsGenerator.submitIfNeeded(svnLogEntry);
        }
    }
}
