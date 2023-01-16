package dqetool.cockroachdb.ast;

import dqetool.Randomly;
import dqetool.cockroachdb.ast.CockroachDBBinaryArithmeticOperation.CockroachDBBinaryArithmeticOperator;
import dqetool.common.ast.BinaryOperatorNode;
import dqetool.common.ast.Operator;

public class CockroachDBBinaryArithmeticOperation
        extends BinaryOperatorNode<CockroachDBExpression, CockroachDBBinaryArithmeticOperator>
        implements CockroachDBExpression {

    public enum CockroachDBBinaryArithmeticOperator implements Operator {
        ADD("+"), MULT("*"), MINUS("-"), DIV("/");

        String textRepresentation;

        CockroachDBBinaryArithmeticOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public static CockroachDBBinaryArithmeticOperator getRandom() {
            return Randomly.fromOptions(values());
        }

        @Override
        public String getTextRepresentation() {
            return textRepresentation;
        }

    }

    public CockroachDBBinaryArithmeticOperation(CockroachDBExpression left, CockroachDBExpression right,
            CockroachDBBinaryArithmeticOperator op) {
        super(left, right, op);
    }

}
