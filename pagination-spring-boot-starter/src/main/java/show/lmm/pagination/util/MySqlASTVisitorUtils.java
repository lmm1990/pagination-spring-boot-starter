package show.lmm.pagination.util;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import show.lmm.pagination.entity.PaginationInfo;

import java.util.List;

/**
 * sql访问器
 */
public class MySqlASTVisitorUtils {

    private static final DbType dbType = DbType.mysql;

    /**
     * 获得分页sql
     *
     * @param sql 原始sql
     * @return sql
     */
    public static String getPaginationSql(String sql) {
        return rewriteSql(sql, false);
    }

    /**
     * 获得数据总条数sql
     *
     * @param sql 原始sql
     * @return sql
     */
    public static String getTotalCountSql(String sql) {
        return rewriteSql(sql, true);
    }

    /**
     * 重写sql
     *
     * @param sql 原始sql
     * @return
     */
    private static String rewriteSql(String sql, boolean isTotalCountSql) {
        final List<SQLStatement> statementList = SQLUtils.parseStatements(sql, dbType);
        for (SQLStatement item : statementList) {
            if (!(item instanceof SQLSelectStatement)) {
                throw new IllegalArgumentException("sql not support : " + sql);
            }
            SQLSelectStatement selectStmt = (SQLSelectStatement) item;
            if (isTotalCountSql) {
                rewriteTotalCountSql(selectStmt);
            } else {
                setLimit(selectStmt.getSelect());
            }
        }
        return SQLUtils.toSQLString(statementList, dbType);
    }

    /**
     * 查询数据总条数sql
     *
     * @param statement 原始sql
     * @return
     */
    public static void rewriteTotalCountSql(SQLSelectStatement statement) {
        final SQLSelectQuery query = statement.getSelect().getQuery();
        if (query instanceof SQLSelectQueryBlock) {
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) query;
            rewriteTotalCountSql(queryBlock, false);
            return;
        }
        if (query instanceof SQLUnionQuery) {
            SQLUnionQuery union = (SQLUnionQuery) query;

            SQLSelectQueryBlock countSelectQuery = new MySqlSelectQueryBlock();
            countSelectQuery.getSelectList().add(new SQLSelectItem(SQLUtils.toSQLExpr("count(1)")));

            SQLSubqueryTableSource fromSubquery = new SQLSubqueryTableSource(statement.getSelect());
            fromSubquery.setAlias("alias_count");
            countSelectQuery.setFrom(fromSubquery);

            statement.setSelect(new SQLSelect(countSelectQuery));

            rewriteTotalCountSql((SQLSelectQueryBlock) union.getLeft(), true);
            rewriteTotalCountSql((SQLSelectQueryBlock) union.getRight(), true);
            return;
        }
        throw new IllegalStateException();
    }

    /**
     * 查询数据总条数sql
     *
     * @param queryBlock 原始sql
     * @return
     */
    public static void rewriteTotalCountSql(SQLSelectQueryBlock queryBlock, boolean isSubquery) {
        SQLTableSource from = queryBlock.getFrom();
        if (from instanceof SQLSubqueryTableSource) {
            SQLSubqueryTableSource subqueryTabSrc = (SQLSubqueryTableSource) from;
            SQLSelect select = subqueryTabSrc.getSelect();
            if (select.getQuery() instanceof SQLSelectQueryBlock) {
                SQLSelectQueryBlock subquery = (SQLSelectQueryBlock) select.getQuery();
                rewriteTotalCountSql(subquery, true);
            }
        }

        //删除 order by
        if (queryBlock.getOrderBy() != null) {
            queryBlock.setOrderBy(null);
        }
        //删除 group by
        if (queryBlock.getGroupBy() != null) {
            queryBlock.setGroupBy(null);
        }
        //删除 limit
        if (queryBlock.getLimit() != null) {
            queryBlock.setLimit(null);
        }

        //设置查询字段
        queryBlock.getSelectList().clear();
        final String queryField = isSubquery ? "1 c" : "count(1)";
        queryBlock.addSelectItem(SQLUtils.toSelectItem(queryField, DbType.mysql));
    }

    /**
     * 设置limit
     *
     * @param x sql
     */
    private static void setLimit(SQLSelect x) {
        final PaginationInfo pageInfo = PaginationHelper.getPaginationInfo();
        final int offset = (pageInfo.getP() - 1) * pageInfo.getPageSize();
        final SQLExpr rowCountExpr = SQLUtils.toSQLExpr(String.valueOf(pageInfo.getPageSize()));
        final SQLExpr offsetExpr = SQLUtils.toSQLExpr(String.valueOf(offset));
        x.setLimit(new SQLLimit(offsetExpr, rowCountExpr));
    }
}
