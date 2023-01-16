package dqetool.cockroachdb.ast;

import dqetool.cockroachdb.CockroachDBSchema.CockroachDBColumn;

public class CockroachDBColumnReference implements CockroachDBExpression {

    private final CockroachDBColumn c;

    public CockroachDBColumnReference(CockroachDBColumn c) {
        this.c = c;
    }

    public CockroachDBColumn getColumn() {
        return c;
    }

}
