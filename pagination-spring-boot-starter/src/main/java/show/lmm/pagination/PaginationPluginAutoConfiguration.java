package show.lmm.pagination;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * mybatis分页插件自动注册
 *
 * @author liumingming
 */
@Configuration
public class PaginationPluginAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PaginationPlugin.class)
    public PaginationPlugin paginationPlugin() {
        return new PaginationPlugin();
    }

    @Bean
    @ConditionalOnMissingBean(PaginationPluginBeanPostProcessor.class)
    public PaginationPluginBeanPostProcessor paginationPluginBeanPostProcessor() {
        return new PaginationPluginBeanPostProcessor();
    }
}