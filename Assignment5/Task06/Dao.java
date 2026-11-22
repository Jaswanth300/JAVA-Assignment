package task06;

import java.sql.SQLException;
import java.util.List;

/**
 * Generic Data Access Object interface decoupling domain models from SQL persistence.
 */
public interface Dao<T, K> {
    void insert(T entity) throws SQLException;
    T findById(K id) throws SQLException;
    List<T> findAll() throws SQLException;
    void update(T entity) throws SQLException;
    void delete(K id) throws SQLException;
}
