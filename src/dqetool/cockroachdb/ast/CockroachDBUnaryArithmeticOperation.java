package dqetool.cockroachdb.ast;

import dqetool.cockroachdb.ast.CockroachDBUnaryArithmeticOperation.CockroachDBUnaryAritmeticOperator;
import dqetool.common.ast.Operator;
import dqetool.common.ast.UnaryOperatorNode;

public class CockroachDBUnaryArithmeticOperation extends
        UnaryOperatorNode<CockroachDBExpression, CockroachDBUnaryAritmeticOperator> implements CockroachDBExpression {

    public enum CockroachDBUnaryAritmeticOperator implements Operator {
        PLUS("+"), MINUS("-"), NEGATION("~");

        private String textRepr;

        CockroachDBUnaryAritmeticOperator(String textRepr) {
            this.textRepr = textRepr;
        }

        @Override
        public String getTextRepresentation() {
            return textRepr;
        }

    }

    public CockroachDBUnaryArithmeticOperation(CockroachDBExpression expr, CockroachDBUnaryAritmeticOperator op) {
        super(expr, op);
    }

    @Override
    public OperatorKind getOperatorKind() {
        return OperatorKind.PREFIX;
    }

}
