package dqetool.common.query;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLancerResultSet implements Closeable {

    ResultSet rs;
    private Runnable runnableEpilogue;

    public SQLancerResultSet(ResultSet rs) {
        this.rs = rs;
    }

    @Override
    public void close() {
        try {
            if (runnableEpilogue != null) {
                runnableEpilogue.run();
            }
            rs.getStatement().close();
            rs.close();
        } catch (SQLException e) {
            throw new AssertionError(e);
        }
    }

    public boolean next() throws SQLException {
        return rs.next();
    }

    public int getInt(int colIndex) throws SQLException {
        return rs.getInt(colIndex);
    }
    
    public long getLong(int colIndex) throws SQLException {
        return rs.getLong(colIndex);
    }

    public String getString(int colIndex) throws SQLException {
        return rs.getString(colIndex);
    }

    public int getInt(String colName) throws SQLException {
        return rs.getInt(colName);
    }

    public String getString(String colName) throws SQLException {
        return rs.getString(colName);
    }

    public boolean isClosed() throws SQLException {
        return rs.isClosed();
    }

    public void registerEpilogue(Runnable runnableEpilogue) {
        this.runnableEpilogue = runnableEpilogue;
    }

}
