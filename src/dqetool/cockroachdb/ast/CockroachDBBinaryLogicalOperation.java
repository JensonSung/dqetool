package dqetool.cockroachdb.ast;

import dqetool.Randomly;
import dqetool.cockroachdb.ast.CockroachDBBinaryLogicalOperation.CockroachDBBinaryLogicalOperator;
import dqetool.common.ast.BinaryOperatorNode;
import dqetool.common.ast.Operator;

public class CockroachDBBinaryLogicalOperation extends
        BinaryOperatorNode<CockroachDBExpression, CockroachDBBinaryLogicalOperator> implements CockroachDBExpression {

    public enum CockroachDBBinaryLogicalOperator implements Operator {
        AND("AND"), OR("OR");

        private String textRepr;

        CockroachDBBinaryLogicalOperator(String textRepr) {
            this.textRepr = textRepr;
        }

        public static CockroachDBBinaryLogicalOperator getRandom() {
            return Randomly.fromOptions(CockroachDBBinaryLogicalOperator.values());
        }

        @Override
        public String getTextRepresentation() {
            return textRepr;
        }

    }

    public CockroachDBBinaryLogicalOperation(CockroachDBExpression left, CockroachDBExpression right,
            CockroachDBBinaryLogicalOperator op) {
        super(left, right, op);
    }

}
