package show.lmm.pagination;

import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.result.DefaultResultHandler;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetWrapper;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import show.lmm.pagination.core.BoundSqlSqlSource;
import show.lmm.pagination.core.PaginationDataHandler;
import show.lmm.pagination.util.MySqlASTVisitorUtils;
import show.lmm.pagination.util.PaginationHelper;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * mybatis分页插件
 *
 * @author liumingming
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
        @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
public class PaginationPlugin implements Interceptor {

    /**
     * 数据总数量sql方法id后缀
     */
    private static final String COUNT_SQL_METHOD_ID_SUFFIX = "$$Count";

    /**
     * 拦截器
     *
     * @param invocation: 调用对象
     * @return java.lang.Object
     **/
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //重写sql
        if (invocation.getTarget() instanceof StatementHandler) {
            return rewriteStatementHandler(invocation);
        }
        //处理结果
        if (invocation.getTarget() instanceof ResultSetHandler) {
            return handleResult(invocation);
        }
        return invocation.proceed();
    }

    /**
     * 处理结果
     *
     * @param invocation 调用对象
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private Object handleResult(Invocation invocation) throws InvocationTargetException, IllegalAccessException, SQLException {
        DefaultResultSetHandler resultSetHandler = (DefaultResultSetHandler) invocation.getTarget();
        MetaObject metaObject = MetaObject.forObject(resultSetHandler, new DefaultObjectFactory(), new DefaultObjectWrapperFactory(), new DefaultReflectorFactory());
        MappedStatement mappedStatement = ((MappedStatement) metaObject.getValue("mappedStatement"));
        boolean isPaginationMethod = PaginationDataHandler.PAGINATION_METHOD_RETURN_TYPES.containsKey(mappedStatement.getId());
        boolean isPaginationCountMethod = mappedStatement.getId().endsWith(COUNT_SQL_METHOD_ID_SUFFIX);
        if (!isPaginationMethod && !isPaginationCountMethod) {
            return invocation.proceed();
        }
        // 获取结果映射
        List<ResultMap> resultMaps = mappedStatement.getResultMaps();
        if (resultMaps.isEmpty()) {
            return invocation.proceed();
        }

        Configuration configuration = (Configuration) metaObject.getValue("configuration");
        ResultMap baseResultMap = resultMaps.get(0);
        Statement statement = (Statement) invocation.getArgs()[0];
        ResultSet resultSet = statement.getResultSet();
        if (resultSet == null) {
            return invocation.proceed();
        }

        DefaultResultHandler resultHandler = new DefaultResultHandler();
        metaObject.setValue("resultHandler", resultHandler);
        if (isPaginationCountMethod) {
            ResultSetWrapper rsw = new ResultSetWrapper(resultSet, configuration);
            ResultMap resultMap = new ResultMap.Builder(configuration, baseResultMap.getId(), Long.TYPE, baseResultMap.getResultMappings())
                    .discriminator(baseResultMap.getDiscriminator())
                    .build();

            resultSetHandler.handleRowValues(rsw, resultMap, resultHandler, RowBounds.DEFAULT, null);
            return resultHandler.getResultList();
        }

        //查询数据总条数
        executeCount(((CachingExecutor) metaObject.getValue("executor")), mappedStatement, metaObject);

        ResultSetWrapper rsw = new ResultSetWrapper(resultSet, configuration);
        ResultMap resultMap = new ResultMap.Builder(configuration, baseResultMap.getId(), PaginationDataHandler.PAGINATION_METHOD_RETURN_TYPES.get(mappedStatement.getId()), baseResultMap.getResultMappings())
                .discriminator(baseResultMap.getDiscriminator())
                .build();

        resultSetHandler.handleRowValues(rsw, resultMap, resultHandler, RowBounds.DEFAULT, null);
        return resultHandler.getResultList();
    }

    /**
     * 重写StatementHandler
     *
     * @param invocation: 调用对象
     * @return java.lang.Object
     **/
    private Object rewriteStatementHandler(Invocation invocation) throws Throwable {
        if (!(invocation.getTarget() instanceof StatementHandler)) {
            return invocation.proceed();
        }
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        String methodId = ((MappedStatement) SystemMetaObject.forObject(statementHandler.getParameterHandler()).getValue("mappedStatement")).getId();
        if (!PaginationDataHandler.PAGINATION_METHOD_RETURN_TYPES.containsKey(methodId)) {
            return invocation.proceed();
        }
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        rewriteSql(metaObject);
        return invocation.proceed();
    }

    /**
     * 重写sql
     *
     * @param metaObject: 反射工具类
     **/
    private void rewriteSql(MetaObject metaObject) {
        final BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        final String rewriteSql = MySqlASTVisitorUtils.getPaginationSql(boundSql.getSql());

        metaObject.setValue("delegate.boundSql.sql", rewriteSql);
    }

    /**
     * 获得查询数据总条数sql
     *
     * @param cachingExecutor:     执行器
     * @param baseMappedStatement: MappedStatement
     * @param metaObject:          反射工具类
     **/
    private void executeCount(CachingExecutor cachingExecutor, MappedStatement baseMappedStatement, MetaObject metaObject) throws SQLException {
        String methodId = String.format("%s%s", baseMappedStatement.getId(), COUNT_SQL_METHOD_ID_SUFFIX);
        BoundSql baseBoundSql = (BoundSql) metaObject.getValue("boundSql");
        RowBounds rowBounds = (RowBounds) metaObject.getValue("rowBounds");
        Object value;
        //自定义查询数据总数量
        if (PaginationHelper.getPaginationInfo().getCountSql() != null) {
            methodId = String.format("%s%s", PaginationHelper.getPaginationInfo().getCountSql(), methodId);
            MappedStatement mappedStatement = newMappedStatement(methodId, baseMappedStatement, PaginationHelper.getPaginationInfo().getCountSql(), baseBoundSql, true);
            ResultHandler resultHandler = (ResultHandler) metaObject.getValue("resultHandler");

            value = cachingExecutor.query(mappedStatement, new MapperMethod.ParamMap(), rowBounds, resultHandler);
        } else {
            String countSql = MySqlASTVisitorUtils.getTotalCountSql(baseBoundSql.getSql());
            MappedStatement mappedStatement = newMappedStatement(methodId, baseMappedStatement, countSql, baseBoundSql, false);
            Object parameter = ((DefaultParameterHandler) metaObject.getValue("parameterHandler")).getParameterObject();
            ResultHandler resultHandler = (ResultHandler) metaObject.getValue("resultHandler");

            BoundSql boundSql = mappedStatement.getBoundSql(parameter);

            //创建 count 查询的缓存 key
            CacheKey countKey = cachingExecutor.createCacheKey(mappedStatement, parameter, RowBounds.DEFAULT, boundSql);

            value = cachingExecutor.query(mappedStatement, parameter, rowBounds, resultHandler, countKey, boundSql);
        }
        long totalCount = (Long) ((ArrayList) value).get(0);
        PaginationHelper.getPaginationInfo().setTotalCount(totalCount);
    }

    /**
     * 构造新的MappedStatement
     *
     * @param methodId:    方法id
     * @param ms:          旧的MappedStatement
     * @param countSql:    查询数据总条数sql
     * @param boundSql:    sql包装类
     * @param isCustomSql: 是否是自定义sql
     * @return org.apache.ibatis.mapping.MappedStatement
     **/
    private MappedStatement newMappedStatement(String methodId, MappedStatement ms, String countSql, BoundSql boundSql, boolean isCustomSql) {
        SqlSource newSqlSource = new BoundSqlSqlSource(ms, countSql, boundSql, isCustomSql);

        MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), methodId, newSqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length > 0) {
            builder.keyProperty(ms.getKeyProperties()[0]);
        }
        builder.timeout(ms.getTimeout());
        if (!isCustomSql) {
            builder.parameterMap(ms.getParameterMap());
            builder.cache(ms.getCache());
            builder.useCache(ms.isUseCache());
        }

        builder.resultMaps(new ArrayList<ResultMap>() {{
            add(new ResultMap.Builder(ms.getConfiguration(), String.format("%s$$return", methodId), Integer.class, new ArrayList<>()).build());
        }});
        builder.resultSetType(ms.getResultSetType());

        builder.flushCacheRequired(ms.isFlushCacheRequired());

        return builder.build();
    }
}