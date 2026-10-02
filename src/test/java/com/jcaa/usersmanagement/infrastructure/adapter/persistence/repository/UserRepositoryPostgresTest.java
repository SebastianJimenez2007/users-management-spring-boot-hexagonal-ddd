package com.jcaa.usersmanagement.infrastructure.adapter.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import com.jcaa.usersmanagement.infrastructure.adapter.persistence.exception.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for UserRepositoryPostgres.
 *
 * <p>Verifica que el adaptador de PostgreSQL hereda correctamente el comportamiento JDBC comun
 * y que traduce los fallos de SQL a las excepciones del dominio.
 */
@DisplayName("UserRepositoryPostgres")
@ExtendWith(MockitoExtension.class)
class UserRepositoryPostgresTest {

  private static final String ID = "u-pg-001";
  private static final String NAME = "Grace Hopper";
  private static final String EMAIL = "grace@example.com";
  private static final String HASH = "$2a$12$abcdefghijklmnopqrstuO";
  private static final String CREATED_AT = "2024-05-01 10:00:00";
  private static final String UPDATED_AT = "2024-05-02 11:30:00";

  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private PreparedStatement statement;
  @Mock private ResultSet resultSet;

  private UserRepositoryPostgres repository;
  private UserModel userModel;

  @BeforeEach
  void setUp() {
    repository = new UserRepositoryPostgres(dataSource);
    userModel =
        new UserModel(
            new UserId(ID),
            new UserName(NAME),
            new UserEmail(EMAIL),
            UserPassword.fromHash(HASH),
            UserRole.MEMBER,
            UserStatus.ACTIVE);
  }

