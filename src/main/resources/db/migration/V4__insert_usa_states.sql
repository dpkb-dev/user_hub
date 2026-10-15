-- =====================================================================
-- V4 : Seed USA -> 50 States
-- =====================================================================

INSERT INTO state (state_name, country_id)
SELECT s.state_name, c.country_id
FROM (
          SELECT 'Alabama' AS state_name
    UNION ALL SELECT 'Alaska'
    UNION ALL SELECT 'Arizona'
    UNION ALL SELECT 'Arkansas'
    UNION ALL SELECT 'California'
    UNION ALL SELECT 'Colorado'
    UNION ALL SELECT 'Connecticut'
    UNION ALL SELECT 'Delaware'
    UNION ALL SELECT 'Florida'
    UNION ALL SELECT 'Georgia'
    UNION ALL SELECT 'Hawaii'
    UNION ALL SELECT 'Idaho'
    UNION ALL SELECT 'Illinois'
    UNION ALL SELECT 'Indiana'
    UNION ALL SELECT 'Iowa'
    UNION ALL SELECT 'Kansas'
    UNION ALL SELECT 'Kentucky'
    UNION ALL SELECT 'Louisiana'
    UNION ALL SELECT 'Maine'
    UNION ALL SELECT 'Maryland'
    UNION ALL SELECT 'Massachusetts'
    UNION ALL SELECT 'Michigan'
    UNION ALL SELECT 'Minnesota'
    UNION ALL SELECT 'Mississippi'
    UNION ALL SELECT 'Missouri'
    UNION ALL SELECT 'Montana'
    UNION ALL SELECT 'Nebraska'
    UNION ALL SELECT 'Nevada'
    UNION ALL SELECT 'New Hampshire'
    UNION ALL SELECT 'New Jersey'
    UNION ALL SELECT 'New Mexico'
    UNION ALL SELECT 'New York'
    UNION ALL SELECT 'North Carolina'
    UNION ALL SELECT 'North Dakota'
    UNION ALL SELECT 'Ohio'
    UNION ALL SELECT 'Oklahoma'
    UNION ALL SELECT 'Oregon'
    UNION ALL SELECT 'Pennsylvania'
    UNION ALL SELECT 'Rhode Island'
    UNION ALL SELECT 'South Carolina'
    UNION ALL SELECT 'South Dakota'
    UNION ALL SELECT 'Tennessee'
    UNION ALL SELECT 'Texas'
    UNION ALL SELECT 'Utah'
    UNION ALL SELECT 'Vermont'
    UNION ALL SELECT 'Virginia'
    UNION ALL SELECT 'Washington'
    UNION ALL SELECT 'West Virginia'
    UNION ALL SELECT 'Wisconsin'
    UNION ALL SELECT 'Wyoming'
) s
JOIN country c ON c.country_name = 'USA'
WHERE NOT EXISTS (
    SELECT 1 FROM state x
    WHERE x.state_name = s.state_name AND x.country_id = c.country_id
);
