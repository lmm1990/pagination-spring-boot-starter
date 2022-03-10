package show.lmm.pagination.util;

import show.lmm.pagination.entity.PaginationInfo;

/**
 * 分页helper
 *
 * @author liumingming
 * @since 2021-09-08 17:07
 */
public class PaginationHelper {

    /**
     * 分页信息
     */
    private static final ThreadLocal<PaginationInfo> PAGINATION_INFO = new ThreadLocal<>();

    /**
     * 初始化分页信息
     *
     * @param p:        当前页码
     * @param pageSize: 每页显示条数
     **/
    public static void init(int p, int pageSize) {
        PAGINATION_INFO.set(new PaginationInfo(p, pageSize));
    }

    /**
     * 初始化分页信息
     *
     * @param p:        当前页码
     * @param pageSize: 每页显示条数
     **/
    public static void init(int p, int pageSize, String countSql) {
        PAGINATION_INFO.set(new PaginationInfo(p, pageSize, countSql));
    }

    /**
     * 获得分页信息
     *
     * @return com.github.lmm1990.spring.boot.starter.entity.Page
     **/
    public static PaginationInfo getPaginationInfo() {
        return PAGINATION_INFO.get();
    }

    /**
     * 清空分页信息
     **/
    public static void clear() {
        PAGINATION_INFO.remove();
    }
}
