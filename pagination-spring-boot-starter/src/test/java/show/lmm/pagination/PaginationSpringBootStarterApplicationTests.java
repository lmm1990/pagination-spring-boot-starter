package show.lmm.pagination;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import show.lmm.pagination.util.MySqlASTVisitorUtils;
import show.lmm.pagination.util.PaginationHelper;

//@SpringBootTest
class PaginationSpringBootStarterApplicationTests {

    @Test
    void testGetPaginationSql() {
        PaginationHelper.init(1, 10);
        String sql = "select a.userId,a.userName from user_info a left join hobby b on a.userId = b.userId where status = 1 order by userId desc";
        final String rewriteSql = MySqlASTVisitorUtils.getPaginationSql(sql);
        System.out.println(rewriteSql);
        Assertions.assertEquals(rewriteSql, "SELECT a.userId, a.userName\n" +
                "FROM user_info a\n" +
                "\tLEFT JOIN hobby b ON a.userId = b.userId\n" +
                "WHERE status = 1\n" +
                "ORDER BY userId DESC\n" +
                "LIMIT 0, 10");
    }

    @Test
    void testGetPaginationSql2() {
        PaginationHelper.init(1, 10);
        String sql = "select * from (select push_time,'总用户数' as '类型',sum(total_member) total_count from crm_push_member_record where push_time between '2022-01-01' and '2022-12-31' group by push_time order by push_time desc) a union all \n" +
                "select * from (select push_time,'失败用户数'as '类型',sum(failed_member) total_count from crm_push_member_record where push_time between '2022-01-01' and '2022-12-31' group by push_time order by push_time desc) b";
        final String rewriteSql = MySqlASTVisitorUtils.getPaginationSql(sql);
        System.out.println(rewriteSql);
        Assertions.assertEquals(rewriteSql, "SELECT *\n" +
                "FROM (\n" +
                "\tSELECT push_time, '总用户数' AS \"类型\", sum(total_member) AS total_count\n" +
                "\tFROM crm_push_member_record\n" +
                "\tWHERE push_time BETWEEN '2022-01-01' AND '2022-12-31'\n" +
                "\tGROUP BY push_time\n" +
                "\tORDER BY push_time DESC\n" +
                ") a\n" +
                "UNION ALL\n" +
                "SELECT *\n" +
                "FROM (\n" +
                "\tSELECT push_time, '失败用户数' AS \"类型\", sum(failed_member) AS total_count\n" +
                "\tFROM crm_push_member_record\n" +
                "\tWHERE push_time BETWEEN '2022-01-01' AND '2022-12-31'\n" +
                "\tGROUP BY push_time\n" +
                "\tORDER BY push_time DESC\n" +
                ") b\n" +
                "LIMIT 0, 10");
    }

    @Test
    void testGetPaginationSql3() {
        PaginationHelper.init(1, 10);
        String sql = "select push_time,'总用户数' as '类型',sum(total_member) total_count from crm_push_member_record where push_time between '2022-01-01' and '2022-12-31' group by push_time union all \n" +
                "select push_time,'总用户数' as '类型',sum(total_member) total_count from crm_push_member_record where push_time between '2022-01-01' and '2022-12-31' group by push_time";
        final String rewriteSql = MySqlASTVisitorUtils.getPaginationSql(sql);
        System.out.println(rewriteSql);
        Assertions.assertEquals(rewriteSql, "SELECT push_time, '总用户数' AS \"类型\", sum(total_member) AS total_count\n" +
                "FROM crm_push_member_record\n" +
                "WHERE push_time BETWEEN '2022-01-01' AND '2022-12-31'\n" +
                "GROUP BY push_time\n" +
                "UNION ALL\n" +
                "SELECT push_time, '总用户数' AS \"类型\", sum(total_member) AS total_count\n" +
                "FROM crm_push_member_record\n" +
                "WHERE push_time BETWEEN '2022-01-01' AND '2022-12-31'\n" +
                "GROUP BY push_time\n" +
                "LIMIT 0, 10");
    }

    @Test
    void testGetTotalCountSql() {
        String sql = "select a.userId,a.userName from user_info a left join hobby b on a.userId = b.userId where status = 1 order by userId desc";
        final String rewriteSql = MySqlASTVisitorUtils.getTotalCountSql(sql);
        System.out.println(rewriteSql);
        Assertions.assertEquals(rewriteSql, "SELECT count(1)\n" +
                "FROM user_info a\n" +
                "\tLEFT JOIN hobby b ON a.userId = b.userId\n" +
                "WHERE status = 1");
    }

    @Test
    void testGetTotalCount2() {
        PaginationHelper.init(1, 10);
        String sql = "select * from (select push_time,'总用户数' as '类型',sum(total_member) total_count from crm_push_member_record where push_time between '2022-01-01' and '2022-12-31' group by push_time order by push_time desc) a union all \n" +
                "select * from (select push_time,'失败用户数'as '类型',sum(failed_member) total_count from crm_push_member_record where push_time between '2022-01-01' and '2022-12-31' group by push_time order by push_time desc) b";
        final String rewriteSql = MySqlASTVisitorUtils.getTotalCountSql(sql);
        System.out.println(rewriteSql);
        Assertions.assertEquals(rewriteSql, "SELECT count(1)\n" +
                "FROM (\n" +
                "\tSELECT 1 AS c\n" +
                "\tFROM (\n" +
                "\t\tSELECT 1 AS c\n" +
                "\t\tFROM crm_push_member_record\n" +
                "\t\tWHERE push_time BETWEEN '2022-01-01' AND '2022-12-31'\n" +
                "\t) a\n" +
                "\tUNION ALL\n" +
                "\tSELECT 1 AS c\n" +
                "\tFROM (\n" +
                "\t\tSELECT 1 AS c\n" +
                "\t\tFROM crm_push_member_record\n" +
                "\t\tWHERE push_time BETWEEN '2022-01-01' AND '2022-12-31'\n" +
                "\t) b\n" +
                ") alias_count");
    }

    @Test
    void testGetTotalCount3() {
        String sql = "select push_time,'总用户数' as '类型',sum(total_member) total_count from crm_push_member_record where push_time between '2022-01-01' and '2022-12-31' group by push_time union all \n" +
                "select push_time,'总用户数' as '类型',sum(total_member) total_count from crm_push_member_record where push_time between '2022-01-01' and '2022-12-31' group by push_time";
        final String rewriteSql = MySqlASTVisitorUtils.getTotalCountSql(sql);
        System.out.println(rewriteSql);
        Assertions.assertEquals(rewriteSql, "SELECT sum(c)\n" +
                "FROM (\n" +
                "\tSELECT 1 AS c\n" +
                "\tFROM crm_push_member_record\n" +
                "\tWHERE push_time BETWEEN '2022-01-01' AND '2022-12-31'\n" +
                "\tUNION ALL\n" +
                "\tSELECT 1 AS c\n" +
                "\tFROM crm_push_member_record\n" +
                "\tWHERE push_time BETWEEN '2022-01-01' AND '2022-12-31'\n" +
                ") alias_count");
    }
}