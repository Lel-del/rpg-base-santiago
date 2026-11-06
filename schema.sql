CREATE DATABASE IF NOT EXISTS rpg_grupal_db
    CHARACTER SET utf8mb4;

USE rpg_grupal_db;

CREATE TABLE IF NOT EXISTS heroes (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    tipo VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    vida_maxima INT NOT NULL,
    nivel INT NOT NULL,
    CONSTRAINT chk_heroe_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
    CONSTRAINT chk_heroe_tipo CHECK (tipo IN ('guerrero', 'mago')),
    CONSTRAINT chk_heroe_vida CHECK (vida_maxima >= 1),
    CONSTRAINT chk_heroe_nivel CHECK (nivel >= 1)
);
