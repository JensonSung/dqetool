package dqetool.mysql.gen;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import dqetool.Randomly;
import dqetool.common.query.ExpectedErrors;
import dqetool.common.query.SQLQueryAdapter;
import dqetool.mysql.MySQLGlobalState;
import dqetool.mysql.MySQLSchema.MySQLColumn;
import dqetool.mysql.MySQLSchema.MySQLTable;
import dqetool.mysql.MySQLVisitor;

public class MySQLInsertGenerator {

    private final MySQLTable table;
    private final StringBuilder sb = new StringBuilder();
    private final ExpectedErrors errors = new ExpectedErrors();
    private final MySQLGlobalState globalState;

    public MySQLInsertGenerator(MySQLGlobalState globalState) {
        this.globalState = globalState;
        table = globalState.getSchema().getRandomTable();
    }

    public static SQLQueryAdapter insertRow(MySQLGlobalState globalState) throws SQLException {
        if (Randomly.getBoolean()) {
            return new MySQLInsertGenerator(globalState).generateInsert();
        } else {
            return new MySQLInsertGenerator(globalState).generateReplace();
        }
    }

    private SQLQueryAdapter generateReplace() {
        sb.append("REPLACE");
        if (Randomly.getBoolean()) {
            sb.append(" ");
            sb.append(Randomly.fromOptions("LOW_PRIORITY", "DELAYED"));
        }
        return generateInto();

    }

    private SQLQueryAdapter generateInsert() {
        sb.append("INSERT");
        if (Randomly.getBoolean()) {
            sb.append(" ");
            sb.append(Randomly.fromOptions("LOW_PRIORITY", "DELAYED", "HIGH_PRIORITY"));
        }
        if (Randomly.getBoolean()) {
            sb.append(" IGNORE");
        }
        return generateInto();
    }

    private SQLQueryAdapter generateInto() {
        sb.append(" INTO ");
        sb.append(table.getName());
        List<MySQLColumn> columns = table.getRandomNonEmptyColumnSubset();
        sb.append("(");
        sb.append(columns.stream().map(c -> c.getName()).collect(Collectors.joining(", ")));
        sb.append(") ");
        sb.append("VALUES");
        MySQLExpressionGenerator gen = new MySQLExpressionGenerator(globalState);
        int nrRows;
        if (Randomly.getBoolean()) {
            nrRows = 1;
        } else {
            nrRows = 1 + Randomly.smallNumber();
        }
        for (int row = 0; row < nrRows; row++) {
            if (row != 0) {
                sb.append(", ");
            }
            sb.append("(");
            for (int c = 0; c < columns.size(); c++) {
                if (c != 0) {
                    sb.append(", ");
                }
                sb.append(MySQLVisitor.asString(gen.generateConstant()));

            }
            sb.append(")");
        }
        errors.add("doesn't have a default value");
        errors.add("Data truncation");
        errors.add("Incorrect integer value");
        errors.add("Duplicate entry");
        errors.add("Data truncated for functional index");
        errors.add("Data truncated for column");
        errors.add("cannot be null");
        errors.add("Incorrect decimal value");
        return new SQLQueryAdapter(sb.toString(), errors);
    }

}
