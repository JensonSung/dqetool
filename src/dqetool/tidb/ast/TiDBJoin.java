package dqetool.tidb.ast;

import java.util.ArrayList;
import java.util.List;

import dqetool.Randomly;
import dqetool.tidb.TiDBExpressionGenerator;
import dqetool.tidb.TiDBProvider.TiDBGlobalState;
import dqetool.tidb.TiDBSchema.TiDBColumn;

public class TiDBJoin implements TiDBExpression {

    private final TiDBExpression leftTable;
    private final TiDBExpression rightTable;
    private final JoinType joinType;
    private final TiDBExpression onCondition;
    private NaturalJoinType outerType;

    public enum JoinType {
        INNER, NATURAL, STRAIGHT, LEFT, RIGHT;

        public static JoinType getRandom() {
            return Randomly.fromOptions(values());
        }
    }

    public enum NaturalJoinType {
        INNER, LEFT, RIGHT;

        public static NaturalJoinType getRandom() {
            return Randomly.fromOptions(values());
        }
    }

    public TiDBJoin(TiDBExpression leftTable, TiDBExpression rightTable, JoinType joinType,
            TiDBExpression whereCondition) {
        this.leftTable = leftTable;
        this.rightTable = rightTable;
        this.joinType = joinType;
        this.onCondition = whereCondition;
    }

    public TiDBExpression getLeftTable() {
        return leftTable;
    }

    public TiDBExpression getRightTable() {
        return rightTable;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public TiDBExpression getOnCondition() {
        return onCondition;
    }

    public static TiDBJoin createNaturalJoin(TiDBExpression left, TiDBExpression right, NaturalJoinType type) {
        TiDBJoin tiDBJoin = new TiDBJoin(left, right, JoinType.NATURAL, null);
        tiDBJoin.setNaturalJoinType(type);
        return tiDBJoin;
    }

    public static TiDBJoin createInnerJoin(TiDBExpression left, TiDBExpression right, TiDBExpression onClause) {
        return new TiDBJoin(left, right, JoinType.INNER, onClause);
    }

    public static TiDBJoin createLeftOuterJoin(TiDBExpression left, TiDBExpression right, TiDBExpression onClause) {
        return new TiDBJoin(left, right, JoinType.LEFT, onClause);
    }

    public static TiDBJoin createRightOuterJoin(TiDBExpression left, TiDBExpression right, TiDBExpression onClause) {
        return new TiDBJoin(left, right, JoinType.RIGHT, onClause);
    }

    public static TiDBJoin createStraightJoin(TiDBExpression left, TiDBExpression right, TiDBExpression onClause) {
        return new TiDBJoin(left, right, JoinType.STRAIGHT, onClause);
    }

    private void setNaturalJoinType(NaturalJoinType outerType) {
        this.outerType = outerType;
    }

    public NaturalJoinType getNaturalJoinType() {
        return outerType;
    }

    public static List<TiDBExpression> getJoins(List<TiDBExpression> tableList, TiDBGlobalState globalState) {
        List<TiDBExpression> joinExpressions = new ArrayList<>();
        while (tableList.size() >= 2 && Randomly.getBoolean()) {
            TiDBTableReference leftTable = (TiDBTableReference) tableList.remove(0);
            TiDBTableReference rightTable = (TiDBTableReference) tableList.remove(0);
            List<TiDBColumn> columns = new ArrayList<>(leftTable.getTable().getColumns());
            columns.addAll(rightTable.getTable().getColumns());
            TiDBExpressionGenerator joinGen = new TiDBExpressionGenerator(globalState).setColumns(columns);
            switch (TiDBJoin.JoinType.getRandom()) {
            case INNER:
                joinExpressions.add(TiDBJoin.createInnerJoin(leftTable, rightTable, joinGen.generateExpression()));
                break;
            case NATURAL:
                joinExpressions.add(TiDBJoin.createNaturalJoin(leftTable, rightTable, NaturalJoinType.getRandom()));
                break;
            case STRAIGHT:
                joinExpressions.add(TiDBJoin.createStraightJoin(leftTable, rightTable, joinGen.generateExpression()));
                break;
            case LEFT:
                joinExpressions.add(TiDBJoin.createLeftOuterJoin(leftTable, rightTable, joinGen.generateExpression()));
                break;
            case RIGHT:
                joinExpressions.add(TiDBJoin.createRightOuterJoin(leftTable, rightTable, joinGen.generateExpression()));
                break;
            default:
                throw new AssertionError();
            }
        }
        return joinExpressions;
    }

}
