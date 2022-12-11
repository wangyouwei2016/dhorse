package org.dhorse.infrastructure.repository.source;

import javax.sql.DataSource;

import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.SqlSessionFactory;
import org.dhorse.infrastructure.component.ComponentConstants;
import org.dhorse.infrastructure.component.MysqlConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableTransactionManagement
@MapperScan("org.dhorse.infrastructure.repository.mapper")
public class DataSourceConfig{

	private static final int DEFAULT_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
	
	private static final String TEST_QUERY = "SELECT 1";
	
	private static final long CONNECTION_TIMEOUT = 10 * 1000L;

	private static final long IDLE_TIMEOUT = 10 * 60 * 1000L;

	private static final long MAX_LIFE_TIME = 30 * 60 * 1000L;

	@Autowired
	private ComponentConstants componentConstants;
	
	@Bean(destroyMethod = "close")
	public DataSource dataSource() {
		HikariDataSource dataSource = dataSourceBuilder();
		dataSource.setConnectionTimeout(CONNECTION_TIMEOUT);
		dataSource.setIdleTimeout(IDLE_TIMEOUT);
		dataSource.setMaxLifetime(MAX_LIFE_TIME);
		dataSource.setMinimumIdle(DEFAULT_POOL_SIZE);
		dataSource.setMaximumPoolSize(DEFAULT_POOL_SIZE);
		dataSource.setConnectionTestQuery(TEST_QUERY);
		return dataSource;
	}

	private HikariDataSource dataSourceBuilder() {
		HikariDataSource dataSource = null;
		if(componentConstants.getMysql().isEnable()) {
			dataSource = new HikariDataSource();
			dataSource.setDriverClassName(MysqlConfig.DRIVER_CLASS);
			dataSource.setJdbcUrl(componentConstants.getMysql().getUrl());
			dataSource.setUsername(componentConstants.getMysql().getUser());
			dataSource.setPassword(componentConstants.getMysql().getPassword());
		}else {
			dataSource = new DefaultHikariDataSource();
			dataSource.setDriverClassName("org.h2.Driver");
			dataSource.setJdbcUrl("jdbc:h2:tcp://localhost:59539/"
					+ componentConstants.getDataPath() + "db/dhorse");
			dataSource.setUsername("dhorse");
			dataSource.setPassword("dhorse");
		}
		return dataSource;
	}
	
	private PaginationInnerInterceptor paginationInnerInterceptor() {
		if(componentConstants.getMysql().isEnable()) {
			return new PaginationInnerInterceptor(DbType.MYSQL);
		}else {
			return new PaginationInnerInterceptor(DbType.H2);
		}
	}
	
	@Bean
	public SqlSessionFactory sqlSessionFactory() throws Exception {
		MybatisSqlSessionFactoryBean sqlSessionFactory = new MybatisSqlSessionFactoryBean();
		sqlSessionFactory.setDataSource(dataSource());
		sqlSessionFactory.setConfiguration(configuration());
		sqlSessionFactory.setPlugins(mybatisPlusInterceptor());
		sqlSessionFactory.setGlobalConfig(globalConfig());
		return sqlSessionFactory.getObject();
	}

	@Bean
	public MybatisConfiguration configuration() {
		MybatisConfiguration configuration = new MybatisConfiguration();
		configuration.setCacheEnabled(true);
		configuration.setLazyLoadingEnabled(true);
		configuration.setAggressiveLazyLoading(false);
		configuration.setMultipleResultSetsEnabled(true);
		configuration.setUseColumnLabel(true);
		configuration.setUseGeneratedKeys(false);
		configuration.setMapUnderscoreToCamelCase(true);
		configuration.setAutoMappingBehavior(AutoMappingBehavior.FULL);
		configuration.setDefaultStatementTimeout(10);
		configuration.setLogImpl(Slf4jImpl.class);
		return configuration;
	}
	
	@Bean
	public PlatformTransactionManager transactionManager() {
		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
		transactionManager.setDataSource(dataSource());
		return transactionManager;
	}

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(paginationInnerInterceptor());
        return interceptor;
    }
	
    @Bean
    public GlobalConfig globalConfig() {
    	GlobalConfig globalConfig = new GlobalConfig();
    	globalConfig.setBanner(false);
    	return globalConfig;
    }
}