  private void configureStatementAndResultSet() throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
  }

  private void configureResultSetRow() throws SQLException {
    when(resultSet.getString("id")).thenReturn(ID);
    when(resultSet.getString("name")).thenReturn(NAME);
    when(resultSet.getString("email")).thenReturn(EMAIL);
    when(resultSet.getString("password")).thenReturn(HASH);
    when(resultSet.getString("role")).thenReturn("MEMBER");
    when(resultSet.getString("status")).thenReturn("ACTIVE");
    when(resultSet.getString("created_at")).thenReturn(CREATED_AT);
    when(resultSet.getString("updated_at")).thenReturn(UPDATED_AT);
  }

  @Test
  @DisplayName("save() inserta el usuario y lo devuelve leido por id")
  void shouldSaveUserAndReturnById() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(true);
    configureResultSetRow();

    // Act
    final UserModel result = repository.save(userModel);

    // Assert
    assertAll(
        "save() happy path",
        () -> assertEquals(ID, result.getId().value(), "id"),
        () -> assertEquals(NAME, result.getName().value(), "name"),
        () -> assertEquals(EMAIL, result.getEmail().value(), "email"),
        () -> assertEquals(UserRole.MEMBER, result.getRole(), "role"),
        () -> assertEquals(UserStatus.ACTIVE, result.getStatus(), "status"));
  }

  @Test
  @DisplayName("save() lanza PersistenceException si el INSERT falla")
  void shouldThrowPersistenceExceptionWhenInsertFails() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenThrow(new SQLException("Insert failed"));

    // Act + Assert
    assertThrows(
        PersistenceException.class,
        () -> repository.save(userModel),
        "debe lanzar PersistenceException cuando el INSERT lanza SQLException");
  }

  @Test
  @DisplayName("save() lanza UserNotFoundException si el usuario no aparece tras insertar")
  void shouldThrowUserNotFoundExceptionWhenInsertedUserIsNotFound() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(false);

    // Act + Assert
    assertThrows(
        UserNotFoundException.class,
        () -> repository.save(userModel),
        "debe lanzar UserNotFoundException cuando el SELECT posterior no devuelve filas");
  }

  @Test
  @DisplayName("update() actualiza el usuario y lo devuelve leido por id")
  void shouldUpdateUserAndReturnById() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(true);
    configureResultSetRow();

    // Act
    final UserModel result = repository.update(userModel);

    // Assert
    assertEquals(ID, result.getId().value(), "el id debe coincidir con el usuario actualizado");
  }

  @Test
  @DisplayName("update() lanza PersistenceException si el UPDATE falla")
  void shouldThrowPersistenceExceptionWhenUpdateFails() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenThrow(new SQLException("Update failed"));

    // Act + Assert
    assertThrows(
        PersistenceException.class,
        () -> repository.update(userModel),
        "debe lanzar PersistenceException cuando el UPDATE lanza SQLException");
  }

  @Test
  @DisplayName("getById() devuelve el usuario cuando existe la fila")
  void shouldReturnUserByIdWhenFound() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(true);
    configureResultSetRow();

    // Act
    final Optional<UserModel> result = repository.getById(new UserId(ID));

    // Assert
    assertAll(
        "getById() encontrado",
        () -> assertTrue(result.isPresent(), "debe estar presente"),
        () -> assertEquals(ID, result.map(user -> user.getId().value()).orElse(null), "id"));
  }

  @Test
  @DisplayName("getById() devuelve Optional.empty() cuando no existe la fila")
  void shouldReturnEmptyByIdWhenNotFound() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(false);

    // Act
    final Optional<UserModel> result = repository.getById(new UserId(ID));

    // Assert
    assertTrue(result.isEmpty(), "debe devolver Optional.empty() si no hay fila con ese id");
  }

  @Test
  @DisplayName("getById() lanza PersistenceException si falla el cierre del statement")
  void shouldThrowPersistenceExceptionWhenStatementCloseFails() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(false);
    doThrow(new SQLException("Close failed")).when(statement).close();

    // Act + Assert
    assertThrows(
        PersistenceException.class,
        () -> repository.getById(new UserId(ID)),
        "debe lanzar PersistenceException si close() falla tras salir del cuerpo del try");
  }

  @Test
  @DisplayName("getByEmail() devuelve el usuario cuando existe la fila")
  void shouldReturnUserByEmailWhenFound() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(true);
    configureResultSetRow();

    // Act
    final Optional<UserModel> result = repository.getByEmail(new UserEmail(EMAIL));

    // Assert
    assertAll(
        "getByEmail() encontrado",
        () -> assertTrue(result.isPresent(), "debe estar presente"),
        () -> assertEquals(EMAIL, result.map(user -> user.getEmail().value()).orElse(null), "email"));
  }

  @Test
  @DisplayName("getByEmail() devuelve Optional.empty() cuando el correo no existe")
  void shouldReturnEmptyByEmailWhenNotFound() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(false);

    // Act
    final Optional<UserModel> result = repository.getByEmail(new UserEmail(EMAIL));

    // Assert
    assertTrue(result.isEmpty(), "debe devolver Optional.empty() si no hay fila con ese email");
  }

  @Test
  @DisplayName("getByEmail() lanza PersistenceException si el SELECT lanza SQLException")
  void shouldThrowPersistenceExceptionOnGetByEmailFailure() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenThrow(new SQLException("Query failed"));

    // Act + Assert
    assertThrows(
        PersistenceException.class,
        () -> repository.getByEmail(new UserEmail(EMAIL)),
        "debe lanzar PersistenceException cuando la consulta lanza SQLException");
  }

  @Test
  @DisplayName("getAll() devuelve un modelo por fila del result set")
  void shouldReturnAllUsers() throws SQLException {
    // Arrange
    configureStatementAndResultSet();
    when(resultSet.next()).thenReturn(true, false);
    configureResultSetRow();

    // Act
    final List<UserModel> result = repository.getAll();

    // Assert
    assertAll(
        "getAll() happy path",
        () -> assertEquals(1, result.size(), "cantidad de usuarios"),
        () -> assertEquals(ID, result.get(0).getId().value(), "id del primer usuario"));
  }

  @Test
  @DisplayName("getAll() lanza PersistenceException si la consulta falla")
  void shouldThrowPersistenceExceptionOnGetAllFailure() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));

    // Act + Assert
    assertThrows(
        PersistenceException.class,
        () -> repository.getAll(),
        "debe lanzar PersistenceException cuando el SELECT lanza SQLException");
  }

  @Test
  @DisplayName("delete() ejecuta el DELETE sin lanzar excepcion")
  void shouldDeleteUserWithoutThrowing() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);

    // Act + Assert
    assertDoesNotThrow(
        () -> repository.delete(new UserId(ID)),
        "delete() no debe lanzar si el DELETE se ejecuta correctamente");
  }

  @Test
  @DisplayName("delete() lanza PersistenceException si el DELETE falla")
  void shouldThrowPersistenceExceptionWhenDeleteFails() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenThrow(new SQLException("Delete failed"));

    // Act + Assert
    assertThrows(
        PersistenceException.class,
        () -> repository.delete(new UserId(ID)),
        "debe lanzar PersistenceException cuando el DELETE lanza SQLException");
  }
}
