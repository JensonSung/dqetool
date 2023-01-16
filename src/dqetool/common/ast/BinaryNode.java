package dqetool.common.ast;

import dqetool.common.visitor.BinaryOperation;

public abstract class BinaryNode<T> implements BinaryOperation<T> {

    private final T left;
    private final T right;

    public BinaryNode(T left, T right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public T getLeft() {
        return left;
    }

    @Override
    public T getRight() {
        return right;
    }

}
