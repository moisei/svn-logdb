package com.dalet.svnstats;

import org.tmatesoft.svn.core.*;

import java.io.Closeable;
import java.sql.*;
import java.sql.Date;
import java.util.*;

import static com.dalet.svnstats.LotusNotesMessageUtils.extractReferencedBugs;
import static com.dalet.svnstats.LotusNotesMessageUtils.extractReferencedFeatures;

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
        long revision = svnLogEntry.getRevision();
        if (0 == (revision % 100)) {
            System.out.println("Handling revision: " + revision);
        }
        try {
            fillCommits(svnLogEntry);
            fillChangedFiles(svnLogEntry);
            fillDateTime(svnLogEntry);
            fillVersion(svnLogEntry);
            fillIssues(svnLogEntry);
        } catch (SQLException e) {
            throw new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e.getMessage()), e);
        }
    }

    // Revision BIGINT, Type VARCHAR(10), Reference VARCHAR(100), NotesClientUrl VARCHAR(1024), NotesWebUrl VARCHAR(1024))
    private void fillIssues(SVNLogEntry svnLogEntry) throws SQLException {
        String message = svnLogEntry.getMessage();
        for (String feature : extractReferencedFeatures(message)) {
            insertIssuesStatement.addrow(svnLogEntry.getRevision(), "F", feature, "", "");
        }
        for (String bug : extractReferencedBugs(message)) {
            insertIssuesStatement.addrow(svnLogEntry.getRevision(), "B", bug, "", "");
        }
    }

    // VERSION(Revision BIGINT, Product VARCHAR(32), Type VARCHAR(10), Branch VARCHAR(1024), ProductVersion VARCHAR(100), FullVersion VARCHAR(100)
    private void fillVersion(SVNLogEntry svnLogEntry) throws SQLException {
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
            if (firstPath.getPath().startsWith("/branches/builds")) {
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
            } else if (firstPath.getPath().startsWith("/branches/hotfixes")) {
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
        String productVersion;
        String[] productVersionTokens = fullVersion.split("\\.");
        if (productVersionTokens.length > 1) {
            productVersion = productVersionTokens[0] + "." + productVersionTokens[1];
        } else {
            System.out.println("*** Warning. revision: " + svnLogEntry.getRevision() + " fullVersion is too short: " + fullVersion + ": " + Arrays.toString(productVersionTokens));
            productVersion = fullVersion;
        }
        return productVersion;
    }

    // Revision BIGINT, Date DATE, Year INTEGER, Month INTEGER, Day INTEGER, Week INTEGER, DayOfWeek INTEGER, Hour INTEGER, Minutes INTEGER, Sec INTEGER;
    private void fillDateTime(SVNLogEntry svnLogEntry) throws SQLException {
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

    private void fillCommits(SVNLogEntry svnLogEntry) throws SQLException {
        String msg;
        if (svnLogEntry.getMessage().length() < SvnlogDbIndexer.MAX_MSG_LENGTH) {
            msg = svnLogEntry.getMessage();
        } else {
            System.out.println("*** Warning. revision: " + svnLogEntry.getRevision() + " message is too long: " + svnLogEntry.getMessage().length());
            msg = svnLogEntry.getMessage().substring(0, SvnlogDbIndexer.MAX_MSG_LENGTH);
        }
        insertCommitsStatement.addrow(svnLogEntry.getRevision(), new Date(svnLogEntry.getDate().getTime()), svnLogEntry.getAuthor(), svnLogEntry.getChangedPaths().size(), msg);
    }

    private void fillChangedFiles(SVNLogEntry svnLogEntry) throws SQLException {
        Map<String, SVNLogEntryPath> changedPaths = svnLogEntry.getChangedPaths();
        for (String path : changedPaths.keySet()) {
            SVNLogEntryPath change = changedPaths.get(path);
            insertFilesStatement.addrow(svnLogEntry.getRevision(), String.valueOf(change.getType()), change.getKind().toString(), change.getPath());
        }
    }

}
