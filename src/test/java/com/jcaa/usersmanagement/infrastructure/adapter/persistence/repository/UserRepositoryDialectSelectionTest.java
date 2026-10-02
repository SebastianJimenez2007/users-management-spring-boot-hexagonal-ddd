package com.jcaa.usersmanagement.infrastructure.adapter.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.jcaa.usersmanagement.application.port.out.SaveUserPort;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Tests for the dialect-based selection between the MySQL and PostgreSQL adapters.
 *
 * <p>Ambos repositorios implementan los mismos seis puertos de salida. Si ambos quedaran activos a
 * la vez, Spring fallaria al arrancar con NoUniqueBeanDefinitionException al inyectar cualquier
 * servicio. Estos tests fijan el contrato de que siempre hay exactamente un repositorio.
 */
@DisplayName("Seleccion de repositorio por dialecto")
class UserRepositoryDialectSelectionTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(BothRepositoriesConfig.class);

  @Test
  @DisplayName("sin db.dialect se registra solo el adaptador de MySQL")
  void shouldRegisterOnlyMySqlAdapterByDefault() {
    // Act
    contextRunner.run(
        context -> {
          // Assert
          assertThat(context).hasSingleBean(UserRepositoryMySQL.class);
          assertThat(context).doesNotHaveBean(UserRepositoryPostgres.class);
          assertThat(context).hasSingleBean(SaveUserPort.class);
        });
  }

  @Test
  @DisplayName("db.dialect=mysql registra solo el adaptador de MySQL")
  void shouldRegisterOnlyMySqlAdapterWhenDialectIsMySql() {
    // Act
    contextRunner
        .withPropertyValues("db.dialect=mysql")
        .run(
            context -> {
              // Assert
              assertThat(context).hasSingleBean(UserRepositoryMySQL.class);
              assertThat(context).doesNotHaveBean(UserRepositoryPostgres.class);
            });
  }

  @Test
  @DisplayName("db.dialect=postgresql registra solo el adaptador de PostgreSQL")
  void shouldRegisterOnlyPostgresAdapterWhenDialectIsPostgreSql() {
    // Act
    contextRunner
        .withPropertyValues("db.dialect=postgresql")
        .run(
            context -> {
              // Assert
              assertThat(context).hasSingleBean(UserRepositoryPostgres.class);
              assertThat(context).doesNotHaveBean(UserRepositoryMySQL.class);
              assertThat(context).hasSingleBean(SaveUserPort.class);
            });
  }

  @Test
  @DisplayName("con cualquiera de los dos dialectos el contexto arranca sin ambigüedad de beans")
  void shouldStartWithoutAmbiguousBeanForEitherDialect() {
    // Act + Assert
    contextRunner
        .withPropertyValues("db.dialect=mysql")
        .run(context -> assertThat(context).hasNotFailed());
    contextRunner
        .withPropertyValues("db.dialect=postgresql")
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Configuration(proxyBeanMethods = false)
  @Import({UserRepositoryMySQL.class, UserRepositoryPostgres.class})
  static class BothRepositoriesConfig {

    @Bean
    DataSource dataSource() {
      return mock(DataSource.class);
    }
  }
}
