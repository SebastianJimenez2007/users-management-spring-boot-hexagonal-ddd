package com.jcaa.usersmanagement.infrastructure.adapter.persistence.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

/**
 * Adaptador de persistencia para PostgreSQL.
 *
 * <p>Se activa con {@code db.dialect=postgresql} (variable de entorno {@code DB_DIALECT}).
 * Reutiliza el SQL de {@link AbstractUserRepository}; lo propio de PostgreSQL son el DDL de
 * {@code schema-postgres.sql} y la URL JDBC construida por {@code DatabaseConfig}.
 */
@Repository
@ConditionalOnProperty(name = "db.dialect", havingValue = "postgresql")
public class UserRepositoryPostgres extends AbstractUserRepository {

  public UserRepositoryPostgres(final DataSource dataSource) {
    super(dataSource);
  }
}
