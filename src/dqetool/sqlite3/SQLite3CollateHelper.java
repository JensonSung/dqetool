package dqetool.sqlite3;

import dqetool.sqlite3.ast.SQLite3Expression;
import dqetool.sqlite3.ast.SQLite3Expression.Cast;
import dqetool.sqlite3.ast.SQLite3Expression.SQLite3ColumnName;
import dqetool.sqlite3.ast.SQLite3UnaryOperation;
import dqetool.sqlite3.ast.SQLite3UnaryOperation.UnaryOperator;

public final class SQLite3CollateHelper {

    private SQLite3CollateHelper() {
    }

    public static boolean shouldGetSubexpressionAffinity(SQLite3Expression expression) {
        return expression instanceof SQLite3UnaryOperation
                && ((SQLite3UnaryOperation) expression).getOperation() == UnaryOperator.PLUS
                || expression instanceof Cast || expression instanceof SQLite3ColumnName;
    }

}
