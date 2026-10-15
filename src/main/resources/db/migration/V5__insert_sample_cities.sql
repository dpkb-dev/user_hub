-- =====================================================================
-- V5 : Seed sample cities (for testing GET /cities/{stateId})
-- state_id is looked up by state name + country name.
-- =====================================================================

INSERT INTO city (city_name, state_id)
SELECT v.city_name, s.state_id
FROM (
          SELECT 'Bengaluru' AS city_name, 'Karnataka' AS state_name, 'India' AS country_name
    UNION ALL SELECT 'Mysuru',        'Karnataka',   'India'
    UNION ALL SELECT 'Mangaluru',     'Karnataka',   'India'
    UNION ALL SELECT 'Chennai',       'Tamil Nadu',  'India'
    UNION ALL SELECT 'Coimbatore',    'Tamil Nadu',  'India'
    UNION ALL SELECT 'Madurai',       'Tamil Nadu',  'India'
    UNION ALL SELECT 'Mumbai',        'Maharashtra', 'India'
    UNION ALL SELECT 'Pune',          'Maharashtra', 'India'
    UNION ALL SELECT 'Los Angeles',   'California',  'USA'
    UNION ALL SELECT 'San Francisco', 'California',  'USA'
    UNION ALL SELECT 'Houston',       'Texas',       'USA'
    UNION ALL SELECT 'Dallas',        'Texas',       'USA'
) v
JOIN country c ON c.country_name = v.country_name
JOIN state   s ON s.state_name = v.state_name AND s.country_id = c.country_id
WHERE NOT EXISTS (
    SELECT 1 FROM city x
    WHERE x.city_name = v.city_name AND x.state_id = s.state_id
);
