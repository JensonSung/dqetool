package dqetool.cockroachdb;

import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import dqetool.DBMSSpecificOptions;
import dqetool.OracleFactory;
import dqetool.cockroachdb.CockroachDBOptions.CockroachDBOracleFactory;
import dqetool.cockroachdb.CockroachDBProvider.CockroachDBGlobalState;
import dqetool.cockroachdb.oracle.CockroachDBDQEOracle;
import dqetool.common.oracle.TestOracle;

@Parameters(separators = "=", commandDescription = "CockroachDB (default port: " + CockroachDBOptions.DEFAULT_PORT
        + " default host: " + CockroachDBOptions.DEFAULT_HOST + ")")
public class CockroachDBOptions implements DBMSSpecificOptions<CockroachDBOracleFactory> {
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 26257;

    @Parameter(names = "--oracle")
    public CockroachDBOracleFactory oracle = CockroachDBOracleFactory.DQE;

    public enum CockroachDBOracleFactory implements OracleFactory<CockroachDBGlobalState> {
        DQE {

            public TestOracle<CockroachDBGlobalState> create(CockroachDBGlobalState globalState) throws Exception {
                return new CockroachDBDQEOracle(globalState);
            }

        },

    }

    @Parameter(names = {
            "--test-hash-indexes" }, description = "Test the USING HASH WITH BUCKET_COUNT=n_buckets option in CREATE INDEX")
    public boolean testHashIndexes = true;

    @Parameter(names = { "--test-temp-tables" }, description = "Test TEMPORARY tables")
    public boolean testTempTables; // default: false https://github.com/cockroachdb/cockroach/issues/85388

    @Parameter(names = { "--max-num-tables" }, description = "The maximum number of tables that can be created")
    public int maxNumTables = 10;

    @Parameter(names = { "--max-num-indexes" }, description = "The maximum number of indexes that can be created")
    public int maxNumIndexes = 20;

    @Override
    public List<CockroachDBOracleFactory> getTestOracleFactory() {
        return Arrays.asList(oracle);
    }

}
