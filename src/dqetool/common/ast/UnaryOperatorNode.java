package dqetool.common.ast;

public abstract class UnaryOperatorNode<T, O extends Operator> extends UnaryNode<T> {

    protected final O op;

    public UnaryOperatorNode(T expr, O op) {
        super(expr);
        this.op = op;
    }

    @Override
    public String getOperatorRepresentation() {
        return op.getTextRepresentation();
    }

}
