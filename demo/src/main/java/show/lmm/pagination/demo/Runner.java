package show.lmm.pagination.demo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import show.lmm.pagination.demo.entity.TestInfo;
import show.lmm.pagination.demo.mapper.TestMapper;
import show.lmm.pagination.entity.Page;
import show.lmm.pagination.util.PaginationHelper;


/**
 * 启动类
 *
 * @author liumingming
 * @since 2021-08-19 9:32
 */
@Component
public class Runner implements CommandLineRunner {

    private TestMapper testMapper;

    public Runner(TestMapper testMapper){
        this.testMapper = testMapper;
    }

    @Override
    public void run(String... args) {
        System.out.println(JSONObject.toJSONString(testMapper.get(20)));

        PaginationHelper.init(1,5);
        Page<TestInfo> list1 = testMapper.listByStatus(1);
        System.out.println(JSONArray.toJSONString(list1));

        PaginationHelper.init(2,10,"select 999;");
        Page<TestInfo> list2 = testMapper.listByStatus(2);
        System.out.println(JSONArray.toJSONString(list2));
    }
}
