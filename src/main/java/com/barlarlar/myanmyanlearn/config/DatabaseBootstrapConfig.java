package com.barlarlar.myanmyanlearn.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseBootstrapConfig {
    @Bean
    public HibernatePropertiesCustomizer bootstrapHibernateDdlAuto(DataSource dataSource) {
        return props -> {
            try {
                if (!isDatabaseEmpty(dataSource)) {
                    return;
                }
                Object current = props.get("hibernate.hbm2ddl.auto");
                String v = current != null ? String.valueOf(current).trim().toLowerCase() : "";
                if (v.isBlank() || "validate".equals(v) || "none".equals(v)) {
                    props.put("hibernate.hbm2ddl.auto", "update");
                }
            } catch (Exception e) {
            }
        };
    }

    private static boolean isDatabaseEmpty(DataSource dataSource) throws Exception {
        try (Connection c = dataSource.getConnection()) {
            String sql = "select count(*) from information_schema.tables where table_schema = database()";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    long count = rs.getLong(1);
                    return count == 0L;
                }
            }
        }
    }
}

