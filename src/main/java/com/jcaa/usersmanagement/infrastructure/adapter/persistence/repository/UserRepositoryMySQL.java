package com.jcaa.usersmanagement.infrastructure.adapter.persistence.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

/**
 * Adaptador de persistencia para MySQL.
 *
 * <p>Es el adaptador por defecto: se activa salvo que {@code db.dialect} indique lo contrario.
 * El SQL vive en {@link AbstractUserRepository} porque es compatible con PostgreSQL.
 */
@Repository
@ConditionalOnProperty(
    name = "db.dialect",
    havingValue = "mysql",
    matchIfMissing = true)
public class UserRepositoryMySQL extends AbstractUserRepository {

  public UserRepositoryMySQL(final DataSource dataSource) {
    super(dataSource);
  }
}
