package com.spring.repo;

import com.spring.entities.City;
import com.spring.entities.Country;
import com.spring.entities.State;
import com.spring.entities.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository "slice" tests - REAL SQL against the REAL MySQL test database.
 * - @DataJpaTest starts only JPA: entities, repositories, Hibernate, DataSource (+ Flyway).
 *   No controllers, no services, no mail.
 * - @AutoConfigureTestDatabase(replace = NONE): keep our MySQL instead of swapping in an
 *   in-memory DB (our Flyway scripts are MySQL-specific).
 * - @ActiveProfiles("test"): DataSource = userhub_test (from application-test.properties).
 * - Every test runs in a transaction that is ROLLED BACK at the end -> inserted users vanish,
 *   the seed data (2 countries, 86 states, 12 cities from Flyway V2-V5) stays untouched.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RepositoryTest {

    @Autowired private CountryRepository countryRepository;
    @Autowired private StateRepository stateRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;   // to force real DB reads (flush + clear)

    // ---------- helpers: look up seed data by NAME (ids may differ per DB) ----------
    private Country country(String name) {
        return countryRepository.findAll().stream()
                .filter(c -> c.getCountryName().equals(name)).findFirst().orElseThrow();
    }

    private State state(String name) {
        return stateRepository.findAll().stream()
                .filter(s -> s.getStateName().equals(name)).findFirst().orElseThrow();
    }

    private City city(String name) {
        return cityRepository.findAll().stream()
                .filter(c -> c.getCityName().equals(name)).findFirst().orElseThrow();
    }

    /** Saves a user like register() would (inside this test's transaction). */
    private User saveUser(String email, String password) {
        User u = new User();
        u.setUserName("Repo Test");
        u.setEmail(email);
        u.setPassword(password);
        u.setPasswordUpdated("NO");
        u.setPhoneNumber(9876543210L);
        u.setCountry(country("India"));
        u.setState(state("Karnataka"));
        u.setCity(city("Bengaluru"));
        return userRepository.save(u);
    }

    // ==========================================================
    // Seed data + derived queries on locations  (TC-R01 .. TC-R05)
    // ==========================================================
    @Nested
    @DisplayName("Locations (Flyway seed data + derived queries)")
    class Locations {

        @Test
        @DisplayName("TC-R01 Flyway seeded exactly 2 countries: India and USA")
        void seededCountries() {
            assertThat(countryRepository.findAll())
                    .extracting(Country::getCountryName)
                    .containsExactlyInAnyOrder("India", "USA");
        }

        @Test
        @DisplayName("TC-R02 findByCountryCountryId(India) returns 36 states incl. Karnataka")
        void indiaStates() {
            List<State> states = stateRepository.findByCountryCountryId(country("India").getCountryId());

            assertThat(states).hasSize(36);
            assertThat(states).extracting(State::getStateName).contains("Karnataka", "Tamil Nadu", "Delhi");
        }

        @Test
        @DisplayName("TC-R03 findByCountryCountryId(USA) returns 50 states")
        void usaStates() {
            List<State> states = stateRepository.findByCountryCountryId(country("USA").getCountryId());

            assertThat(states).hasSize(50);
            assertThat(states).extracting(State::getStateName).contains("California", "Texas");
        }

        @Test
        @DisplayName("TC-R04 findByStateStateId(Karnataka) returns its 3 cities")
        void karnatakaCities() {
            List<City> cities = cityRepository.findByStateStateId(state("Karnataka").getStateId());

            assertThat(cities).extracting(City::getCityName)
                    .containsExactlyInAnyOrder("Bengaluru", "Mysuru", "Mangaluru");
        }

        @Test
        @DisplayName("TC-R05 unknown country id returns an empty list")
        void unknownCountry() {
            assertThat(stateRepository.findByCountryCountryId(999_999)).isEmpty();
        }
    }

    // ==========================================================
    // User queries + column mapping  (TC-R06 .. TC-R10)
    // ==========================================================
    @Nested
    @DisplayName("User (save + derived queries + column types)")
    class Users {

        private static final String EMAIL = "repo.test@example.com";

        @Test
        @DisplayName("TC-R06 existsByEmail: true for a saved email, false for another")
        void existsByEmail() {
            saveUser(EMAIL, "X7K2P");

            assertThat(userRepository.existsByEmail(EMAIL)).isTrue();
            assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
        }

        @Test
        @DisplayName("TC-R07 findByEmailAndPassword: found with right password, null with wrong")
        void findByEmailAndPassword() {
            saveUser(EMAIL, "X7K2P");

            assertThat(userRepository.findByEmailAndPassword(EMAIL, "X7K2P")).isNotNull();
            assertThat(userRepository.findByEmailAndPassword(EMAIL, "WRONG")).isNull();
        }

        @Test
        @DisplayName("TC-R08 created_at and last_updated_at are filled by Hibernate (regression A9)")
        void timestampsFilled() {
            User saved = saveUser(EMAIL, "X7K2P");

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getLastUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("TC-R09 10-digit phone survives a real DB round-trip (regression A8, BIGINT)")
        void phoneBigintRoundTrip() {
            Integer id = saveUser(EMAIL, "X7K2P").getUserId();
            entityManager.flush();   // push the INSERT to MySQL now
            entityManager.clear();   // forget cached objects -> next read really hits the DB

            User fromDb = userRepository.findById(id).orElseThrow();

            assertThat(fromDb.getPhoneNumber()).isEqualTo(9876543210L);
            assertThat(fromDb.getCity().getCityName()).isEqualTo("Bengaluru");   // FK works too
        }

        @Test
        @DisplayName("TC-R10 findByEmail returns the user (used by resetPwd)")
        void findByEmail() {
            saveUser(EMAIL, "X7K2P");

            User found = userRepository.findByEmail(EMAIL);

            assertThat(found).isNotNull();
            assertThat(found.getPasswordUpdated()).isEqualTo("NO");
        }
    }
}
