package com.dalet.svnstats;

import java.io.Closeable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
* User: Moisei Rabinovich
* Date: 2/20/14
* Time: 3:43 PM
*/
public class InsertStatement implements Closeable {
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

    @SuppressWarnings("ConstantConditions") // false alert of IntelliJ
    public void addrow(Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; ++i) {
            String typeName = types.get(i);
            Object parameter = parameters[i];
            int idx = i + 1;
            Date sqlDate = (parameter instanceof java.util.Date) ? new Date(((java.util.Date) parameter).getTime()) : null;
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
                    statement.setDate(idx, sqlDate);
                    break;
                case "TIME":
                    statement.setTime(idx, new Time(sqlDate.getTime()));
                    break;
                case "TIMESTAMP":
                    statement.setTimestamp(idx, new Timestamp(sqlDate.getTime()));
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
