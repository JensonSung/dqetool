package dqetool.cockroachdb.ast;

import dqetool.cockroachdb.CockroachDBSchema.CockroachDBTable;

public class CockroachDBTableReference implements CockroachDBExpression {

    private final CockroachDBTable table;

    public CockroachDBTableReference(CockroachDBTable table) {
        this.table = table;
    }

    public CockroachDBTable getTable() {
        return table;
    }

}
