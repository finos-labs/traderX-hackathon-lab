package finos.traderx.tradeservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.jdbc.DataSourceBuilder;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        String jdbcUrl = "jdbc:h2:tcp://database:18082/traderx;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE";
        
        System.out.println("🔧 FORCING DATABASE CONNECTION TO: " + jdbcUrl);
        System.out.println("🔧 NO IN-MEMORY FALLBACK ALLOWED!");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUsername("sa");
        dataSource.setPassword("sa");
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        dataSource.setConnectionTimeout(30000);
        dataSource.setValidationTimeout(10000);
        
        return dataSource;
    }
}