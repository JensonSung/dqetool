package dqetool.postgres.ast;

import dqetool.postgres.PostgresSchema.PostgresColumn;
import dqetool.postgres.PostgresSchema.PostgresDataType;

public class PostgresColumnValue implements PostgresExpression {

    private final PostgresColumn c;
    private final PostgresConstant expectedValue;

    public PostgresColumnValue(PostgresColumn c, PostgresConstant expectedValue) {
        this.c = c;
        this.expectedValue = expectedValue;
    }

    @Override
    public PostgresDataType getExpressionType() {
        return c.getType();
    }

    @Override
    public PostgresConstant getExpectedValue() {
        return expectedValue;
    }

    public static PostgresColumnValue create(PostgresColumn c, PostgresConstant expected) {
        return new PostgresColumnValue(c, expected);
    }

    public PostgresColumn getColumn() {
        return c;
    }

}
