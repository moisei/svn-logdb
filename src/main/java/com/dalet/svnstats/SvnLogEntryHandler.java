package com.dalet.svnstats;

import org.tmatesoft.svn.core.*;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

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

    public SvnLogEntryHandler(Connection sqlConnection) throws SQLException {
        insertCommitsStatement = new InsertStatement("COMMITS", sqlConnection);
        insertFilesStatement = new InsertStatement("FILES", sqlConnection);
        insertDateTimeStatement = new InsertStatement("DATE_TIME", sqlConnection);
        insertVersionStatement = new InsertStatement("VERSION", sqlConnection);
        insertIssuesStatement = new InsertStatement("ISSUES", sqlConnection);
    }

    @Override
    public void close() {
        insertCommitsStatement.close();
        insertFilesStatement.close();
        insertDateTimeStatement.close();
        insertDateTimeStatement.close();
        insertIssuesStatement.close();
    }

    @Override
    public void handleLogEntry(SVNLogEntry svnLogEntry) throws SVNException {
        if (0 == (svnLogEntry.getRevision() % 100)) {
            System.out.println("Handling revision: " + svnLogEntry.getRevision());
        }
        try {
            new SvnLogEntryHandlerHelper(svnLogEntry).handle();
        } catch (SQLException e) {
            throw new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e.getMessage()), e);
        }
    }

    private class SvnLogEntryHandlerHelper {
        private SVNLogEntry svnLogEntry;

        private SvnLogEntryHandlerHelper(SVNLogEntry svnLogEntry) throws SVNException, SQLException {
            this.svnLogEntry = svnLogEntry;
        }

        private void handle() throws SQLException {
            fillCommits();
            fillChangedFiles();
            fillDateTime();
            fillVersion();
            fillIssues();
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
                SVNLogEntryPath firstPath = svnLogEntry.getChangedPaths().entrySet().iterator().next().getValue();
                if (firstPath.getPath().startsWith("/branches/builds/")) {
                    product = "Dalet";
                    type = "prod";
                    branch = "builds";
                    fullVersion = firstPath.getPath().split("/")[3];
                    productVersion = productVersionByFullVersion(svnLogEntry, fullVersion);
                } else if (firstPath.getPath().startsWith("/branches/tnt")) {
                    product = "Italy";
                    type = "prod";
                    branch = "tnt";
                    fullVersion = "other";
                    productVersion = "other";
                } else if (firstPath.getPath().startsWith("/branches/amberfin")) {
                    product = "Amberfin";
                    type = "prod";
                    branch = "amberfin";
                    fullVersion = "other";
                    productVersion = "other";
                } else if (firstPath.getPath().startsWith("/branches/hotfixes/")) {
                    product = "Dalet";
                    type = "prod";
                    branch = "hotfix";
                    fullVersion = firstPath.getPath().split("/")[3];
                    productVersion = productVersionByFullVersion(svnLogEntry, fullVersion);
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

        private String productVersionByFullVersion(SVNLogEntry svnLogEntry, String fullVersion) {
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
                System.out.println("*** Warning. revision: " + svnLogEntry.getRevision() + " MESSAGE IS NULL ");
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
            String team = SvnAuthors.getTeam(svnLogEntry.getAuthor());
            insertCommitsStatement.addrow(svnLogEntry.getRevision(), new Date(svnLogEntry.getDate().getTime()), svnLogEntry.getAuthor(), team, svnLogEntry.getChangedPaths().size(), msg);
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
    }

}
