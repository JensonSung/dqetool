package dqetool.postgres.ast;

import dqetool.postgres.PostgresSchema.PostgresDataType;

public interface PostgresExpression {

    default PostgresDataType getExpressionType() {
        return null;
    }

    default PostgresConstant getExpectedValue() {
        return null;
    }
}
