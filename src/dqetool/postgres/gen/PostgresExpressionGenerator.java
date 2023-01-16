package dqetool.postgres.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dqetool.IgnoreMeException;
import dqetool.Randomly;
import dqetool.common.gen.ExpressionGenerator;
import dqetool.postgres.PostgresCompoundDataType;
import dqetool.postgres.PostgresGlobalState;
import dqetool.postgres.PostgresProvider;
import dqetool.postgres.PostgresSchema.PostgresColumn;
import dqetool.postgres.PostgresSchema.PostgresDataType;
import dqetool.postgres.PostgresSchema.PostgresRowValue;
import dqetool.postgres.ast.PostgresAggregate;
import dqetool.postgres.ast.PostgresAggregate.PostgresAggregateFunction;
import dqetool.postgres.ast.PostgresBetweenOperation;
import dqetool.postgres.ast.PostgresBinaryArithmeticOperation;
import dqetool.postgres.ast.PostgresBinaryArithmeticOperation.PostgresBinaryOperator;
import dqetool.postgres.ast.PostgresBinaryBitOperation;
import dqetool.postgres.ast.PostgresBinaryBitOperation.PostgresBinaryBitOperator;
import dqetool.postgres.ast.PostgresBinaryComparisonOperation;
import dqetool.postgres.ast.PostgresBinaryLogicalOperation;
import dqetool.postgres.ast.PostgresBinaryLogicalOperation.BinaryLogicalOperator;
import dqetool.postgres.ast.PostgresBinaryRangeOperation;
import dqetool.postgres.ast.PostgresBinaryRangeOperation.PostgresBinaryRangeComparisonOperator;
import dqetool.postgres.ast.PostgresBinaryRangeOperation.PostgresBinaryRangeOperator;
import dqetool.postgres.ast.PostgresCastOperation;
import dqetool.postgres.ast.PostgresCollate;
import dqetool.postgres.ast.PostgresColumnValue;
import dqetool.postgres.ast.PostgresConcatOperation;
import dqetool.postgres.ast.PostgresConstant;
import dqetool.postgres.ast.PostgresExpression;
import dqetool.postgres.ast.PostgresFunction;
import dqetool.postgres.ast.PostgresFunction.PostgresFunctionWithResult;
import dqetool.postgres.ast.PostgresFunctionWithUnknownResult;
import dqetool.postgres.ast.PostgresInOperation;
import dqetool.postgres.ast.PostgresLikeOperation;
import dqetool.postgres.ast.PostgresOrderByTerm;
import dqetool.postgres.ast.PostgresOrderByTerm.PostgresOrder;
import dqetool.postgres.ast.PostgresPOSIXRegularExpression;
import dqetool.postgres.ast.PostgresPOSIXRegularExpression.POSIXRegex;
import dqetool.postgres.ast.PostgresPostfixOperation;
import dqetool.postgres.ast.PostgresPostfixOperation.PostfixOperator;
import dqetool.postgres.ast.PostgresPrefixOperation;
import dqetool.postgres.ast.PostgresPrefixOperation.PrefixOperator;
import dqetool.postgres.ast.PostgresSimilarTo;

public class PostgresExpressionGenerator implements ExpressionGenerator<PostgresExpression> {

    private final int maxDepth;

    private final Randomly r;

    private List<PostgresColumn> columns;

    private PostgresRowValue rw;

    private boolean expectedResult;

    private PostgresGlobalState globalState;

    private boolean allowAggregateFunctions;

    private final Map<String, Character> functionsAndTypes;

    private final List<Character> allowedFunctionTypes;

    public PostgresExpressionGenerator(PostgresGlobalState globalState) {
        this.r = globalState.getRandomly();
        this.maxDepth = globalState.getOptions().getMaxExpressionDepth();
        this.globalState = globalState;
        this.functionsAndTypes = globalState.getFunctionsAndTypes();
        this.allowedFunctionTypes = globalState.getAllowedFunctionTypes();
    }

    public PostgresExpressionGenerator setColumns(List<PostgresColumn> columns) {
        this.columns = columns;
        return this;
    }

    public PostgresExpressionGenerator setRowValue(PostgresRowValue rw) {
        this.rw = rw;
        return this;
    }

