package show.lmm.pagination.demo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import show.lmm.pagination.demo.entity.TestInfo;
import show.lmm.pagination.entity.Page;

import java.util.List;

/**
 * TestMapper
 */
@Mapper
public interface TestMapper {

    /**
     * 列表
     *
     * @return java.util.List<com.github.lmm1990.mybatis.plugin.pagination.demo.entity.TestInfo>
     * @since 刘明明/2021-09-09 16:41:56
     **/
    @Select("SELECT id, name, tenantId FROM test")
    List<TestInfo> list();

    /**
     * 列表
     *
     * @return java.util.List<com.github.lmm1990.mybatis.plugin.pagination.demo.entity.TestInfo>
     * @since 刘明明/2021-09-09 16:41:56
     **/
    @Select("SELECT id, name, tenantId FROM test where id = #{id}")
    TestInfo get(int id);

    /**
     * 列表
     *
     * @param status: 状态
     * @return io.github.lmm1990.mybatis.plugin.pagination.spring.boot.starter.entity.Page<com.github.lmm1990.mybatis.plugin.pagination.demo.entity.TestInfo>
     * @since 刘明明/2021-09-09 16:42:07
     **/
    Page<TestInfo> listByStatus(int status);
}