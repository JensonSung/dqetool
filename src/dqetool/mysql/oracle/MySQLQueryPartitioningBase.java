package dqetool.mysql.oracle;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import dqetool.common.gen.ExpressionGenerator;
import dqetool.common.oracle.TernaryLogicPartitioningOracleBase;
import dqetool.common.oracle.TestOracle;
import dqetool.mysql.MySQLErrors;
import dqetool.mysql.MySQLGlobalState;
import dqetool.mysql.MySQLSchema;
import dqetool.mysql.MySQLSchema.MySQLTable;
import dqetool.mysql.MySQLSchema.MySQLTables;
import dqetool.mysql.ast.MySQLColumnReference;
import dqetool.mysql.ast.MySQLExpression;
import dqetool.mysql.ast.MySQLSelect;
import dqetool.mysql.ast.MySQLTableReference;
import dqetool.mysql.gen.MySQLExpressionGenerator;

public abstract class MySQLQueryPartitioningBase extends
        TernaryLogicPartitioningOracleBase<MySQLExpression, MySQLGlobalState> implements TestOracle<MySQLGlobalState> {

    MySQLSchema s;
    MySQLTables targetTables;
    MySQLExpressionGenerator gen;
    MySQLSelect select;

    public MySQLQueryPartitioningBase(MySQLGlobalState state) {
        super(state);
        MySQLErrors.addExpressionErrors(errors);
    }

    public void check() throws SQLException {
        s = state.getSchema();
        targetTables = s.getRandomTableNonEmptyTables();
        gen = new MySQLExpressionGenerator(state).setColumns(targetTables.getColumns());
        initializeTernaryPredicateVariants();
        select = new MySQLSelect();
        select.setFetchColumns(generateFetchColumns());
        List<MySQLTable> tables = targetTables.getTables();
        List<MySQLExpression> tableList = tables.stream().map(t -> new MySQLTableReference(t))
                .collect(Collectors.toList());
        // List<MySQLExpression> joins = MySQLJoin.getJoins(tableList, state);
        select.setFromList(tableList);
        select.setWhereClause(null);
        // select.setJoins(joins);
    }

    List<MySQLExpression> generateFetchColumns() {
        return Arrays.asList(MySQLColumnReference.create(targetTables.getColumns().get(0), null));
    }

    @Override
    protected ExpressionGenerator<MySQLExpression> getGen() {
        return gen;
    }

}
