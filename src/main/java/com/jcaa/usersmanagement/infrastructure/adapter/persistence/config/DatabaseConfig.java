package com.jcaa.usersmanagement.infrastructure.adapter.persistence.config;

/**
 * Datos de conexion y dialecto del motor de persistencia.
 *
 * <p>El dialecto determina el esquema de la URL JDBC. MySQL es el valor por defecto para no
 * alterar el comportamiento previo; PostgreSQL se selecciona con {@code db.dialect=postgresql}.
 */
public record DatabaseConfig(
    String dialect,
    String host,
    int port,
    String databaseName,
    String username,
    String password) {

  public static final String DIALECT_MYSQL = "mysql";
  public static final String DIALECT_POSTGRESQL = "postgresql";

  private static final String MYSQL_URL_TEMPLATE =
      "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

  /**
   * {@code stringtype=unspecified} es obligatorio: el repositorio enlaza los valores con
   * {@code setString}, que el driver envía como {@code varchar}. Sin este parámetro PostgreSQL
   * rechaza el INSERT contra las columnas ENUM con
   * {@code column "role" is of type user_role but expression is of type character varying}.
   * El parámetro hace que el driver envíe el valor sin tipo declarado y sea el servidor quien lo
   * resuelva contra el tipo de la columna.
   */
  private static final String POSTGRESQL_URL_TEMPLATE =
      "jdbc:postgresql://%s:%d/%s?stringtype=unspecified";

  public String buildJdbcUrl() {
    final String template = isPostgreSql() ? POSTGRESQL_URL_TEMPLATE : MYSQL_URL_TEMPLATE;
    return String.format(template, host, port, databaseName);
  }

  public boolean isPostgreSql() {
    return DIALECT_POSTGRESQL.equalsIgnoreCase(dialect);
  }
}
