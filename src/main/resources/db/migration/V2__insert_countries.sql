-- =====================================================================
-- V2 : Seed countries
-- "WHERE NOT EXISTS" = skip a row that is already there
-- (e.g. you inserted it manually before Flyway) -> no duplicates.
-- =====================================================================

INSERT INTO country (country_name)
SELECT 'India' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM country WHERE country_name = 'India');

INSERT INTO country (country_name)
SELECT 'USA' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM country WHERE country_name = 'USA');
