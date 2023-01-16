package dqetool.mysql.ast;

import dqetool.mysql.MySQLSchema.MySQLTable;

public class MySQLTableReference implements MySQLExpression {

    private final MySQLTable table;

    public MySQLTableReference(MySQLTable table) {
        this.table = table;
    }

    public MySQLTable getTable() {
        return table;
    }

}
