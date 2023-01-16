package dqetool.mariadb.gen;

import java.util.List;

import dqetool.Randomly;
import dqetool.common.DBMSCommon;
import dqetool.common.query.ExpectedErrors;
import dqetool.common.query.SQLQueryAdapter;
import dqetool.mariadb.MariaDBSchema;
import dqetool.mariadb.MariaDBSchema.MariaDBColumn;
import dqetool.mariadb.MariaDBSchema.MariaDBTable;

public final class MariaDBIndexGenerator {

    private MariaDBIndexGenerator() {
    }

    public static SQLQueryAdapter generate(MariaDBSchema s) {
        ExpectedErrors errors = new ExpectedErrors();
        StringBuilder sb = new StringBuilder("CREATE ");
        errors.add("Key/Index cannot be defined on a virtual generated column");
        if (Randomly.getBoolean()) {
            errors.add("Duplicate entry");
            errors.add("Key/Index cannot be defined on a virtual generated column");
            sb.append("UNIQUE ");
        }
        sb.append("INDEX ");
        sb.append("i");
        sb.append(DBMSCommon.createColumnName(Randomly.smallNumber()));
        if (Randomly.getBoolean()) {
            sb.append(" USING ");
            sb.append(Randomly.fromOptions("BTREE", "HASH")); // , "RTREE")
        }

        sb.append(" ON ");
        MariaDBTable randomTable = s.getRandomTable();
        sb.append(randomTable.getName());
        sb.append("(");
        List<MariaDBColumn> columns = Randomly.nonEmptySubset(randomTable.getColumns());
        for (int i = 0; i < columns.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(columns.get(i).getName());
            if (Randomly.getBoolean()) {
                sb.append(" ");
                sb.append(Randomly.fromOptions("ASC", "DESC"));
            }
        }
        sb.append(")");
        // if (Randomly.getBoolean()) {
        // sb.append(" ALGORITHM=");
        // sb.append(Randomly.fromOptions("DEFAULT", "INPLACE", "COPY", "NOCOPY", "INSTANT"));
        // errors.add("is not supported for this operation");
        // }

        return new SQLQueryAdapter(sb.toString(), errors, true);
    }

}
