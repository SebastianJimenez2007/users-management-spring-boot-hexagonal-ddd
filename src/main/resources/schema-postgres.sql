-- =============================================
-- Script de creacion de la base de datos (PostgreSQL)
-- Gestion de Usuarios - Arquitectura Hexagonal
-- =============================================
-- Diferencias respecto al DDL de MySQL (schema.sql):
--   * ENUM en linea -> tipo enumerated propio (CREATE TYPE).
--   * ENGINE=InnoDB y COLLATE no existen: PostgreSQL ya es UTF8.
--   * ON UPDATE CURRENT_TIMESTAMP no existe: el repositorio actualiza
--     updated_at explicitamente con NOW() en el UPDATE, asi que no hace falta trigger.
--   * No se ejecuta CREATE DATABASE ni USE: en Supabase o Render la base ya existe.
--
-- Ejecutar una sola vez. PostgreSQL no admite IF NOT EXISTS en CREATE TYPE, por eso los
-- tipos se crean sin esa clausula (si la repites, fallara con "type already exists").
-- =============================================

CREATE TYPE user_role AS ENUM ('ADMIN', 'MEMBER', 'REVIEWER');

CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'PENDING', 'BLOCKED');

CREATE TABLE IF NOT EXISTS users (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        user_role    NOT NULL,
    status      user_status  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- Usuario administrador inicial
-- =============================================
-- AVISO: el hash siguiente es un placeholder, NO es un BCrypt valido. El login con
-- "Admin1234!" fallara hasta que lo reemplaces por un hash real.
--
-- Para obtener un hash valido puedes generar uno desde la propia aplicacion, o con el
-- codec BCrypt del proyecto (at.favre.lib.crypto.bcrypt) en un test.
--
-- INSERT INTO users (id, name, email, password, role, status)
-- VALUES (
--     '00000000-0000-0000-0000-000000000001',
--     'Administrador',
--     'admin@example.com',
--     '<pega aqui el hash BCrypt de Admin1234!>',
--     'ADMIN',
--     'ACTIVE'
-- );
--
-- ---------------------------------------------------------------------------------
-- ALTERNATIVA sin tipos enumerados: si al insertar PostgreSQL responde
-- "column "role" is of type user_role but expression is of type character varying",
-- cambia las dos columnas por VARCHAR con restricciones CHECK. Es menos idiomatico pero
-- evita por completo el problema de casteo de los parametros JDBC:
--
--   role   VARCHAR(20) NOT NULL CHECK (role   IN ('ADMIN', 'MEMBER', 'REVIEWER')),
--   status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
--                      CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'BLOCKED')),
-- ---------------------------------------------------------------------------------
