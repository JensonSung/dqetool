package dqetool.sqlite3.oracle;

import com.beust.jcommander.Strings;
import dqetool.IgnoreMeException;
import dqetool.common.oracle.DQEBase;
import dqetool.common.oracle.TestOracle;
import dqetool.common.query.SQLQueryAdapter;
import dqetool.common.query.SQLQueryError;
import dqetool.common.query.SQLancerResultSet;
import dqetool.common.schema.AbstractRelationalTable;
import dqetool.sqlite3.SQLite3Errors;
import dqetool.sqlite3.SQLite3GlobalState;
import dqetool.sqlite3.SQLite3Visitor;
import dqetool.sqlite3.ast.SQLite3Expression;
import dqetool.sqlite3.gen.SQLite3ExpressionGenerator;
import dqetool.sqlite3.schema.SQLite3Schema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SQLite3DQEOracle extends DQEBase<SQLite3GlobalState> implements TestOracle<SQLite3GlobalState> {

    private final SQLite3Schema schema;

    public SQLite3DQEOracle(SQLite3GlobalState state) {
        super(state);
        schema = state.getSchema();

        SQLite3Errors.addExpectedExpressionErrors(selectExpectedErrors);

        SQLite3Errors.addExpectedExpressionErrors(updateExpectedErrors);
        updateExpectedErrors.add("NOT NULL constraint failed");
        updateExpectedErrors.add("UNIQUE constraint failed");
        updateExpectedErrors.add("cannot UPDATE generated column");

        SQLite3Errors.addExpectedExpressionErrors(deleteExpectedErrors);
        deleteExpectedErrors.add("FOREIGN KEY constraint failed");
    }

    public void check() throws SQLException {
        if (schema.getDatabaseTablesWithoutViewsWithoutVirtualTables().isEmpty()) {
            // DQE does not support views and virtual tables.
            throw new IgnoreMeException();
        }
        SQLite3Schema.SQLite3Table table = schema.getRandomTableNoViewNoVirtualTable();

        // DQE does not support aggregate functions, windows functions
        // This method does not generate them, may need some configurations if they can be generated
        SQLite3ExpressionGenerator expressionGenerator = new SQLite3ExpressionGenerator(state).setColumns(table.getColumns());
        SQLite3Expression whereClause = expressionGenerator.generatePredicate();

        // PostgresVisitor is not deterministic, we should keep it only once.
        // Especially, in PostgresUnaryPostfixOperation and PostgresDBUnaryPrefixOperation
        String whereClauseStr = SQLite3Visitor.asString(whereClause);

        // Generate a SELECT statement
        List<String> selectColumns = new ArrayList<>();
        selectColumns.add(COLUMN_ROWID);

        String selectStmt = String.format("SELECT %s FROM %s WHERE %s",
                Strings.join(",", selectColumns), table.getName(), whereClauseStr);

        // Generate an UPDATE statement
        // UPDATE operates on single table and does not support order by
        List<String> updateColumns = new ArrayList<>();
        updateColumns.add(String.format("%s = 1", COLUMN_UPDATED));
        String updateStmt = String.format("UPDATE %s SET %s WHERE %s",
                table.getName(), Strings.join(",", updateColumns), whereClauseStr);

        // Generate a DELETE statement
        String deleteStmt = String.format("DELETE FROM %s WHERE %s", table.getName(), whereClauseStr);

        addAuxiliaryColumns(table);
        backupTableContent(table);

        state.getState().getLocalState().log(selectStmt);
        SQLQueryResult selectExecutionResult = executeSelect(selectStmt, table);
        state.getState().getLocalState().log(selectExecutionResult.getAccessedRows().values().toString());
        state.getState().getLocalState().log(selectExecutionResult.getQueryErrors().toString());

        state.getState().getLocalState().log(updateStmt);
        SQLQueryResult updateExecutionResult = executeUpdate(updateStmt, table);
        state.getState().getLocalState().log(updateExecutionResult.getAccessedRows().values().toString());
        state.getState().getLocalState().log(updateExecutionResult.getQueryErrors().toString());
        recreateTable(table);

        state.getState().getLocalState().log(deleteStmt);
        SQLQueryResult deleteExecutionResult = executeDelete(deleteStmt, table);
        state.getState().getLocalState().log(deleteExecutionResult.getAccessedRows().values().toString());
        state.getState().getLocalState().log(deleteExecutionResult.getQueryErrors().toString());
        recreateTable(table);

        String compareSelectAndUpdate = compareSelectAndUpdate(selectExecutionResult, updateExecutionResult);
        String compareSelectAndDelete = compareSelectAndDelete(selectExecutionResult, deleteExecutionResult);
        String compareUpdateAndDelete = compareUpdateAndDelete(updateExecutionResult, deleteExecutionResult);

        String errorMessage = compareSelectAndUpdate == null ? "" : compareSelectAndUpdate + "\n";
        errorMessage += compareSelectAndDelete == null ? "" : compareSelectAndDelete + "\n";
        errorMessage += compareUpdateAndDelete == null ? "" : compareUpdateAndDelete + "\n";

        if (!errorMessage.equals("")) {
            throw new AssertionError(errorMessage);
        } else {
            state.getState().getLocalState().log("PASS");
        }

        dropAuxiliaryColumns(table);
    }

    public String compareSelectAndUpdate(SQLQueryResult selectResult, SQLQueryResult updateResult) {
        if (updateResult.hasEmptyErrors()) {
            if (selectResult.hasErrors()) {
                return "SELECT has errors, but UPDATE does not.";
            }
            if (!selectResult.hasSameAccessedRows(updateResult)) {
                return "SELECT accessed different rows from UPDATE.";
            }
            return null;
        } else { // update has errors
            if (hasUpdateSpecificErrors(updateResult)) {
                if (updateResult.hasAccessedRows()) {
                    return "UPDATE accessed non-empty rows when specific errors happen.";
                } else {
                    // we do not compare update with select when update has specific errors
                    return null;
                }
            }

            // update errors should all appear in the select errors
            List<SQLQueryError> queryErrors = new ArrayList<>(selectResult.getQueryErrors());
            for (int i = 0; i < updateResult.getQueryErrors().size(); i++) {
                SQLQueryError updateError = updateResult.getQueryErrors().get(i);
                boolean found = false;
                for (int j = 0; j < queryErrors.size(); j++) {
                    SQLQueryError selectError = queryErrors.get(j);
                    if (selectError.hasSameCodeAndMessage(updateError)) {
                        queryErrors.remove(selectError);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return "SELECT has different errors from UPDATE.";
                }
            }

            // all errors are stop errors, they should not have accessed rows
            if (updateResult.hasAccessedRows()) {
                return "UPDATE accessed non-empty rows when stop errors happen.";
            }

            if (!selectResult.hasSameAccessedRows(updateResult)) {
                return "SELECT accessed different rows from UPDATE when errors happen.";
            }

            return null;
        }
    }

    public String compareSelectAndDelete(SQLQueryResult selectResult, SQLQueryResult deleteResult) {
        if (deleteResult.hasEmptyErrors()) {
            if (selectResult.hasErrors()) {
                return "SELECT has errors, but DELETE does not.";
            }
            if (!selectResult.hasSameAccessedRows(deleteResult)) {
                return "SELECT accessed different rows from DELETE.";
            }
            return null;
        } else { // delete has errors
            if (hasDeleteSpecificErrors(deleteResult)) {
                if (deleteResult.hasAccessedRows()) {
                    return "DELETE accessed non-empty rows when specific errors happen.";
                } else {
                    // we do not compare DELETE with select when DELETE has specific errors
                    return null;
                }
            }

            // DELETE errors should all appear in the select errors
            List<SQLQueryError> queryErrors = new ArrayList<>(selectResult.getQueryErrors());
            for (int i = 0; i < deleteResult.getQueryErrors().size(); i++) {
                SQLQueryError deleteError = deleteResult.getQueryErrors().get(i);
                boolean found = false;
                for (int j = 0; j < queryErrors.size(); j++) {
                    SQLQueryError selectError = queryErrors.get(j);
                    if (selectError.hasSameCodeAndMessage(deleteError)) {
                        queryErrors.remove(selectError);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return "SELECT has different errors from DELETE.";
                }
            }

            // all errors are stop errors, they should not have accessed rows
            if (deleteResult.hasAccessedRows()) {
                return "DELETE accessed non-empty rows when stop errors happen.";
            }

            if (!selectResult.hasSameAccessedRows(deleteResult)) {
                return "SELECT accessed different rows from DELETE when errors happen.";
            }

            return null;
        }
    }

    public String compareUpdateAndDelete(SQLQueryResult updateResult, SQLQueryResult deleteResult) {
        if (updateResult.hasEmptyErrors() && deleteResult.hasEmptyErrors()) {
            if (updateResult.hasSameAccessedRows(deleteResult)) {
                return null;
            } else {
                return "UPDATE accessed different rows from DELETE.";
            }
        } else {// update or delete has errors
            boolean hasSpecificErrors = false;

            if (hasUpdateSpecificErrors(updateResult)) {
                hasSpecificErrors = true;
                if (updateResult.hasAccessedRows()) {
                    return "UPDATE accessed non-empty rows when specific errors happen.";
                }
            }

            if (hasDeleteSpecificErrors(deleteResult)) {
                hasSpecificErrors = true;
                if (deleteResult.hasAccessedRows()) {
                    return "DELETE accessed non-empty rows when specific errors happen.";
                }
            }

            // when one of these statements has specific errors, do not compare them
            if (hasSpecificErrors) {
                return null;
            }

            // all errors are stop errors, they should not have accessed rows
            if (!updateResult.hasSameErrors(deleteResult)) {
                return "UPDATE has different errors from DELETE.";
            } else {
                if (updateResult.hasAccessedRows() || deleteResult.hasAccessedRows()) {
                    return "UPDATE or DELETE accessed non-empty rows when stop errors happen.";
                }
            }

            return null;
        }
    }

    /**
     * when update violates column constraints, such as not null, unique, primary key and generated column,
     * we cannot compare it with other queries.
     */
    private boolean hasUpdateSpecificErrors(SQLQueryResult updateResult) {
        for (SQLQueryError queryError : updateResult.getQueryErrors()) {
            String message = queryError.getMessage();
            // UNIQUE constraint failed: t1.c1
            // NOT NULL constraint failed: t1.c1
            // cannot UPDATE generated column "c2"
            if (message.contains("UNIQUE constraint failed") || message.contains("NOT NULL constraint failed") ||
                    message.contains("cannot UPDATE generated column")) {
                // TODO: error code may not complete
                return true;
            }
        }
        return false;
    }

    /**
     * when delete violates column constraints, such as foreign key,
     * we cannot compare it with other queries.
     */
    private boolean hasDeleteSpecificErrors(SQLQueryResult deleteResult) {
        for (SQLQueryError queryError : deleteResult.getQueryErrors()) {
            String message = queryError.getMessage();
            // FOREIGN KEY constraint failed
            if (message.contains("FOREIGN KEY constraint failed")) {
                // TODO: error code may not complete
                return true;
            }
        }

        return false;
    }

    private SQLQueryResult executeSelect(String selectStmt, SQLite3Schema.SQLite3Table table) throws SQLException {
        Map<AbstractRelationalTable<?, ?, ?>, Set<String>> accessedRows = new HashMap<>();
        List<SQLQueryError> queryErrors = new ArrayList<>();
        SQLancerResultSet resultSet = null;
        try {
            resultSet = new SQLQueryAdapter(selectStmt, selectExpectedErrors).executeAndGet(state, true);
        } catch (SQLException e) {
            SQLQueryError queryError = new SQLQueryError();
            if (e.getMessage().contains("\n")) {
                queryError.setMessage(e.getMessage().split("\n")[0]);
            } else {
                queryError.setMessage(e.getMessage());
            }
            queryErrors.add(queryError);
        } finally {
            HashSet<String> rows = new HashSet<>();
            accessedRows.put(table, rows);
            if (resultSet != null) {
                try {
                    while (resultSet.next()) {
                        accessedRows.get(table).add(resultSet.getString(COLUMN_ROWID));
                    }
                    resultSet.close();
                } catch (SQLException ignored) {
                }
            }
        }

        return new SQLQueryResult(accessedRows, queryErrors);
    }

    private SQLQueryResult executeUpdate(String updateStmt, SQLite3Schema.SQLite3Table table) throws SQLException {
        Map<AbstractRelationalTable<?, ?, ?>, Set<String>> accessedRows = new HashMap<>();
        List<SQLQueryError> queryErrors = new ArrayList<>();
        try {
            new SQLQueryAdapter(updateStmt, updateExpectedErrors).execute(state, true);
        } catch (SQLException e) {
            SQLQueryError queryError = new SQLQueryError();
            if (e.getMessage().contains("\n")) {
                queryError.setMessage(e.getMessage().split("\n")[0]);
            } else {
                queryError.setMessage(e.getMessage());
            }
            queryErrors.add(queryError);
        } finally {
            String selectRowIdWithUpdated = String.format("SELECT %s FROM %s WHERE %s = 1", COLUMN_ROWID, table.getName(), COLUMN_UPDATED);
            SQLancerResultSet resultSet = new SQLQueryAdapter(selectRowIdWithUpdated).executeAndGet(state);
            HashSet<String> rows = new HashSet<>();
            if (resultSet != null) {
                try {
                    while (resultSet.next()) {
                        rows.add(resultSet.getString(COLUMN_ROWID));
                    }
                    resultSet.close();
                } catch (SQLException ignored) {
                }
            }
            accessedRows.put(table, rows);
        }

        return new SQLQueryResult(accessedRows, queryErrors);
    }

    private SQLQueryResult executeDelete(String deleteStmt, SQLite3Schema.SQLite3Table table) throws SQLException {
        Map<AbstractRelationalTable<?, ?, ?>, Set<String>> accessedRows = new HashMap<>();
        List<SQLQueryError> queryErrors = new ArrayList<>();
        try {
            String selectRowId = String.format("SELECT %s FROM %s", COLUMN_ROWID, table.getName());
            SQLancerResultSet resultSet = new SQLQueryAdapter(selectRowId).executeAndGet(state);
            HashSet<String> rows = new HashSet<>();
            if (resultSet != null) {
                try {
                    while (resultSet.next()) {
                        rows.add(resultSet.getString(COLUMN_ROWID));
                    }
                    resultSet.close();
                } catch (SQLException ignored) {
                }
            }
            accessedRows.put(table, rows);

            new SQLQueryAdapter(deleteStmt, deleteExpectedErrors).execute(state, true);
        } catch (SQLException e) {
            SQLQueryError queryError = new SQLQueryError();
            if (e.getMessage().contains("\n")) {
                queryError.setMessage(e.getMessage().split("\n")[0]);
            } else {
                queryError.setMessage(e.getMessage());
            }
            queryErrors.add(queryError);
        } finally {
            String selectRowId = String.format("SELECT %s FROM %s", COLUMN_ROWID, table.getName());
            SQLancerResultSet resultSet = new SQLQueryAdapter(selectRowId).executeAndGet(state);
            HashSet<String> rows = new HashSet<>();
            if (resultSet != null) {
                try {
                    while (resultSet.next()) {
                        rows.add(resultSet.getString(COLUMN_ROWID));
                    }
                    resultSet.close();
                } catch (SQLException ignored) {
                }
            }

            accessedRows.get(table).removeAll(rows);
        }

        return new SQLQueryResult(accessedRows, queryErrors);
    }

    @Override
    public void addAuxiliaryColumns(AbstractRelationalTable<?, ?, ?> table) throws SQLException {
        String tableName = table.getName();

        String addColumnRowID = String.format("ALTER TABLE %s ADD %s TEXT", tableName, COLUMN_ROWID);
        new SQLQueryAdapter(addColumnRowID).execute(state);
        state.getState().getLocalState().log(addColumnRowID);

        String addColumnUpdated = String.format("ALTER TABLE %s ADD %s INT DEFAULT 0", tableName, COLUMN_UPDATED);
        new SQLQueryAdapter(addColumnUpdated).execute(state);
        state.getState().getLocalState().log(addColumnUpdated);

        String updateRowsWithUniqueID = String.format("UPDATE %s SET %s = hex(randomblob(16))", tableName, COLUMN_ROWID);
        new SQLQueryAdapter(updateRowsWithUniqueID).execute(state);
        state.getState().getLocalState().log(updateRowsWithUniqueID);
    }

    private void backupTableContent(SQLite3Schema.SQLite3Table table) throws SQLException {
        String dropBackupTable = String.format("DROP TABLE IF EXISTS backup%s", table.getName());
        new SQLQueryAdapter(dropBackupTable).execute(state);
        String createBackupTable = String.format("CREATE TABLE backup%s AS SELECT * FROM %s", table.getName(), table.getName());
        new SQLQueryAdapter(createBackupTable, true).execute(state);
    }

    private void recreateTable(SQLite3Schema.SQLite3Table table) throws SQLException {
        String truncateTable = String.format("DELETE FROM %s", table.getName());
        new SQLQueryAdapter(truncateTable).execute(state);
        String recreateTable = String.format("INSERT INTO %s SELECT * FROM backup%s", table.getName(), table.getName());
        new SQLQueryAdapter(recreateTable).execute(state);
    }
}
