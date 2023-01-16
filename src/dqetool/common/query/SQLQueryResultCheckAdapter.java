package dqetool.common.query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;

import dqetool.GlobalState;
import dqetool.SQLConnection;

public class SQLQueryResultCheckAdapter extends SQLQueryAdapter {

    private final Consumer<ResultSet> rsChecker;

    public SQLQueryResultCheckAdapter(String query, Consumer<ResultSet> rsChecker) {
        super(query);
        this.rsChecker = rsChecker;
    }

    @Override
    public <G extends GlobalState<?, ?, SQLConnection>> boolean execute(G globalState, String... fills)
            throws SQLException {
        try (Statement s = globalState.getConnection().createStatement()) {
            ResultSet rs = s.executeQuery(getQueryString());
            rsChecker.accept(rs);
            return true;
        } catch (Exception e) {
            checkException(e);
            return false;
        }
    }

}
