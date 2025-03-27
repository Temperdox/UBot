package com.cottonlesergal.ubot.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.stereotype.Component;

/**
 * Custom implementation of Hibernate's PhysicalNamingStrategy
 * to handle specific column type mappings for database compatibility.
 */
@Component
public class PhysicalNamingStrategyImpl extends PhysicalNamingStrategyStandardImpl {

    private static final long serialVersionUID = 1L;

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        // Let Hibernate use exact column names as defined in the database
        return super.toPhysicalColumnName(name, jdbcEnvironment);
    }

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        // Let Hibernate use exact table names as defined in the database
        return super.toPhysicalTableName(name, jdbcEnvironment);
    }
}