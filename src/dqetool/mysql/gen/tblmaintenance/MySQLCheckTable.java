package dqetool.mysql.gen.tblmaintenance;

import java.util.List;
import java.util.stream.Collectors;

import dqetool.Randomly;
import dqetool.common.query.SQLQueryAdapter;
import dqetool.mysql.MySQLGlobalState;
import dqetool.mysql.MySQLSchema.MySQLTable;

/**
 * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/check-table.html">CHECK TABLE Statement</a>
 */
public class MySQLCheckTable {

    private final List<MySQLTable> tables;
    private final StringBuilder sb = new StringBuilder();

    public MySQLCheckTable(List<MySQLTable> tables) {
        this.tables = tables;
    }

    public static SQLQueryAdapter check(MySQLGlobalState globalState) {
        return new MySQLCheckTable(globalState.getSchema().getDatabaseTablesRandomSubsetNotEmpty()).generate();
    }

    // CHECK TABLE tbl_name [, tbl_name] ... [option] ...
    //
    // option: {
    // FOR UPGRADE
    // | QUICK
    // | FAST
    // | MEDIUM
    // | EXTENDED
    // | CHANGED
    // }
    private SQLQueryAdapter generate() {
        sb.append("CHECK TABLE ");
        sb.append(tables.stream().map(t -> t.getName()).collect(Collectors.joining(", ")));
        sb.append(" ");
        List<String> options = Randomly.subset("FOR UPGRADE", "QUICK", "FAST", "MEDIUM", "EXTENDED", "CHANGED");
        sb.append(options.stream().collect(Collectors.joining(" ")));
        return new SQLQueryAdapter(sb.toString());
    }

}
