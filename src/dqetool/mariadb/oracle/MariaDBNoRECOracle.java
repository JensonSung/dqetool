package dqetool.mariadb.oracle;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dqetool.IgnoreMeException;
import dqetool.common.oracle.NoRECBase;
import dqetool.common.oracle.TestOracle;
import dqetool.common.query.SQLQueryAdapter;
import dqetool.common.query.SQLancerResultSet;
import dqetool.mariadb.MariaDBProvider.MariaDBGlobalState;
import dqetool.mariadb.MariaDBSchema;
import dqetool.mariadb.MariaDBSchema.MariaDBColumn;
import dqetool.mariadb.MariaDBSchema.MariaDBDataType;
import dqetool.mariadb.MariaDBSchema.MariaDBTable;
import dqetool.mariadb.ast.MariaDBAggregate;
import dqetool.mariadb.ast.MariaDBAggregate.MariaDBAggregateFunction;
import dqetool.mariadb.ast.MariaDBColumnName;
import dqetool.mariadb.ast.MariaDBExpression;
import dqetool.mariadb.ast.MariaDBPostfixUnaryOperation;
import dqetool.mariadb.ast.MariaDBPostfixUnaryOperation.MariaDBPostfixUnaryOperator;
import dqetool.mariadb.ast.MariaDBSelectStatement;
import dqetool.mariadb.ast.MariaDBSelectStatement.MariaDBSelectType;
import dqetool.mariadb.ast.MariaDBText;
import dqetool.mariadb.ast.MariaDBVisitor;
import dqetool.mariadb.gen.MariaDBExpressionGenerator;

public class MariaDBNoRECOracle extends NoRECBase<MariaDBGlobalState> implements TestOracle<MariaDBGlobalState> {

    private final MariaDBSchema s;
    private static final int NOT_FOUND = -1;

    public MariaDBNoRECOracle(MariaDBGlobalState globalState) {
        super(globalState);
        this.s = globalState.getSchema();
        errors.add("is out of range");
        // regex
        errors.add("unmatched parentheses");
        errors.add("nothing to repeat at offset");
        errors.add("missing )");
        errors.add("missing terminating ]");
        errors.add("range out of order in character class");
        errors.add("unrecognized character after ");
        errors.add("Got error '(*VERB) not recognized or malformed");
        errors.add("must be followed by");
        errors.add("malformed number or name after");
        errors.add("digit expected after");
    }

    public void check() throws SQLException {
        MariaDBTable randomTable = s.getRandomTable();
        List<MariaDBColumn> columns = randomTable.getColumns();
        MariaDBExpressionGenerator gen = new MariaDBExpressionGenerator(state.getRandomly()).setColumns(columns)
                .setCon(con).setState(state.getState());
        MariaDBExpression randomWhereCondition = gen.getRandomExpression();
        List<MariaDBExpression> groupBys = Collections.emptyList(); // getRandomExpressions(columns);
        int optimizedCount = getOptimizedQuery(randomTable, randomWhereCondition, groupBys);
        int unoptimizedCount = getUnoptimizedQuery(randomTable, randomWhereCondition, groupBys);
        if (optimizedCount == NOT_FOUND || unoptimizedCount == NOT_FOUND) {
            throw new IgnoreMeException();
        }
        if (optimizedCount != unoptimizedCount) {
            state.getState().getLocalState().log(optimizedQueryString + ";\n" + unoptimizedQueryString + ";");
            throw new AssertionError(optimizedCount + " " + unoptimizedCount);
        }
    }

    private int getUnoptimizedQuery(MariaDBTable randomTable, MariaDBExpression randomWhereCondition,
            List<MariaDBExpression> groupBys) throws SQLException {
        MariaDBSelectStatement select = new MariaDBSelectStatement();
        select.setGroupByClause(groupBys);
        MariaDBPostfixUnaryOperation isTrue = new MariaDBPostfixUnaryOperation(MariaDBPostfixUnaryOperator.IS_TRUE,
                randomWhereCondition);
        MariaDBText asText = new MariaDBText(isTrue, " as count", false);
        select.setFetchColumns(Arrays.asList(asText));
        select.setFromTables(Arrays.asList(randomTable));
        select.setSelectType(MariaDBSelectType.ALL);
        int secondCount = 0;

        unoptimizedQueryString = "SELECT SUM(count) FROM (" + MariaDBVisitor.asString(select) + ") as asdf";
        SQLQueryAdapter q = new SQLQueryAdapter(unoptimizedQueryString, errors);
        try (SQLancerResultSet rs = q.executeAndGet(state)) {
            if (rs == null) {
                return NOT_FOUND;
            } else {
                while (rs.next()) {
                    secondCount = rs.getInt(1);
                }
            }
        }

        return secondCount;
    }

    private int getOptimizedQuery(MariaDBTable randomTable, MariaDBExpression randomWhereCondition,
            List<MariaDBExpression> groupBys) throws SQLException {
        MariaDBSelectStatement select = new MariaDBSelectStatement();
        select.setGroupByClause(groupBys);
        MariaDBAggregate aggr = new MariaDBAggregate(
                new MariaDBColumnName(new MariaDBColumn("*", MariaDBDataType.INT, false, 0)),
                MariaDBAggregateFunction.COUNT);
        select.setFetchColumns(Arrays.asList(aggr));
        select.setFromTables(Arrays.asList(randomTable));
        select.setWhereClause(randomWhereCondition);
        select.setSelectType(MariaDBSelectType.ALL);
        int firstCount;
        optimizedQueryString = MariaDBVisitor.asString(select);
        SQLQueryAdapter q = new SQLQueryAdapter(optimizedQueryString, errors);
        try (SQLancerResultSet rs = q.executeAndGet(state)) {
            if (rs == null) {
                firstCount = NOT_FOUND;
            } else {
                rs.next();
                firstCount = rs.getInt(1);
            }
        } catch (Exception e) {
            throw new AssertionError(optimizedQueryString, e);
        }
        return firstCount;
    }

}
