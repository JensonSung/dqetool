package dqetool.common.ast.newast;

import dqetool.common.ast.Operator;

public class NewUnaryPrefixOperatorNode<T> implements Node<T> {

    protected final Operator op;
    private final Node<T> expr;

    public NewUnaryPrefixOperatorNode(Node<T> expr, Operator op) {
        this.expr = expr;
        this.op = op;
    }

    public String getOperatorRepresentation() {
        return op.getTextRepresentation();
    }

    public Node<T> getExpr() {
        return expr;
    }

}
