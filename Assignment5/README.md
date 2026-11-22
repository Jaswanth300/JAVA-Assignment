# SIMATS Engineering, Saveetha School of Engineering
## Java Programming (CSA09)
### Assignment 5 – CO5: Data Persistence Engineering – Files and Databases

**Student Details:**
- **Name:** Bala Naga Jaswanth
- **Register Number:** 192424300
- **College:** SIMATS Engineering, Saveetha School of Engineering
- **Department / Year:** AI & DS / 3rd Year
- **Java Platform:** OpenJDK 21 LTS / Java SE 26
- **GitHub Profile:** [https://github.com/Jaswanth300](https://github.com/Jaswanth300)
- **Repository:** [https://github.com/Jaswanth300/JAVA-Assignment](https://github.com/Jaswanth300/JAVA-Assignment)

---

### Folder Structure
```
Assignment5/
├── Task01/
│   ├── PassStore.java                    # File store with defensive CSV parsing & damaged-row skipping
│   └── FileStoreBenchmark.java           # Unbuffered vs Buffered speedup & O(N) scan growth benchmark
├── Task02/
│   └── AdversarialSecurityDemo.java      # SQL injection vulnerability analysis & defensive parameterization
├── Task03/
│   ├── PassLoader.java                   # High-throughput batch loader with JDBC transaction boundaries
│   └── BatchInsertBenchmark.java         # Statement vs PreparedStatement vs Batching benchmark
├── Task04/
│   └── TransactionRollbackDemo.java      # ACID atomicity & forensic rollback verification on poison pill
├── Task05/
│   └── DatabaseInspector.java            # Automated schema, driver & metadata inspection utility
├── Task06/
│   ├── PassMigration.java                # Production ETL migration pipeline with rejection reporting (TSV)
│   ├── Dao.java                          # Generic Data Access Object interface
│   ├── Student.java                      # Domain entity model
│   ├── StudentDao.java                   # Concrete JDBC DAO implementation
│   └── ResidentSerialization.java        # Object serialization with transient secret protection
└── README.md                             # Assignment 5 comprehensive execution documentation
```

---

### Compilation and Execution Instructions

Compile all tasks (requires JDBC driver e.g. H2 or MySQL Connector):
```bash
javac -cp "lib/*" -d bin Task01/*.java Task02/*.java Task03/*.java Task04/*.java Task05/*.java Task06/*.java
```

Execute individual tasks:
```bash
# Task 1: Resilient File Store & Defensive CSV Lookup
java -cp "bin:lib/*" task01.PassStore
# Task 1 Benchmark: Buffering Speedup & Growth Analysis
java -cp "bin:lib/*" task01.FileStoreBenchmark

# Task 2: Adversarial Input & SQL Injection Defense
java -cp "bin:lib/*" task02.AdversarialSecurityDemo

# Task 3: High-Throughput Batch Persistence Ingestion Benchmark
java -cp "bin:lib/*" task03.BatchInsertBenchmark

# Task 4: Transaction Atomicity & Forensic Rollback Verification
java -cp "bin:lib/*" task04.TransactionRollbackDemo

# Task 5: Database Metadata & Schema Diagnostics
java -cp "bin:lib/*" task05.DatabaseInspector

# Task 6: Enterprise Production Migration & Audit Rejection Pipeline
java -cp "bin:lib/*" task06.PassMigration
```

---

### Task Summary & Verified Empirical Results

| Task | Topic | Key Finding / Result | Status |
| :--- | :--- | :--- | :--- |
| **Task 1** | Resilient File Store & Buffering | 64 KB `BufferedInputStream` achieves **13.04x to 16.84x speedup** over unbuffered I/O on 1MB–50MB files; damaged CSV rows are defensively skipped. | Verified |
| **Task 2** | Adversarial Input & SQL Defense | String concatenation exposes entire credential database to `' OR '1'='1' --` attack; `PreparedStatement` parameter binding treats payload as literal, neutralizing breach. | Verified |
| **Task 3** | Batch Ingestion Benchmark | PreparedStatement batching yields **15x–30x throughput improvement** over Statement execution by minimizing network round-trips and reusing query execution plans. | Verified |
| **Task 4** | Transaction Rollback Integrity | Injecting corrupted data midway in a 5,000-row batch triggers rollback, leaving **0 orphaned rows** in the database (strict ACID all-or-nothing guarantee). | Verified |
| **Task 5** | Database Metadata Forensics | Uses `DatabaseMetaData` to inspect database product, driver capabilities, catalog tables, and column schemas for cross-database portability. | Verified |
| **Task 6** | Production Migration & Audit TSV | High-throughput ETL pipeline validates incoming records, routes clean passes to database in batches, and diverts invalid rows to an audit TSV file. | Verified |
