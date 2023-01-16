package dqetool.cockroachdb.gen;

import java.util.List;

import dqetool.IgnoreMeException;
import dqetool.Randomly;
import dqetool.cockroachdb.CockroachDBErrors;
import dqetool.cockroachdb.CockroachDBProvider.CockroachDBGlobalState;
import dqetool.cockroachdb.CockroachDBSchema.CockroachDBColumn;
import dqetool.cockroachdb.CockroachDBSchema.CockroachDBTable;
import dqetool.common.query.ExpectedErrors;
import dqetool.common.query.SQLQueryAdapter;
import dqetool.common.schema.TableIndex;

public final class CockroachDBCommentOnGenerator {

    private CockroachDBCommentOnGenerator() {
    }

    private enum Option {
        TABLE, INDEX, COLUMN
    }

    public static SQLQueryAdapter comment(CockroachDBGlobalState globalState) {
        StringBuilder sb = new StringBuilder("COMMENT ON ");
        CockroachDBTable randomTable = globalState.getSchema().getRandomTable(t -> !t.isView());
        switch (Randomly.fromOptions(Option.values())) {
        case TABLE:
            sb.append("TABLE ");
            sb.append(randomTable.getName());
            break;
        case INDEX:
            List<TableIndex> indexes = randomTable.getIndexes();
            if (indexes.isEmpty()) {
                throw new IgnoreMeException();
            }
            TableIndex index = Randomly.fromList(indexes);
            if (index.getIndexName().contains("primary")) {
                throw new IgnoreMeException();
            }
            sb.append("INDEX ");
            sb.append(index.getIndexName());
            break;
        case COLUMN:
            sb.append("COLUMN ");
            CockroachDBColumn randomColumn = randomTable.getRandomColumn();

            sb.append(randomColumn.getFullQualifiedName());
            break;
        default:
            throw new AssertionError();
        }
        sb.append(" IS '");
        sb.append(globalState.getRandomly().getString().replace("'", "''"));
        sb.append("'");
        ExpectedErrors errors = new ExpectedErrors();
        CockroachDBErrors.addTransactionErrors(errors);
        return new SQLQueryAdapter(sb.toString(), errors);
    }

}
