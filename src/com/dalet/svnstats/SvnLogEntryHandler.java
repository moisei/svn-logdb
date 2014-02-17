package com.dalet.svnstats;

import org.tmatesoft.svn.core.*;

import java.io.Closeable;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * User: Moisei Rabinovich
 * Date: 2/17/14
 * Time: 6:22 PM
 */
class SvnLogEntryHandler implements ISVNLogEntryHandler, Closeable {
    private final InsertStatement insertCommitsStatement;
    private final InsertStatement insertFilesStatement;
    private final InsertStatement insertDateTimeStatement;

    public SvnLogEntryHandler(Connection sqlConnection) throws SQLException {
        insertCommitsStatement = new InsertStatement("COMMITS", sqlConnection);
        insertFilesStatement = new InsertStatement("FILES", sqlConnection);
        insertDateTimeStatement = new InsertStatement("DATE_TIME", sqlConnection);
    }

    @Override
    public void handleLogEntry(SVNLogEntry svnLogEntry) throws SVNException {
        System.out.println("Handling revision: " + svnLogEntry.getRevision());
        try {
            fillCommits(svnLogEntry);
            fillChangedFiles(svnLogEntry);
            fillDates(svnLogEntry);
        } catch (SQLException e) {
            throw new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN, e.getMessage()), e);
        }
    }

    // Revision BIGINT, Date DATE, Year INTEGER, Month INTEGER, Day INTEGER, Week INTEGER, DayOfWeek INTEGER, Hour INTEGER, Minutes INTEGER, Sec INTEGER;
    private void fillDates(SVNLogEntry logEntry) throws SQLException {
        Calendar cal = new GregorianCalendar();
        cal.setTime(logEntry.getDate());
        insertDateTimeStatement.addrow(logEntry.getRevision(),
                cal.getTime(),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.WEEK_OF_YEAR),
                cal.get(Calendar.DAY_OF_WEEK),
                cal.get(Calendar.HOUR),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND)
        );
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
                        statement.setLong(idx, (long) parameter);
                        break;
                    case "INTEGER":
                        statement.setLong(idx, (int) parameter);
                        break;
                    case "VARCHAR":
                        statement.setString(idx, (String) parameter);
                        break;
                    case "DATE":
                        if (parameter instanceof java.util.Date) {
                            statement.setDate(idx, new Date(((java.util.Date) parameter).getTime()));
                        } else {
                            //noinspection ConstantConditions
                            statement.setDate(idx, (java.sql.Date) parameter);
                        }
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
