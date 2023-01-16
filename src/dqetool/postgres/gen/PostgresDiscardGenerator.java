package dqetool.postgres.gen;

import dqetool.Randomly;
import dqetool.common.query.ExpectedErrors;
import dqetool.common.query.SQLQueryAdapter;
import dqetool.postgres.PostgresGlobalState;
import dqetool.postgres.PostgresSchema.PostgresTable.TableType;

public final class PostgresDiscardGenerator {

    private PostgresDiscardGenerator() {
    }

    public static SQLQueryAdapter create(PostgresGlobalState globalState) {
        StringBuilder sb = new StringBuilder();
        sb.append("DISCARD ");
        // prevent that DISCARD discards all tables (if they are TEMP tables)
        boolean hasNonTempTables = globalState.getSchema().getDatabaseTables().stream()
                .anyMatch(t -> t.getTableType() == TableType.STANDARD);
        String what;
        if (hasNonTempTables) {
            what = Randomly.fromOptions("ALL", "PLANS", "SEQUENCES", "TEMPORARY", "TEMP");
        } else {
            what = Randomly.fromOptions("PLANS", "SEQUENCES");
        }
        sb.append(what);
        return new SQLQueryAdapter(sb.toString(), ExpectedErrors.from("cannot run inside a transaction block")) {

            @Override
            public boolean couldAffectSchema() {
                return canDiscardTemporaryTables(what);
            }
        };
    }

    private static boolean canDiscardTemporaryTables(String what) {
        return what.contentEquals("TEMPORARY") || what.contentEquals("TEMP") || what.contentEquals("ALL");
    }
}
