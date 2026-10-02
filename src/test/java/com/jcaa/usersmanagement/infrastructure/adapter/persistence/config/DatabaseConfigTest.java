package com.jcaa.usersmanagement.infrastructure.adapter.persistence.config;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for DatabaseConfig.
 *
 * <p>Cubre la construccion de la URL JDBC para cada dialecto y la deteccion de PostgreSQL.
 */
@DisplayName("DatabaseConfig")
class DatabaseConfigTest {

  private static final String HOST = "db.example.com";
  private static final int PORT = 5432;
  private static final String DATABASE = "crud_usuarios";
  private static final String USERNAME = "user";
  private static final String PASSWORD = "pass";

  @Test
  @DisplayName("buildJdbcUrl() devuelve la URL de MySQL cuando el dialecto es mysql")
  void shouldBuildMySqlUrl() {
    // Arrange
    final DatabaseConfig config =
        new DatabaseConfig(DatabaseConfig.DIALECT_MYSQL, HOST, PORT, DATABASE, USERNAME, PASSWORD);

    // Act
    final String url = config.buildJdbcUrl();

    // Assert
    assertAll(
        "URL de MySQL",
        () -> assertTrue(url.startsWith("jdbc:mysql://"), "debe usar el prefijo de MySQL"),
        () -> assertTrue(url.contains(HOST + ":" + PORT + "/" + DATABASE), "debe incluir host, puerto y base"),
        () -> assertTrue(url.contains("useSSL=false"), "debe conservar los parametros de MySQL"),
        () -> assertTrue(url.contains("allowPublicKeyRetrieval=true"), "debe conservar allowPublicKeyRetrieval"));
  }

  @Test
  @DisplayName("buildJdbcUrl() devuelve la URL de PostgreSQL cuando el dialecto es postgresql")
  void shouldBuildPostgreSqlUrl() {
    // Arrange
    final DatabaseConfig config =
        new DatabaseConfig(
            DatabaseConfig.DIALECT_POSTGRESQL, HOST, PORT, DATABASE, USERNAME, PASSWORD);

    // Act
    final String url = config.buildJdbcUrl();

    // Assert
    assertAll(
        "URL de PostgreSQL",
        () -> assertEquals("jdbc:postgresql://" + HOST + ":" + PORT + "/" + DATABASE
            + "?stringtype=unspecified", url),
        () -> assertFalse(url.contains("useSSL"), "no debe arrastrar parametros de MySQL"),
        () -> assertTrue(
            url.contains("stringtype=unspecified"),
            "debe incluir stringtype=unspecified para poder enlazar valores sobre columnas ENUM"));
  }

  @Test
  @DisplayName("buildJdbcUrl() cae en MySQL cuando el dialecto viene vacio")
  void shouldFallBackToMySqlWhenDialectIsBlank() {
    // Arrange
    final DatabaseConfig config = new DatabaseConfig("", HOST, PORT, DATABASE, USERNAME, PASSWORD);

    // Act + Assert
    assertAll(
        "dialecto vacio",
        () -> assertFalse(config.isPostgreSql(), "no debe detectar PostgreSQL"),
        () -> assertTrue(config.buildJdbcUrl().startsWith("jdbc:mysql://"), "debe usar MySQL por defecto"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"postgresql", "POSTGRESQL", "PostgreSql"})
  @DisplayName("isPostgreSql() no distingue mayusculas de minusculas")
  void shouldDetectPostgreSqlIgnoringCase(final String dialect) {
    // Arrange
    final DatabaseConfig config = new DatabaseConfig(dialect, HOST, PORT, DATABASE, USERNAME, PASSWORD);

    // Act + Assert
    assertTrue(config.isPostgreSql(), "debe reconocer el dialecto PostgreSQL sin importar el caso");
  }

  @Test
  @DisplayName("el record expone las credenciales sin transformarlas")
  void shouldExposeCredentialsUnchanged() {
    // Arrange
    final DatabaseConfig config =
        new DatabaseConfig(
            DatabaseConfig.DIALECT_POSTGRESQL, HOST, PORT, DATABASE, USERNAME, PASSWORD);

    // Act + Assert
    assertAll(
        "credenciales",
        () -> assertEquals(USERNAME, config.username()),
        () -> assertEquals(PASSWORD, config.password()));
  }
}
