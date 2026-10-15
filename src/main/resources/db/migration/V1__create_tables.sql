-- =====================================================================
-- V1 : Create all tables (mirrors the JPA entities exactly)
-- ---------------------------------------------------------------------
-- Entity  -> Table      Notes
-- Country -> country
-- State   -> state      FK country_id -> country   (State is the MANY side)
-- City    -> city       FK state_id   -> state     (City is the MANY side)
-- User    -> user       FKs country_id / state_id / city_id
--
-- "IF NOT EXISTS" = safe on a database where Hibernate already
-- created these tables earlier (ddl-auto=update days).
-- =====================================================================

CREATE TABLE IF NOT EXISTS country (
    country_id   INT          NOT NULL AUTO_INCREMENT,
    country_name VARCHAR(255),
    PRIMARY KEY (country_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS state (
    state_id   INT          NOT NULL AUTO_INCREMENT,
    state_name VARCHAR(255),
    country_id INT,
    PRIMARY KEY (state_id),
    CONSTRAINT fk_state_country FOREIGN KEY (country_id) REFERENCES country (country_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS city (
    city_id   INT          NOT NULL AUTO_INCREMENT,
    city_name VARCHAR(255),
    state_id  INT,
    PRIMARY KEY (city_id),
    CONSTRAINT fk_city_state FOREIGN KEY (state_id) REFERENCES state (state_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `user` (
    user_id          INT          NOT NULL AUTO_INCREMENT,
    user_name        VARCHAR(255),
    email            VARCHAR(255),
    phone_number     INT,
    password         VARCHAR(255),
    password_updated VARCHAR(255),
    password_reset   VARCHAR(255),
    created_at       DATETIME(6),
    last_updated_at  DATETIME(6),
    country_id       INT,
    state_id         INT,
    city_id          INT,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_country FOREIGN KEY (country_id) REFERENCES country (country_id),
    CONSTRAINT fk_user_state   FOREIGN KEY (state_id)   REFERENCES state (state_id),
    CONSTRAINT fk_user_city    FOREIGN KEY (city_id)    REFERENCES city (city_id)
) ENGINE = InnoDB;
