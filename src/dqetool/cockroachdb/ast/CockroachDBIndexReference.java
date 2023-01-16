package dqetool.cockroachdb.ast;

import dqetool.Randomly;
import dqetool.common.schema.TableIndex;
import dqetool.common.visitor.UnaryOperation;

public class CockroachDBIndexReference extends CockroachDBTableReference
        implements UnaryOperation<CockroachDBExpression> {

    private final CockroachDBTableReference tableReference;
    private final TableIndex index;

    public CockroachDBIndexReference(CockroachDBTableReference tableReference, TableIndex index) {
        super(tableReference.getTable());
        this.tableReference = tableReference;
        this.index = index;
    }

    @Override
    public CockroachDBExpression getExpression() {
        return tableReference;
    }

    @Override
    public String getOperatorRepresentation() {
        if (Randomly.getBoolean()) {
            return String.format("@{FORCE_INDEX=%s}", index.getIndexName());
        } else {
            return String.format("@{FORCE_INDEX=%s,%s}", index.getIndexName(), Randomly.fromOptions("ASC", "DESC"));
        }
    }

    @Override
    public OperatorKind getOperatorKind() {
        return OperatorKind.POSTFIX;
    }

    @Override
    public boolean omitBracketsWhenPrinting() {
        return true;
    }

}