    public PostgresExpression generateExpression(int depth) {
        return generateExpression(depth, PostgresDataType.getRandomType());
    }

    public List<PostgresExpression> generateOrderBy() {
        List<PostgresExpression> orderBys = new ArrayList<>();
        for (int i = 0; i < Randomly.smallNumber(); i++) {
            orderBys.add(new PostgresOrderByTerm(PostgresColumnValue.create(Randomly.fromList(columns), null),
                    PostgresOrder.getRandomOrder()));
        }
        return orderBys;
    }

    private enum BooleanExpression {
        POSTFIX_OPERATOR, NOT, BINARY_LOGICAL_OPERATOR, BINARY_COMPARISON, FUNCTION, CAST, LIKE, BETWEEN, IN_OPERATION,
        SIMILAR_TO, POSIX_REGEX, BINARY_RANGE_COMPARISON;
    }

    private PostgresExpression generateFunctionWithUnknownResult(int depth, PostgresDataType type) {
        List<PostgresFunctionWithUnknownResult> supportedFunctions = PostgresFunctionWithUnknownResult
                .getSupportedFunctions(type);
        // filters functions by allowed type (STABLE 's', IMMUTABLE 'i', VOLATILE 'v')
        supportedFunctions = supportedFunctions.stream()
                .filter(f -> allowedFunctionTypes.contains(functionsAndTypes.get(f.getName())))
                .collect(Collectors.toList());
        if (supportedFunctions.isEmpty()) {
            throw new IgnoreMeException();
        }
        PostgresFunctionWithUnknownResult randomFunction = Randomly.fromList(supportedFunctions);
        return new PostgresFunction(randomFunction, type, randomFunction.getArguments(type, this, depth + 1));
    }

    private PostgresExpression generateFunctionWithKnownResult(int depth, PostgresDataType type) {
        List<PostgresFunctionWithResult> functions = Stream.of(PostgresFunction.PostgresFunctionWithResult.values())
                .filter(f -> f.supportsReturnType(type)).collect(Collectors.toList());
        // filters functions by allowed type (STABLE 's', IMMUTABLE 'i', VOLATILE 'v')
        functions = functions.stream().filter(f -> allowedFunctionTypes.contains(functionsAndTypes.get(f.getName())))
                .collect(Collectors.toList());
        if (functions.isEmpty()) {
            throw new IgnoreMeException();
        }
        PostgresFunctionWithResult randomFunction = Randomly.fromList(functions);
        int nrArgs = randomFunction.getNrArgs();
        if (randomFunction.isVariadic()) {
            nrArgs += Randomly.smallNumber();
        }
        PostgresDataType[] argTypes = randomFunction.getInputTypesForReturnType(type, nrArgs);
        PostgresExpression[] args = new PostgresExpression[nrArgs];
        do {
            for (int i = 0; i < args.length; i++) {
                args[i] = generateExpression(depth + 1, argTypes[i]);
            }
        } while (!randomFunction.checkArguments(args));
        return new PostgresFunction(randomFunction, type, args);
    }

