package dqetool.tidb.ast;

import dqetool.Randomly;
import dqetool.common.ast.BinaryOperatorNode;
import dqetool.common.ast.Operator;
import dqetool.tidb.ast.TiDBRegexOperation.TiDBRegexOperator;

public class TiDBRegexOperation extends BinaryOperatorNode<TiDBExpression, TiDBRegexOperator>
        implements TiDBExpression {

    public enum TiDBRegexOperator implements Operator {
        LIKE("LIKE"), //
        NOT_LIKE("NOT LIKE"), //
        ILIKE("REGEXP"), //
        NOT_REGEXP("NOT REGEXP");

        private String textRepr;

        TiDBRegexOperator(String textRepr) {
            this.textRepr = textRepr;
        }

        public static TiDBRegexOperator getRandom() {
            return Randomly.fromOptions(values());
        }

        @Override
        public String getTextRepresentation() {
            return textRepr;
        }

    }

    public TiDBRegexOperation(TiDBExpression left, TiDBExpression right, TiDBRegexOperator op) {
        super(left, right, op);
    }

}
