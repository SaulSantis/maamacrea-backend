package com.maamacrea.backend;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    Flyway flyway(
            DataSource dataSource,
            @Value("${spring.flyway.locations:classpath:db/migration}") String[] locations,
            @Value("${spring.flyway.baseline-on-migrate:false}") boolean baselineOnMigrate,
            @Value("${spring.flyway.baseline-version:1}") String baselineVersion) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .load();
    }

    @Bean
    static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
        return beanFactory -> addDependsOn(beanFactory, "entityManagerFactory", "flyway");
    }

    private static void addDependsOn(
            ConfigurableListableBeanFactory beanFactory, String beanName, String dependencyBeanName) {
        if (!beanFactory.containsBeanDefinition(beanName)) {
            return;
        }

        var beanDefinition = beanFactory.getBeanDefinition(beanName);
        beanDefinition.setDependsOn(
                StringUtils.addStringToArray(beanDefinition.getDependsOn(), dependencyBeanName));
    }
}