    private PostgresExpression generateBooleanExpression(int depth) {
        List<BooleanExpression> validOptions = new ArrayList<>(Arrays.asList(BooleanExpression.values()));
        if (PostgresProvider.generateOnlyKnown) {
            validOptions.remove(BooleanExpression.SIMILAR_TO);
            validOptions.remove(BooleanExpression.POSIX_REGEX);
            validOptions.remove(BooleanExpression.BINARY_RANGE_COMPARISON);
        }
        BooleanExpression option = Randomly.fromList(validOptions);
        switch (option) {
        case POSTFIX_OPERATOR:
            PostfixOperator random = PostfixOperator.getRandom();
            return PostgresPostfixOperation
                    .create(generateExpression(depth + 1, Randomly.fromOptions(random.getInputDataTypes())), random);
        case IN_OPERATION:
            return inOperation(depth + 1);
        case NOT:
            return new PostgresPrefixOperation(generateExpression(depth + 1, PostgresDataType.BOOLEAN),
                    PrefixOperator.NOT);
        case BINARY_LOGICAL_OPERATOR:
            PostgresExpression first = generateExpression(depth + 1, PostgresDataType.BOOLEAN);
            int nr = Randomly.smallNumber() + 1;
            for (int i = 0; i < nr; i++) {
                first = new PostgresBinaryLogicalOperation(first,
                        generateExpression(depth + 1, PostgresDataType.BOOLEAN), BinaryLogicalOperator.getRandom());
            }
            return first;
        case BINARY_COMPARISON:
            PostgresDataType dataType = getMeaningfulType();
            return generateComparison(depth, dataType);
        case CAST:
            return new PostgresCastOperation(generateExpression(depth + 1),
                    getCompoundDataType(PostgresDataType.BOOLEAN));
        case FUNCTION:
            return generateFunction(depth + 1, PostgresDataType.BOOLEAN);
        case LIKE:
            return new PostgresLikeOperation(generateExpression(depth + 1, PostgresDataType.TEXT),
                    generateExpression(depth + 1, PostgresDataType.TEXT));
        case BETWEEN:
            PostgresDataType type = getMeaningfulType();
            return new PostgresBetweenOperation(generateExpression(depth + 1, type),
                    generateExpression(depth + 1, type), generateExpression(depth + 1, type), Randomly.getBoolean());
        case SIMILAR_TO:
            assert !expectedResult;
            // TODO also generate the escape character
            return new PostgresSimilarTo(generateExpression(depth + 1, PostgresDataType.TEXT),
                    generateExpression(depth + 1, PostgresDataType.TEXT), null);
        case POSIX_REGEX:
            assert !expectedResult;
            return new PostgresPOSIXRegularExpression(generateExpression(depth + 1, PostgresDataType.TEXT),
                    generateExpression(depth + 1, PostgresDataType.TEXT), POSIXRegex.getRandom());
        case BINARY_RANGE_COMPARISON:
            // TODO element check
            return new PostgresBinaryRangeOperation(PostgresBinaryRangeComparisonOperator.getRandom(),
                    generateExpression(depth + 1, PostgresDataType.RANGE),
                    generateExpression(depth + 1, PostgresDataType.RANGE));
        default:
            throw new AssertionError();
        }
    }

    private PostgresDataType getMeaningfulType() {
        // make it more likely that the expression does not only consist of constant
        // expressions
        if (Randomly.getBooleanWithSmallProbability() || columns == null || columns.isEmpty()) {
            return PostgresDataType.getRandomType();
        } else {
            return Randomly.fromList(columns).getType();
        }
    }

    private PostgresExpression generateFunction(int depth, PostgresDataType type) {
        if (PostgresProvider.generateOnlyKnown || Randomly.getBoolean()) {
            return generateFunctionWithKnownResult(depth, type);
        } else {
            return generateFunctionWithUnknownResult(depth, type);
        }
    }

    private PostgresExpression generateComparison(int depth, PostgresDataType dataType) {
        PostgresExpression leftExpr = generateExpression(depth + 1, dataType);
        PostgresExpression rightExpr = generateExpression(depth + 1, dataType);
        return getComparison(leftExpr, rightExpr);
    }

    private PostgresExpression getComparison(PostgresExpression leftExpr, PostgresExpression rightExpr) {
        PostgresBinaryComparisonOperation op = new PostgresBinaryComparisonOperation(leftExpr, rightExpr,
                PostgresBinaryComparisonOperation.PostgresBinaryComparisonOperator.getRandom());
        if (PostgresProvider.generateOnlyKnown && op.getLeft().getExpressionType() == PostgresDataType.TEXT
                && op.getRight().getExpressionType() == PostgresDataType.TEXT) {
            return new PostgresCollate(op, "C");
        }
        return op;
    }

