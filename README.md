
# DQETool

DQETool is the implementation of differential query execution in paper.

# Getting Started

Requirements:
* Java 11 or above
* [Maven](https://maven.apache.org/) (`sudo apt install maven` on Ubuntu)
* The DBMS that you want to test (currently support MariaDB, MySQL, TiDB, SQLite, CockroachDB)
* SQLite is an embedded database, which does not need extra setup and does not require connection parameters
* Other databases like MySQL, which requires connection parameters and needs to create a database named test
```
cd dqetool
mvn package -DskipTests
cd target
java -jar dqetool-*.jar sqlite3
java -jar dqetool-*.jar --host hostString --port portNumber --username usernameString --password passwordString databaseName
java -jar dqetool-*.jar --host 127.0.0.1 --port 4000 --username root --password '' tidb
java -jar dqetool-*.jar --host 127.0.0.1 --port 26257 --username cockroach --password cockroach cockroachdb
```

# Bug List
| ID | Database  | Version | Issue                                                                    | Status    |
| -- | --------- | ------- | ------------------------------------------------------------------------ | --------- |
| 1  | TiDB      | 5.2.0   | [TiDB#27648](https://github.com/pingcap/tidb/issues/27648)               | Fixed     |
| 2  | TiDB      | 5.2.1   | [TiDB#28012](https://github.com/pingcap/tidb/issues/28012)               | Fixed     |
| 3  | TiDB      | 5.2.1   | [TiDB#28154](https://github.com/pingcap/tidb/issues/28154)               | Fixed     |
| 4  | TiDB      | 5.2.1   | [TiDB#28273](https://github.com/pingcap/tidb/issues/28273)               | Fixed     |
| 5  | TiDB      | 5.3.0   | [TiDB#31708](https://github.com/pingcap/tidb/issues/31708)               | Fixed     |
| 6  | TiDB      | 5.4.0   | [TiDB#32480](https://github.com/pingcap/tidb/issues/32480)               | Fixed     |
| 7  | TiDB      | 5.4.0   | [TiDB#32556](https://github.com/pingcap/tidb/issues/32556)               | Fixed     |
| 8  | TIDB      | 5.4.0   | [TiDB#32614](https://github.com/pingcap/tidb/issues/32614)               | Fixed     |
| 9  | TiDB      | 5.4.0   | [TiDB#32744](https://github.com/pingcap/tidb/issues/32744)               | Fixed     |
| 10 | TiDB      | 5.4.0   | [TiDB#32802](https://github.com/pingcap/tidb/issues/32802)               | Fixed     |
| 11 | TiDB      | 5.2.1   | [TiDB#28103](https://github.com/pingcap/tidb/issues/28103)               | Confirmed |
| 12 | TiDB      | 5.2.1   | [TiDB#28104](https://github.com/pingcap/tidb/issues/28104)               | Confirmed |
| 13 | TiDB      | 5.3.0   | [TiDB#31391](https://github.com/pingcap/tidb/issues/31391)               | Confirmed |
| 14 | TiDB      | 5.3.0   | [TiDB#31405](https://github.com/pingcap/tidb/issues/31405)               | Confirmed |
| 15 | TiDB      | 5.3.0   | [TiDB#31464](https://github.com/pingcap/tidb/issues/31464)               | Confirmed |
| 16 | TiDB      | 5.3.0   | [TiDB#31478](https://github.com/pingcap/tidb/issues/31478)               | Confirmed |
| 17 | TiDB      | 5.3.0   | [TiDB#31669](https://github.com/pingcap/tidb/issues/31669)               | Confirmed |
| 18 | TiDB      | 5.3.0   | [TiDB#31711](https://github.com/pingcap/tidb/issues/31711)               | Confirmed |
| 19 | TiDB      | 5.3.0   | [TiDB#31719](https://github.com/pingcap/tidb/issues/31719)               | Confirmed |
| 20 | TiDB      | 5.3.0   | [TiDB#32174](https://github.com/pingcap/tidb/issues/32174)               | Confirmed |
| 21 | TiDB      | 5.4.0   | [TiDB#32337](https://github.com/pingcap/tidb/issues/32337)               | Confirmed |
| 22 | TiDB      | 5.4.0   | [TiDB#32544](https://github.com/pingcap/tidb/issues/32544)               | Confirmed |
| 23 | TiDB      | 5.4.0   | [TiDB#32583](https://github.com/pingcap/tidb/issues/32583)               | Confirmed |
| 24 | TiDB      | 5.4.0   | [TiDB#32617](https://github.com/pingcap/tidb/issues/32617)               | Confirmed |
| 25 | TiDB      | 5.4.0   | [TiDB#32619](https://github.com/pingcap/tidb/issues/32619)               | Confirmed |
| 26 | TiDB      | 5.4.0   | [TiDB#32633](https://github.com/pingcap/tidb/issues/32633)               | Confirmed |
| 27 | TiDB      | 5.4.0   | [TiDB#32640](https://github.com/pingcap/tidb/issues/32640)               | Confirmed |
| 28 | TiDB      | 5.4.0   | [TiDB#32641](https://github.com/pingcap/tidb/issues/32641)               | Confirmed |
| 29 | TiDB      | 5.4.0   | [TiDB#32654](https://github.com/pingcap/tidb/issues/32654)               | Confirmed |
| 30 | TiDB      | 5.4.0   | [TiDB#32671](https://github.com/pingcap/tidb/issues/32671)               | Confirmed |
| 31 | TiDB      | 5.4.0   | [TiDB#32720](https://github.com/pingcap/tidb/issues/32720)               | Confirmed |
| 32 | TiDB      | 5.4.0   | [TiDB#32751](https://github.com/pingcap/tidb/issues/32751)               | Confirmed |
| 33 | TiDB      | 5.4.0   | [TiDB#32857](https://github.com/pingcap/tidb/issues/32857)               | Confirmed |
| 34 | TIDB      | 5.4.0   | [TiDB#33226](https://github.com/pingcap/tidb/issues/33226)               | Confirmed |
| 35 | TiDB      | 5.4.0   | [TiDB#33232](https://github.com/pingcap/tidb/issues/33232)               | Confirmed |
| 36 | TiDB      | 5.4.0   | [TiDB#33271](https://github.com/pingcap/tidb/issues/33271)               | Confirmed |
| 37 | TiDB      | 5.4.0   | [TiDB#33292](https://github.com/pingcap/tidb/issues/33292)               | Confirmed |
| 38 | MariaDB   | 10.8.2  | [MDEV#27885](https://jira.mariadb.org/browse/MDEV-27885)                 | Confirmed |
| 39 | MariaDB   | 10.8.2  | [MDEV#28140](https://jira.mariadb.org/browse/MDEV-28140)                 | Confirmed |
| 40 | MariaDB   | 10.8.2  | [MDEV#28069](https://jira.mariadb.org/browse/MDEV-28069)                 | Open      |
| 41 | MariaDB   | 10.8.2  | [MDEV#28142](https://jira.mariadb.org/browse/MDEV-28142)                 | Open      |
| 42 | MySQL     | 8.0.28  | [MySQL#106517](https://bugs.mysql.com/bug.php?id=106517)                 | Fixed     |
| 43 | MySQL     | 8.0.28  | [MySQL#106386](https://bugs.mysql.com/bug.php?id=106386)                 | Not A Bug |
| 44 | MySQL     | 8.0.28  | [MySQL#106407](https://bugs.mysql.com/bug.php?id=106407)                 | Not A Bug |
| 45 | MySQL     | 8.0.28  | [MySQL#106420](https://bugs.mysql.com/bug.php?id=106420)                 | Not A Bug |
| 46 | MySQL     | 8.0.28  | [MySQL#106483](https://bugs.mysql.com/bug.php?id=106483)                 | Not A Bug |
| 47 | MySQL     | 8.0.28  | [MySQL#106544](https://bugs.mysql.com/bug.php?id=106554)                 | Not A Bug |
| 48 | MySQL     | 8.0.28  | [MySQL#106571](https://bugs.mysql.com/bug.php?id=106571)                 | Not A Bug |
| 49 | SQLite    | 3.39.2  | [SQLite#12638](https://sqlite.org/forum/forumpost/12638a0aea0602a8)      | Confirmed |
| 50 | Cockroach | 21.2.6  | [Cockroach#77086](https://github.com/cockroachdb/cockroach/issues/77086) | Duplicate |