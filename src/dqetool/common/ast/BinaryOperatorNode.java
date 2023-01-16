package dqetool.common.ast;

public abstract class BinaryOperatorNode<T, O extends Operator> extends BinaryNode<T> {

    private final O op;

    public BinaryOperatorNode(T left, T right, O op) {
        super(left, right);
        this.op = op;
    }

    @Override
    public String getOperatorRepresentation() {
        return op.getTextRepresentation();
    }

    public O getOp() {
        return op;
    }

}