    private PostgresExpression inOperation(int depth) {
        PostgresDataType type = PostgresDataType.getRandomType();
        PostgresExpression leftExpr = generateExpression(depth + 1, type);
        List<PostgresExpression> rightExpr = new ArrayList<>();
        for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
            rightExpr.add(generateExpression(depth + 1, type));
        }
        return new PostgresInOperation(leftExpr, rightExpr, Randomly.getBoolean());
    }

    public static PostgresExpression generateExpression(PostgresGlobalState globalState, PostgresDataType type) {
        return new PostgresExpressionGenerator(globalState).generateExpression(0, type);
    }

    public PostgresExpression generateExpression(int depth, PostgresDataType originalType) {
        PostgresDataType dataType = originalType;
        if (dataType == PostgresDataType.REAL && Randomly.getBoolean()) {
            dataType = Randomly.fromOptions(PostgresDataType.INT, PostgresDataType.FLOAT);
        }
        if (dataType == PostgresDataType.FLOAT && Randomly.getBoolean()) {
            dataType = PostgresDataType.INT;
        }
        if (!filterColumns(dataType).isEmpty() && Randomly.getBoolean()) {
            return potentiallyWrapInCollate(dataType, createColumnOfType(dataType));
        }
        PostgresExpression exprInternal = generateExpressionInternal(depth, dataType);
        return potentiallyWrapInCollate(dataType, exprInternal);
    }

    private PostgresExpression potentiallyWrapInCollate(PostgresDataType dataType, PostgresExpression exprInternal) {
        if (dataType == PostgresDataType.TEXT && PostgresProvider.generateOnlyKnown) {
            return new PostgresCollate(exprInternal, "C");
        } else {
            return exprInternal;
        }
    }

    private PostgresExpression generateExpressionInternal(int depth, PostgresDataType dataType) throws AssertionError {
        if (allowAggregateFunctions && Randomly.getBoolean()) {
            allowAggregateFunctions = false; // aggregate function calls cannot be nested
            return getAggregate(dataType);
        }
        if (Randomly.getBooleanWithRatherLowProbability() || depth > maxDepth) {
            // generic expression
            if (Randomly.getBoolean() || depth > maxDepth) {
                if (Randomly.getBooleanWithRatherLowProbability()) {
                    return generateConstant(r, dataType);
                } else {
                    if (filterColumns(dataType).isEmpty()) {
                        return generateConstant(r, dataType);
                    } else {
                        return createColumnOfType(dataType);
                    }
                }
            } else {
                if (Randomly.getBoolean()) {
                    return new PostgresCastOperation(generateExpression(depth + 1), getCompoundDataType(dataType));
                } else {
                    return generateFunctionWithUnknownResult(depth, dataType);
                }
            }
        } else {
            switch (dataType) {
            case BOOLEAN:
                return generateBooleanExpression(depth);
            case INT:
                return generateIntExpression(depth);
            case TEXT:
                return generateTextExpression(depth);
            case DECIMAL:
            case REAL:
            case FLOAT:
            case MONEY:
            case INET:
                return generateConstant(r, dataType);
            case BIT:
                return generateBitExpression(depth);
            case RANGE:
                return generateRangeExpression(depth);
            default:
                throw new AssertionError(dataType);
            }
        }
    }

    private static PostgresCompoundDataType getCompoundDataType(PostgresDataType type) {
        switch (type) {
        case BOOLEAN:
        case DECIMAL: // TODO
        case FLOAT:
        case INT:
        case MONEY:
        case RANGE:
        case REAL:
        case INET:
            return PostgresCompoundDataType.create(type);
        case TEXT: // TODO
        case BIT:
            if (Randomly.getBoolean() || PostgresProvider.generateOnlyKnown /*
                                                                             * The PQS implementation does not check for
                                                                             * size specifications
                                                                             */) {
                return PostgresCompoundDataType.create(type);
            } else {
                return PostgresCompoundDataType.create(type, (int) Randomly.getNotCachedInteger(1, 1000));
            }
        default:
            throw new AssertionError(type);
        }

    }

    private enum RangeExpression {
        BINARY_OP;
    }

    private PostgresExpression generateRangeExpression(int depth) {
        RangeExpression option;
        List<RangeExpression> validOptions = new ArrayList<>(Arrays.asList(RangeExpression.values()));
        option = Randomly.fromList(validOptions);
        switch (option) {
        case BINARY_OP:
            return new PostgresBinaryRangeOperation(PostgresBinaryRangeOperator.getRandom(),
                    generateExpression(depth + 1, PostgresDataType.RANGE),
                    generateExpression(depth + 1, PostgresDataType.RANGE));
        default:
            throw new AssertionError(option);
        }
    }

    private enum TextExpression {
        CAST, FUNCTION, CONCAT, COLLATE
    }

    private PostgresExpression generateTextExpression(int depth) {
        TextExpression option;
        List<TextExpression> validOptions = new ArrayList<>(Arrays.asList(TextExpression.values()));
        if (expectedResult) {
            validOptions.remove(TextExpression.COLLATE);
        }
        if (!globalState.getDbmsSpecificOptions().testCollations) {
            validOptions.remove(TextExpression.COLLATE);
        }
        option = Randomly.fromList(validOptions);

        switch (option) {
        case CAST:
            return new PostgresCastOperation(generateExpression(depth + 1), getCompoundDataType(PostgresDataType.TEXT));
        case FUNCTION:
            return generateFunction(depth + 1, PostgresDataType.TEXT);
        case CONCAT:
            return generateConcat(depth);
        case COLLATE:
            assert !expectedResult;
            return new PostgresCollate(generateExpression(depth + 1, PostgresDataType.TEXT), globalState == null
                    ? Randomly.fromOptions("C", "POSIX", "de_CH.utf8", "es_CR.utf8") : globalState.getRandomCollate());
        default:
            throw new AssertionError();
        }
    }

    private PostgresExpression generateConcat(int depth) {
        PostgresExpression left = generateExpression(depth + 1, PostgresDataType.TEXT);
        PostgresExpression right = generateExpression(depth + 1);
        return new PostgresConcatOperation(left, right);
    }

    private enum BitExpression {
        BINARY_OPERATION
    };

    private PostgresExpression generateBitExpression(int depth) {
        BitExpression option;
        option = Randomly.fromOptions(BitExpression.values());
        switch (option) {
        case BINARY_OPERATION:
            return new PostgresBinaryBitOperation(PostgresBinaryBitOperator.getRandom(),
                    generateExpression(depth + 1, PostgresDataType.BIT),
                    generateExpression(depth + 1, PostgresDataType.BIT));
        default:
            throw new AssertionError();
        }
    }

    private enum IntExpression {
        UNARY_OPERATION, FUNCTION, CAST, BINARY_ARITHMETIC_EXPRESSION
    }

    private PostgresExpression generateIntExpression(int depth) {
        IntExpression option;
        option = Randomly.fromOptions(IntExpression.values());
        switch (option) {
        case CAST:
            return new PostgresCastOperation(generateExpression(depth + 1), getCompoundDataType(PostgresDataType.INT));
        case UNARY_OPERATION:
            PostgresExpression intExpression = generateExpression(depth + 1, PostgresDataType.INT);
            return new PostgresPrefixOperation(intExpression,
                    Randomly.getBoolean() ? PrefixOperator.UNARY_PLUS : PrefixOperator.UNARY_MINUS);
        case FUNCTION:
            return generateFunction(depth + 1, PostgresDataType.INT);
        case BINARY_ARITHMETIC_EXPRESSION:
            return new PostgresBinaryArithmeticOperation(generateExpression(depth + 1, PostgresDataType.INT),
                    generateExpression(depth + 1, PostgresDataType.INT), PostgresBinaryOperator.getRandom());
        default:
            throw new AssertionError();
        }
    }

    private PostgresExpression createColumnOfType(PostgresDataType type) {
        List<PostgresColumn> columns = filterColumns(type);
        PostgresColumn fromList = Randomly.fromList(columns);
        PostgresConstant value = rw == null ? null : rw.getValues().get(fromList);
        return PostgresColumnValue.create(fromList, value);
    }

    final List<PostgresColumn> filterColumns(PostgresDataType type) {
        if (columns == null) {
            return Collections.emptyList();
        } else {
            return columns.stream().filter(c -> c.getType() == type).collect(Collectors.toList());
        }
    }

    public PostgresExpression generateExpressionWithExpectedResult(PostgresDataType type) {
        this.expectedResult = true;
        PostgresExpressionGenerator gen = new PostgresExpressionGenerator(globalState).setColumns(columns)
                .setRowValue(rw);
        PostgresExpression expr;
        do {
            expr = gen.generateExpression(type);
        } while (expr.getExpectedValue() == null);
        return expr;
    }

    public static PostgresExpression generateConstant(Randomly r, PostgresDataType type) {
        if (Randomly.getBooleanWithSmallProbability()) {
            return PostgresConstant.createNullConstant();
        }
        // if (Randomly.getBooleanWithSmallProbability()) {
        // return PostgresConstant.createTextConstant(r.getString());
        // }
        switch (type) {
        case INT:
            if (Randomly.getBooleanWithSmallProbability()) {
                return PostgresConstant.createTextConstant(String.valueOf(r.getInteger()));
            } else {
                return PostgresConstant.createIntConstant(r.getInteger());
            }
        case BOOLEAN:
            if (Randomly.getBooleanWithSmallProbability() && !PostgresProvider.generateOnlyKnown) {
                return PostgresConstant
                        .createTextConstant(Randomly.fromOptions("TR", "TRUE", "FA", "FALSE", "0", "1", "ON", "off"));
            } else {
                return PostgresConstant.createBooleanConstant(Randomly.getBoolean());
            }
        case TEXT:
            return PostgresConstant.createTextConstant(r.getString());
        case DECIMAL:
            return PostgresConstant.createDecimalConstant(r.getRandomBigDecimal());
        case FLOAT:
            return PostgresConstant.createFloatConstant((float) r.getDouble());
        case REAL:
            return PostgresConstant.createDoubleConstant(r.getDouble());
        case RANGE:
            return PostgresConstant.createRange(r.getInteger(), Randomly.getBoolean(), r.getInteger(),
                    Randomly.getBoolean());
        case MONEY:
            return new PostgresCastOperation(generateConstant(r, PostgresDataType.FLOAT),
                    getCompoundDataType(PostgresDataType.MONEY));
        case INET:
            return PostgresConstant.createInetConstant(getRandomInet(r));
        case BIT:
            return PostgresConstant.createBitConstant(r.getInteger());
        default:
            throw new AssertionError(type);
        }
    }

    private static String getRandomInet(Randomly r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i != 0) {
                sb.append('.');
            }
            sb.append(r.getInteger() & 255);
        }
        return sb.toString();
    }

    public static PostgresExpression generateExpression(PostgresGlobalState globalState, List<PostgresColumn> columns,
            PostgresDataType type) {
        return new PostgresExpressionGenerator(globalState).setColumns(columns).generateExpression(0, type);
    }

    public static PostgresExpression generateExpression(PostgresGlobalState globalState, List<PostgresColumn> columns) {
        return new PostgresExpressionGenerator(globalState).setColumns(columns).generateExpression(0);

    }

    public List<PostgresExpression> generateExpressions(int nr) {
        List<PostgresExpression> expressions = new ArrayList<>();
        for (int i = 0; i < nr; i++) {
            expressions.add(generateExpression(0));
        }
        return expressions;
    }

    public PostgresExpression generateExpression(PostgresDataType dataType) {
        return generateExpression(0, dataType);
    }

    public PostgresExpressionGenerator setGlobalState(PostgresGlobalState globalState) {
        this.globalState = globalState;
        return this;
    }

    public PostgresExpression generateHavingClause() {
        this.allowAggregateFunctions = true;
        PostgresExpression expression = generateExpression(PostgresDataType.BOOLEAN);
        this.allowAggregateFunctions = false;
        return expression;
    }

    public PostgresExpression generateAggregate() {
        return getAggregate(PostgresDataType.getRandomType());
    }

    private PostgresExpression getAggregate(PostgresDataType dataType) {
        List<PostgresAggregateFunction> aggregates = PostgresAggregateFunction.getAggregates(dataType);
        PostgresAggregateFunction agg = Randomly.fromList(aggregates);
        return generateArgsForAggregate(dataType, agg);
    }

    public PostgresAggregate generateArgsForAggregate(PostgresDataType dataType, PostgresAggregateFunction agg) {
        List<PostgresDataType> types = agg.getTypes(dataType);
        List<PostgresExpression> args = new ArrayList<>();
        for (PostgresDataType argType : types) {
            args.add(generateExpression(argType));
        }
        return new PostgresAggregate(args, agg);
    }

    public PostgresExpressionGenerator allowAggregates(boolean value) {
        allowAggregateFunctions = value;
        return this;
    }

    @Override
    public PostgresExpression generatePredicate() {
        return generateExpression(PostgresDataType.BOOLEAN);
    }

    @Override
    public PostgresExpression negatePredicate(PostgresExpression predicate) {
        return new PostgresPrefixOperation(predicate, PostgresPrefixOperation.PrefixOperator.NOT);
    }

    @Override
    public PostgresExpression isNull(PostgresExpression expr) {
        return new PostgresPostfixOperation(expr, PostfixOperator.IS_NULL);
    }

}
