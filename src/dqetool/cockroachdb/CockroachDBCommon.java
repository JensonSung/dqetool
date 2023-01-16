package dqetool.cockroachdb;

import java.util.ArrayList;
import java.util.List;

import dqetool.Randomly;
import dqetool.cockroachdb.CockroachDBSchema.CockroachDBTable;
import dqetool.cockroachdb.ast.CockroachDBExpression;
import dqetool.cockroachdb.ast.CockroachDBIndexReference;
import dqetool.cockroachdb.ast.CockroachDBTableReference;

public final class CockroachDBCommon {

    private CockroachDBCommon() {
    }

    public static String getRandomCollate() {
        return Randomly.fromOptions("en", "de", "es", "cmn");
    }

    public static List<CockroachDBExpression> getTableReferences(List<CockroachDBTableReference> tableList) {
        List<CockroachDBExpression> from = new ArrayList<>();
        for (CockroachDBTableReference t : tableList) {
            CockroachDBTable table = t.getTable();
            if (!table.getIndexes().isEmpty() && Randomly.getBooleanWithSmallProbability()) {
                from.add(new CockroachDBIndexReference(t, table.getRandomIndex()));
            } else {
                from.add(t);
            }
        }
        return from;
    }

}
