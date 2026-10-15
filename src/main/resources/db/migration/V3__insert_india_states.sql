-- =====================================================================
-- V3 : Seed India -> 28 States + 8 Union Territories (36 rows)
-- country_id is looked up by name, so it works whatever id India got.
-- =====================================================================

INSERT INTO state (state_name, country_id)
SELECT s.state_name, c.country_id
FROM (
          SELECT 'Andhra Pradesh' AS state_name
    UNION ALL SELECT 'Arunachal Pradesh'
    UNION ALL SELECT 'Assam'
    UNION ALL SELECT 'Bihar'
    UNION ALL SELECT 'Chhattisgarh'
    UNION ALL SELECT 'Goa'
    UNION ALL SELECT 'Gujarat'
    UNION ALL SELECT 'Haryana'
    UNION ALL SELECT 'Himachal Pradesh'
    UNION ALL SELECT 'Jharkhand'
    UNION ALL SELECT 'Karnataka'
    UNION ALL SELECT 'Kerala'
    UNION ALL SELECT 'Madhya Pradesh'
    UNION ALL SELECT 'Maharashtra'
    UNION ALL SELECT 'Manipur'
    UNION ALL SELECT 'Meghalaya'
    UNION ALL SELECT 'Mizoram'
    UNION ALL SELECT 'Nagaland'
    UNION ALL SELECT 'Odisha'
    UNION ALL SELECT 'Punjab'
    UNION ALL SELECT 'Rajasthan'
    UNION ALL SELECT 'Sikkim'
    UNION ALL SELECT 'Tamil Nadu'
    UNION ALL SELECT 'Telangana'
    UNION ALL SELECT 'Tripura'
    UNION ALL SELECT 'Uttar Pradesh'
    UNION ALL SELECT 'Uttarakhand'
    UNION ALL SELECT 'West Bengal'
    -- Union Territories
    UNION ALL SELECT 'Andaman and Nicobar Islands'
    UNION ALL SELECT 'Chandigarh'
    UNION ALL SELECT 'Dadra and Nagar Haveli and Daman and Diu'
    UNION ALL SELECT 'Delhi'
    UNION ALL SELECT 'Jammu and Kashmir'
    UNION ALL SELECT 'Ladakh'
    UNION ALL SELECT 'Lakshadweep'
    UNION ALL SELECT 'Puducherry'
) s
JOIN country c ON c.country_name = 'India'
WHERE NOT EXISTS (
    SELECT 1 FROM state x
    WHERE x.state_name = s.state_name AND x.country_id = c.country_id
);
