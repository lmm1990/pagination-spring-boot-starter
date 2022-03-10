# pagination-spring-boot-starter
基于spring boot+mybatis的分页插件

## 💿 快速开始

### 在代码中使用

```
//初始化分页参数
PaginationHelper.init(1,5);
//多表联查时，建议手写查询总数量sql性能更佳
PaginationHelper.init(2,10,"select 999;");
//分页结果
Page<TestInfo> result = testMapper.listByStatus(1);
```