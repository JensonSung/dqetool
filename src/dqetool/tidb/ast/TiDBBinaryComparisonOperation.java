package dqetool.tidb.ast;

import dqetool.Randomly;
import dqetool.common.ast.BinaryOperatorNode;
import dqetool.common.ast.Operator;
import dqetool.tidb.ast.TiDBBinaryComparisonOperation.TiDBComparisonOperator;

public class TiDBBinaryComparisonOperation extends BinaryOperatorNode<TiDBExpression, TiDBComparisonOperator>
        implements TiDBExpression {

    public enum TiDBComparisonOperator implements Operator {
        EQUALS("="), //
        GREATER(">"), //
        GREATER_EQUALS(">="), //
        SMALLER("<"), //
        SMALLER_EQUALS("<="), //
        NOT_EQUALS("!="), //
        NULL_SAFE_EQUALS("<=>"); // https://github.com/tidb-challenge-program/bug-hunting-issue/issues/5

        private String textRepr;

        TiDBComparisonOperator(String textRepr) {
            this.textRepr = textRepr;
        }

        public static TiDBComparisonOperator getRandom() {
            return Randomly.fromOptions(TiDBComparisonOperator.values());
        }

        @Override
        public String getTextRepresentation() {
            return textRepr;
        }

    }

    public TiDBBinaryComparisonOperation(TiDBExpression left, TiDBExpression right, TiDBComparisonOperator op) {
        super(left, right, op);
    }

}
