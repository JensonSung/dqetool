package dqetool.mysql;

import dqetool.IgnoreMeException;
import dqetool.mysql.ast.MySQLBetweenOperation;
import dqetool.mysql.ast.MySQLBinaryComparisonOperation;
import dqetool.mysql.ast.MySQLBinaryLogicalOperation;
import dqetool.mysql.ast.MySQLBinaryOperation;
import dqetool.mysql.ast.MySQLCastOperation;
import dqetool.mysql.ast.MySQLCollate;
import dqetool.mysql.ast.MySQLColumnReference;
import dqetool.mysql.ast.MySQLComputableFunction;
import dqetool.mysql.ast.MySQLConstant;
import dqetool.mysql.ast.MySQLExists;
import dqetool.mysql.ast.MySQLExpression;
import dqetool.mysql.ast.MySQLInOperation;
import dqetool.mysql.ast.MySQLOrderByTerm;
import dqetool.mysql.ast.MySQLSelect;
import dqetool.mysql.ast.MySQLStringExpression;
import dqetool.mysql.ast.MySQLTableReference;
import dqetool.mysql.ast.MySQLUnaryPostfixOperation;

public class MySQLExpectedValueVisitor implements MySQLVisitor {

    private final StringBuilder sb = new StringBuilder();
    private int nrTabs;

    private void print(MySQLExpression expr) {
        MySQLToStringVisitor v = new MySQLToStringVisitor();
        v.visit(expr);
        for (int i = 0; i < nrTabs; i++) {
            sb.append("\t");
        }
        sb.append(v.get());
        sb.append(" -- ");
        sb.append(expr.getExpectedValue());
        sb.append("\n");
    }

    @Override
    public void visit(MySQLExpression expr) {
        nrTabs++;
        try {
            MySQLVisitor.super.visit(expr);
        } catch (IgnoreMeException e) {

        }
        nrTabs--;
    }

    @Override
    public void visit(MySQLConstant constant) {
        print(constant);
    }

    @Override
    public void visit(MySQLColumnReference column) {
        print(column);
    }

    @Override
    public void visit(MySQLUnaryPostfixOperation op) {
        print(op);
        visit(op.getExpression());
    }

    @Override
    public void visit(MySQLComputableFunction f) {
        print(f);
        for (MySQLExpression expr : f.getArguments()) {
            visit(expr);
        }
    }

    @Override
    public void visit(MySQLBinaryLogicalOperation op) {
        print(op);
        visit(op.getLeft());
        visit(op.getRight());
    }

    public String get() {
        return sb.toString();
    }

    @Override
    public void visit(MySQLSelect select) {
        for (MySQLExpression j : select.getJoinList()) {
            visit(j);
        }
        if (select.getWhereClause() != null) {
            visit(select.getWhereClause());
        }
    }

    @Override
    public void visit(MySQLBinaryComparisonOperation op) {
        print(op);
        visit(op.getLeft());
        visit(op.getRight());
    }

    @Override
    public void visit(MySQLCastOperation op) {
        print(op);
        visit(op.getExpr());
    }

    @Override
    public void visit(MySQLInOperation op) {
        print(op);
        for (MySQLExpression right : op.getListElements()) {
            visit(right);
        }
    }

    @Override
    public void visit(MySQLBinaryOperation op) {
        print(op);
        visit(op.getLeft());
        visit(op.getRight());
    }

    @Override
    public void visit(MySQLOrderByTerm op) {
    }

    @Override
    public void visit(MySQLExists op) {
        print(op);
        visit(op.getExpr());
    }

    @Override
    public void visit(MySQLStringExpression op) {
        print(op);
    }

    @Override
    public void visit(MySQLBetweenOperation op) {
        print(op);
        visit(op.getExpr());
        visit(op.getLeft());
        visit(op.getRight());
    }

    @Override
    public void visit(MySQLTableReference ref) {
    }

    @Override
    public void visit(MySQLCollate collate) {
        print(collate);
        visit(collate.getExpectedValue());
    }

}
