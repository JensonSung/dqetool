package dqetool.mysql.ast;

import dqetool.IgnoreMeException;
import dqetool.Randomly;
import dqetool.common.ast.Operator;
import dqetool.common.ast.UnaryOperatorNode;
import dqetool.mysql.ast.MySQLUnaryPrefixOperation.MySQLUnaryPrefixOperator;

public class MySQLUnaryPrefixOperation extends UnaryOperatorNode<MySQLExpression, MySQLUnaryPrefixOperator>
        implements MySQLExpression {

    public enum MySQLUnaryPrefixOperator implements Operator {
        NOT("!", "NOT") {
            @Override
            public MySQLConstant applyNotNull(MySQLConstant expr) {
                return MySQLConstant.createIntConstant(expr.asBooleanNotNull() ? 0 : 1);
            }
        },
        PLUS("+") {
            @Override
            public MySQLConstant applyNotNull(MySQLConstant expr) {
                return expr;
            }
        },
        MINUS("-") {
            @Override
            public MySQLConstant applyNotNull(MySQLConstant expr) {
                if (expr.isString()) {
                    // TODO: implement floating points
                    throw new IgnoreMeException();
                } else if (expr.isInt()) {
                    if (!expr.isSigned()) {
                        // TODO
                        throw new IgnoreMeException();
                    }
                    return MySQLConstant.createIntConstant(-expr.getInt());
                } else {
                    throw new AssertionError(expr);
                }
            }
        };

        private String[] textRepresentations;

        MySQLUnaryPrefixOperator(String... textRepresentations) {
            this.textRepresentations = textRepresentations.clone();
        }

        public abstract MySQLConstant applyNotNull(MySQLConstant expr);

        public static MySQLUnaryPrefixOperator getRandom() {
            return Randomly.fromOptions(values());
        }

        @Override
        public String getTextRepresentation() {
            return Randomly.fromOptions(textRepresentations);
        }
    }

    public MySQLUnaryPrefixOperation(MySQLExpression expr, MySQLUnaryPrefixOperator op) {
        super(expr, op);
    }

    @Override
    public MySQLConstant getExpectedValue() {
        MySQLConstant subExprVal = expr.getExpectedValue();
        if (subExprVal.isNull()) {
            return MySQLConstant.createNullConstant();
        } else {
            return op.applyNotNull(subExprVal);
        }
    }

    @Override
    public OperatorKind getOperatorKind() {
        return OperatorKind.PREFIX;
    }

}
