package show.lmm.pagination;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import show.lmm.pagination.core.PageObjectFactory;
import show.lmm.pagination.core.PaginationDataHandler;
import show.lmm.pagination.entity.Page;

import java.lang.reflect.ParameterizedType;

/**
 * 后置处理器，解析mapper方法自定义注解
 *
 * @author liumingming
 * @since 2021-09-03 12:02
 */
@Component
public class PaginationPluginBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DefaultSqlSessionFactory sessionFactory) {
            sessionFactory.getConfiguration().setObjectFactory(new PageObjectFactory());
            return bean;
        }
        if (bean instanceof MapperFactoryBean) {
            MapperFactoryBean mapperFactoryBean = (MapperFactoryBean) bean;
            final String mapperName = mapperFactoryBean.getObjectType().getName();
            ReflectionUtils.doWithMethods(mapperFactoryBean.getObjectType(), method -> {
                Class<?> returnType = (Class<?>) ((((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0]));
                PaginationDataHandler.PAGINATION_METHOD_RETURN_TYPES.put(String.format("%s.%s", mapperName, method.getName()), returnType);
            }, method -> method.getReturnType().isAssignableFrom(Page.class));
            return bean;
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
