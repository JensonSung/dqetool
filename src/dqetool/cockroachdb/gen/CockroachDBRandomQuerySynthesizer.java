package dqetool.cockroachdb.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dqetool.Randomly;
import dqetool.cockroachdb.CockroachDBCommon;
import dqetool.cockroachdb.CockroachDBProvider.CockroachDBGlobalState;
import dqetool.cockroachdb.CockroachDBSchema.CockroachDBDataType;
import dqetool.cockroachdb.CockroachDBSchema.CockroachDBTables;
import dqetool.cockroachdb.CockroachDBVisitor;
import dqetool.cockroachdb.ast.CockroachDBExpression;
import dqetool.cockroachdb.ast.CockroachDBSelect;
import dqetool.cockroachdb.ast.CockroachDBTableReference;
import dqetool.common.query.SQLQueryAdapter;

public final class CockroachDBRandomQuerySynthesizer {

    private CockroachDBRandomQuerySynthesizer() {
    }

    public static SQLQueryAdapter generate(CockroachDBGlobalState globalState, int nrColumns) {
        CockroachDBSelect select = generateSelect(globalState, nrColumns);
        return new SQLQueryAdapter(CockroachDBVisitor.asString(select));
    }

    public static CockroachDBSelect generateSelect(CockroachDBGlobalState globalState, int nrColumns) {
        CockroachDBTables tables = globalState.getSchema().getRandomTableNonEmptyTables();
        CockroachDBExpressionGenerator gen = new CockroachDBExpressionGenerator(globalState)
                .setColumns(tables.getColumns());
        CockroachDBSelect select = new CockroachDBSelect();
        select.setDistinct(Randomly.getBoolean());
        boolean allowAggregates = Randomly.getBooleanWithSmallProbability();
        List<CockroachDBExpression> columns = new ArrayList<>();
        List<CockroachDBExpression> columnsWithoutAggregates = new ArrayList<>();
        for (int i = 0; i < nrColumns; i++) {
            if (allowAggregates && Randomly.getBoolean()) {
                CockroachDBExpression expression = gen.generateExpression(CockroachDBDataType.getRandom().get());
                columns.add(expression);
                columnsWithoutAggregates.add(expression);
            } else {
                columns.add(gen.generateAggregate());
            }
        }
        select.setFetchColumns(columns);
        List<CockroachDBTableReference> tableList = tables.getTables().stream()
                .map(t -> new CockroachDBTableReference(t)).collect(Collectors.toList());
        List<CockroachDBExpression> updatedTableList = CockroachDBCommon.getTableReferences(tableList);
        select.setFromList(updatedTableList);
        if (Randomly.getBoolean()) {
            select.setWhereClause(gen.generateExpression(CockroachDBDataType.BOOL.get()));
        }
        if (Randomly.getBoolean()) {
            select.setOrderByExpressions(gen.getOrderingTerms());
        }
        if (Randomly.getBoolean()) {
            select.setGroupByExpressions(gen.generateExpressions(Randomly.smallNumber() + 1));
        }

        if (Randomly.getBoolean()) { // TODO expression
            select.setLimitClause(gen.generateConstant(CockroachDBDataType.INT.get()));
        }
        if (Randomly.getBoolean()) {
            select.setOffsetClause(gen.generateConstant(CockroachDBDataType.INT.get()));
        }
        if (Randomly.getBoolean()) {
            select.setHavingClause(gen.generateHavingClause());
        }
        return select;
    }

}
