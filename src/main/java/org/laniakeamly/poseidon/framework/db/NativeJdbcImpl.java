package org.laniakeamly.poseidon.framework.db;

import org.laniakeamly.poseidon.customize.ConnectionPool;
import org.laniakeamly.poseidon.framework.annotation.Valid;
import org.laniakeamly.poseidon.framework.beans.BeansManager;
import org.laniakeamly.poseidon.framework.cache.PoseidonCache;
import org.laniakeamly.poseidon.framework.config.Config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author 404NotFoundx
 * @version 1.0.0
 * @date 2019/11/30 2:28
 * @since 1.8
 */
@SuppressWarnings("SpellCheckingInspection")
public class NativeJdbcImpl implements NativeJdbc {

    protected final boolean isCache = Config.getInstance().getCache();
    protected final boolean auto = Config.getInstance().getTransaction();

    @Valid
    private ConnectionPool pool;

    @Valid
    private PoseidonCache cache;


    @Override
    public boolean execute(String sql, Object... args) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = pool.getConnection();
            /*if (connection == null) {
                synchronized (this) {
                    wait();
                }
                return execute(sql, args);
            }*/
            statement = connection.prepareStatement(sql);
            Boolean bool = setValues(statement, args).execute();
            if (auto) connection.commit(); // 提交
            return bool;
        } catch (Exception e) {
            rollback(connection, auto);
            e.printStackTrace();
        } finally {
            close(null, statement, null);
            release(connection, pool);
        }
        return false;
    }

    @Override
    public NativeResult executeQuery(String sql, Object... args) {
        NativeResult result = null;
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = pool.getConnection();
            /*if (connection == null) {
                synchronized (this) {
                    wait();
                }
                return executeQuery(sql, args);
            }*/
            statement = connection.prepareStatement(sql);
            // 判断是否开启缓存
            if (isCache) {
                result = cache.get(sql, args);
                if (result == null) {
                    ResultSet resultSet = setValues(statement, args).executeQuery();
                    result = BeansManager.newNativeResult().build(resultSet);
                    cache.save(sql, result, args);
                    return cache.get(sql, args);
                }
                return result;
            } else {
                ResultSet resultSet = setValues(statement, args).executeQuery();
                return BeansManager.newNativeResult().build(resultSet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(null, statement, null);
            release(connection, pool);
        }
        return null;
    }

    @Override
    public int executeUpdate(String sql, Object... args) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = pool.getConnection();
            /*if (connection == null) {
                synchronized (this) {
                    wait();
                }
                executeUpdate(sql, args);
            }*/
            statement = connection.prepareStatement(sql);
            int result = setValues(statement, args).executeUpdate();
            if (auto) connection.commit(); // 提交
            if (isCache) cache.refresh(sql); // 刷新缓存
            return result;
        } catch (Exception e) {
            rollback(connection, auto); // 回滚
            e.printStackTrace();
        } finally {
            close(null, statement, null);
            release(connection, pool);
        }
        return 0;
    }

    @Override
    public int executeBatch(String sql, Object... args) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = pool.getConnection();
            /*if (connection == null) {
                synchronized (this) {
                    wait();
                }
                executeUpdate(sql, args);
            }*/
            statement.executeBatch();
        } catch (Exception e) {
            rollback(connection, auto); // 回滚
            e.printStackTrace();
        } finally {
            close(null, statement, null);
            release(connection, pool);
        }
        return 0;
    }
}
