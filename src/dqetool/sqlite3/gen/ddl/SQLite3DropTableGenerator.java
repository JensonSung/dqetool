package dqetool.sqlite3.gen.ddl;

import dqetool.IgnoreMeException;
import dqetool.Randomly;
import dqetool.common.query.ExpectedErrors;
import dqetool.common.query.SQLQueryAdapter;
import dqetool.sqlite3.SQLite3GlobalState;

public final class SQLite3DropTableGenerator {

    private SQLite3DropTableGenerator() {
    }

    public static SQLQueryAdapter dropTable(SQLite3GlobalState globalState) {
        if (globalState.getSchema().getTables(t -> !t.isView()).size() == 1) {
            throw new IgnoreMeException();
        }
        StringBuilder sb = new StringBuilder("DROP TABLE ");
        if (Randomly.getBoolean()) {
            sb.append("IF EXISTS ");
        }
        sb.append(globalState.getSchema().getRandomTableOrBailout(t -> !t.isView()).getName());
        return new SQLQueryAdapter(sb.toString(),
                ExpectedErrors.from("[SQLITE_ERROR] SQL error or missing database (foreign key mismatch",
                        "Abort due to constraint violation (FOREIGN KEY constraint failed)",
                        "SQL error or missing database"),
                true);

    }

}
