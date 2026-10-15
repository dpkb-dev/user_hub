package com.spring.service;

import com.spring.dto.CityDto;
import com.spring.dto.CountryDto;
import com.spring.dto.QuoteApiResponseDto;
import com.spring.dto.ResetPwdDto;
import com.spring.dto.StateDto;
import com.spring.dto.UserDto;
import com.spring.entities.City;
import com.spring.entities.Country;
import com.spring.entities.State;
import com.spring.entities.User;
import com.spring.repo.CityRepository;
import com.spring.repo.CountryRepository;
import com.spring.repo.StateRepository;
import com.spring.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserServiceImpl.
 * - NO Spring context, NO database: runs in milliseconds.
 * - Repositories + EmailService are Mockito mocks (fake objects we control).
 * - ModelMapper is REAL, so mapping bugs (like the empty {} DTOs) are caught.
 * Test case IDs (TC-Sxx) match notes/04-test-cases.md.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CountryRepository countryRepository;
    @Mock private StateRepository stateRepository;
    @Mock private CityRepository cityRepository;
    @Mock private EmailService emailService;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        // Same constructor Lombok generates (@RequiredArgsConstructor) - built by hand
        // so we can pass a REAL ModelMapper together with the mocks.
        service = new UserServiceImpl(userRepository, countryRepository, stateRepository,
                cityRepository, emailService, new ModelMapper());
    }

    // ---------- small helpers to build test entities ----------

    private static Country country(int id, String name) {
        Country c = new Country();
        c.setCountryId(id);
        c.setCountryName(name);
        return c;
    }

    private static State state(int id, String name, Country country) {
        State s = new State();
        s.setStateId(id);
        s.setStateName(name);
        s.setCountry(country);
        return s;
    }

    private static City city(int id, String name, State state) {
        City c = new City();
        c.setCityId(id);
        c.setCityName(name);
        c.setState(state);
        return c;
    }

    // ==========================================================
    // 3.1 Lookups  (TC-S01 .. TC-S07)
    // ==========================================================
    @Nested
    @DisplayName("Lookups: countries, states, cities, email uniqueness")
    class Lookups {

        @Test
        @DisplayName("TC-S01 getCountries maps every Country entity to CountryDto")
        void getCountries_mapsEntitiesToDtos() {
            // Arrange (Given)
            when(countryRepository.findAll())
                    .thenReturn(List.of(country(1, "India"), country(2, "USA")));

            // Act (When)
            List<CountryDto> result = service.getCountries();

            // Assert (Then)
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCountryId()).isEqualTo(1);
            assertThat(result.get(0).getCountryName()).isEqualTo("India");
            assertThat(result.get(1).getCountryName()).isEqualTo("USA");
        }

        @Test
        @DisplayName("TC-S02 getCountries returns an empty list (not null) when table is empty")
        void getCountries_emptyTable_returnsEmptyList() {
            when(countryRepository.findAll()).thenReturn(List.of());

            List<CountryDto> result = service.getCountries();

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("TC-S03 getStates maps states of the given country and queries with that id")
        void getStates_mapsStatesOfCountry() {
            Country india = country(1, "India");
            when(stateRepository.findByCountryCountryId(1))
                    .thenReturn(List.of(state(11, "Karnataka", india), state(23, "Tamil Nadu", india)));

            List<StateDto> result = service.getStates(1);

            assertThat(result).extracting(StateDto::getStateName)
                    .containsExactly("Karnataka", "Tamil Nadu");
            assertThat(result.get(0).getStateId()).isEqualTo(11);
            verify(stateRepository).findByCountryCountryId(1);   // called with the right id
        }

        @Test
        @DisplayName("TC-S04 getStates returns an empty list for an unknown country")
        void getStates_unknownCountry_returnsEmptyList() {
            when(stateRepository.findByCountryCountryId(999)).thenReturn(List.of());

            assertThat(service.getStates(999)).isEmpty();
        }

        @Test
        @DisplayName("TC-S05 getCities maps cities with real values (regression: no empty {} DTOs)")
        void getCities_mapsCitiesWithValues() {
            State karnataka = state(11, "Karnataka", country(1, "India"));
            when(cityRepository.findByStateStateId(11))
                    .thenReturn(List.of(city(1, "Bengaluru", karnataka), city(2, "Mysuru", karnataka)));

            List<CityDto> result = service.getCities(11);

            assertThat(result).hasSize(2);
            // If CityDto lost @Data, these would be null -> this test fails
            assertThat(result).extracting(CityDto::getCityId).containsExactly(1, 2);
            assertThat(result).extracting(CityDto::getCityName).containsExactly("Bengaluru", "Mysuru");
        }

        @Test
        @DisplayName("TC-S06 isEmailUnique is true when the email does not exist")
        void isEmailUnique_newEmail_true() {
            when(userRepository.existsByEmail("new@gmail.com")).thenReturn(false);

            assertThat(service.isEmailUnique("new@gmail.com")).isTrue();
        }

        @Test
        @DisplayName("TC-S07 isEmailUnique is false when the email already exists")
        void isEmailUnique_existingEmail_false() {
            when(userRepository.existsByEmail("shreedeepak8@gmail.com")).thenReturn(true);

            assertThat(service.isEmailUnique("shreedeepak8@gmail.com")).isFalse();
        }
    }

    // ==========================================================
    // 3.2 Register  (TC-S10 .. TC-S16)
    // ==========================================================
    @Nested
    @DisplayName("Register")
    class Register {

        private static final String EMAIL = "deeps@gmail.com";

        private final Country india = country(1, "India");
        private final State karnataka = state(11, "Karnataka", india);
        private final City bengaluru = city(1, "Bengaluru", karnataka);

        /** The JSON a client would send to POST /user, as a DTO. */
        private UserDto registerRequest() {
            UserDto dto = new UserDto();
            dto.setName("Deeps");
            dto.setEmail(EMAIL);
            dto.setPhno(9876543210L);
            dto.setCountryId(1);
            dto.setStateId(11);
            dto.setCityId(1);
            return dto;
        }

        /** Stubs everything a successful registration needs. */
        private void stubHappyPath(boolean mailSent) {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(countryRepository.findById(1)).thenReturn(Optional.of(india));
            when(stateRepository.findById(11)).thenReturn(Optional.of(karnataka));
            when(cityRepository.findById(1)).thenReturn(Optional.of(bengaluru));
            // save(...) returns the same object it receives (like the real repo would)
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(emailService.sendEmail(anyString(), anyString(), eq(EMAIL))).thenReturn(mailSent);
        }

        @Test
        @DisplayName("TC-S10 duplicate email: returns false, nothing saved, no mail")
        void register_duplicateEmail_returnsFalse() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            boolean result = service.register(registerRequest());

            assertThat(result).isFalse();
            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("TC-S11 new email + valid ids + mail OK: returns true and saves once")
        void register_success_returnsTrue() {
            stubHappyPath(true);

            boolean result = service.register(registerRequest());

            assertThat(result).isTrue();
            verify(userRepository).save(any(User.class));   // exactly once (default)
        }

        @Test
        @DisplayName("TC-S12 saved User has name, email, phone, NO flag and locations")
        void register_savedUserHasAllFields() {
            stubHappyPath(true);
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

            service.register(registerRequest());

            verify(userRepository).save(captor.capture());   // grab the User passed to save()
            User saved = captor.getValue();
            assertThat(saved.getUserName()).isEqualTo("Deeps");
            assertThat(saved.getEmail()).isEqualTo(EMAIL);
            assertThat(saved.getPhoneNumber()).isEqualTo(9876543210L);   // regression: phno not mapped
            assertThat(saved.getPasswordUpdated()).isEqualTo("NO");
            assertThat(saved.getCountry()).isSameAs(india);
            assertThat(saved.getState()).isSameAs(karnataka);
            assertThat(saved.getCity()).isSameAs(bengaluru);
        }

        @Test
        @DisplayName("TC-S13 temporary password is 5 chars from A-Z and 1-9")
        void register_generatesTemporaryPassword() {
            stubHappyPath(true);
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

            service.register(registerRequest());

            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).matches("[A-Z1-9]{5}");
        }

        @Test
        @DisplayName("TC-S14 mail goes to the user's email and contains the temp password")
        void register_sendsTempPasswordByMail() {
            stubHappyPath(true);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

            service.register(registerRequest());

            verify(userRepository).save(userCaptor.capture());
            verify(emailService).sendEmail(anyString(), bodyCaptor.capture(), eq(EMAIL));
            String tempPwd = userCaptor.getValue().getPassword();
            assertThat(bodyCaptor.getValue()).contains(tempPwd);
        }

        @Test
        @DisplayName("TC-S15 mail fails: returns false BUT user is already saved (documents open item B5)")
        void register_mailFails_returnsFalse_butUserSaved() {
            stubHappyPath(false);

            boolean result = service.register(registerRequest());

            assertThat(result).isFalse();
            verify(userRepository).save(any(User.class));   // row exists although client sees FAILED
        }

        @Test
        @DisplayName("TC-S16 unknown countryId: throws NoSuchElementException, nothing saved")
        void register_unknownCountry_throws() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(countryRepository.findById(1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.register(registerRequest()))
                    .isInstanceOf(NoSuchElementException.class);
            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }
    }

    // ==========================================================
    // 3.3 Login  (TC-S20 .. TC-S24)
    // ==========================================================
    @Nested
    @DisplayName("Login")
    class Login {

        private static final String EMAIL = "deeps@gmail.com";
        private static final String PWD = "X7K2P";

        /** A user row as it looks in the DB right after registration. */
        private User registeredUser() {
            Country india = country(1, "India");
            State karnataka = state(11, "Karnataka", india);
            User u = new User();
            u.setUserId(2);
            u.setUserName("Deeps");
            u.setEmail(EMAIL);
            u.setPassword(PWD);
            u.setPasswordUpdated("NO");
            u.setPhoneNumber(9876543210L);
            u.setCountry(india);
            u.setState(karnataka);
            u.setCity(city(1, "Bengaluru", karnataka));
            return u;
        }

        @Test
        @DisplayName("TC-S20 correct credentials: returns DTO with id, name and email")
        void login_success_basicFields() {
            when(userRepository.findByEmailAndPassword(EMAIL, PWD)).thenReturn(registeredUser());

            UserDto dto = service.login(EMAIL, PWD);

            assertThat(dto).isNotNull();
            assertThat(dto.getUserId()).isEqualTo(2);
            assertThat(dto.getName()).isEqualTo("Deeps");      // userName -> name (ModelMapper)
            assertThat(dto.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("TC-S21 pwdUpdated is copied - UI needs it to force a reset (regression)")
        void login_success_pwdUpdatedNotNull() {
            when(userRepository.findByEmailAndPassword(EMAIL, PWD)).thenReturn(registeredUser());

            UserDto dto = service.login(EMAIL, PWD);

            assertThat(dto.getPwdUpdated()).isEqualTo("NO");
        }

        @Test
        @DisplayName("TC-S22 phone number is copied (regression)")
        void login_success_phoneCopied() {
            when(userRepository.findByEmailAndPassword(EMAIL, PWD)).thenReturn(registeredUser());

            UserDto dto = service.login(EMAIL, PWD);

            assertThat(dto.getPhno()).isEqualTo(9876543210L);
        }

        @Test
        @DisplayName("TC-S23 country/state/city ids come from the nested entities")
        void login_success_locationIds() {
            when(userRepository.findByEmailAndPassword(EMAIL, PWD)).thenReturn(registeredUser());

            UserDto dto = service.login(EMAIL, PWD);

            assertThat(dto.getCountryId()).isEqualTo(1);
            assertThat(dto.getStateId()).isEqualTo(11);
            assertThat(dto.getCityId()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-S24 wrong credentials: returns null")
        void login_wrongPassword_returnsNull() {
            when(userRepository.findByEmailAndPassword(EMAIL, "WRONG")).thenReturn(null);

            assertThat(service.login(EMAIL, "WRONG")).isNull();
        }
    }

    // ==========================================================
    // 3.4 Reset password  (TC-S30 .. TC-S34)
    // ==========================================================
    @Nested
    @DisplayName("Reset password")
    class ResetPassword {

        private static final String EMAIL = "deeps@gmail.com";
        private static final String TEMP_PWD = "X7K2P";

        private User userWithTempPassword() {
            User u = new User();
            u.setEmail(EMAIL);
            u.setPassword(TEMP_PWD);
            u.setPasswordUpdated("NO");
            return u;
        }

        private ResetPwdDto request(String oldPwd, String newPwd, String confirmPwd) {
            ResetPwdDto dto = new ResetPwdDto();
            dto.setEmail(EMAIL);
            dto.setOldPwd(oldPwd);
            dto.setNewPwd(newPwd);
            dto.setConfirmPwd(confirmPwd);
            return dto;
        }

        @Test
        @DisplayName("TC-S30 unknown email: false, nothing saved")
        void reset_unknownEmail_false() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(null);

            boolean result = service.resetPwd(request(TEMP_PWD, "Deeps@123", "Deeps@123"));

            assertThat(result).isFalse();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-S31 wrong old password: false, nothing saved (security regression)")
        void reset_wrongOldPassword_false() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(userWithTempPassword());

            boolean result = service.resetPwd(request("WRONG", "Deeps@123", "Deeps@123"));

            assertThat(result).isFalse();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-S32 new and confirm passwords differ: false, nothing saved")
        void reset_confirmMismatch_false() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(userWithTempPassword());

            boolean result = service.resetPwd(request(TEMP_PWD, "Deeps@123", "Other@123"));

            assertThat(result).isFalse();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-S33 new password missing (null): false, no exception")
        void reset_nullNewPassword_false() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(userWithTempPassword());

            boolean result = service.resetPwd(request(TEMP_PWD, null, null));

            assertThat(result).isFalse();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-S34 all correct: true, saves new password and YES flag")
        void reset_success_savesNewPasswordAndFlag() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(userWithTempPassword());
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

            boolean result = service.resetPwd(request(TEMP_PWD, "Deeps@123", "Deeps@123"));

            assertThat(result).isTrue();
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("Deeps@123");
            assertThat(captor.getValue().getPasswordUpdated()).isEqualTo("YES");
        }
    }

    // ==========================================================
    // 3.5 Quote  (TC-S40 .. TC-S42)
    // getQuote() does "new RestTemplate()" INSIDE the method, so we can't pass a @Mock.
    // mockConstruction(...) intercepts that "new" during the try-block and returns a mock.
    // (After rectification B9 - injected client - these become plain @Mock tests.)
    // ==========================================================
    @Nested
    @DisplayName("Quote (external API)")
    class Quote {

        private static final String URL = "https://dummyjson.com/quotes/random";

        private QuoteApiResponseDto sampleQuote() {
            QuoteApiResponseDto q = new QuoteApiResponseDto();
            q.setId(12);
            q.setQuote("Simplicity is the soul of efficiency.");
            q.setAuthor("Austin Freeman");
            return q;
        }

        @Test
        @DisplayName("TC-S40 returns the body of the API response; calls the right URL once")
        void getQuote_returnsBody() {
            QuoteApiResponseDto quote = sampleQuote();

            // Every "new RestTemplate()" inside this block returns a mock, pre-stubbed here:
            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> when(mock.getForEntity(URL, QuoteApiResponseDto.class))
                            .thenReturn(ResponseEntity.ok(quote)))) {

                QuoteApiResponseDto result = service.getQuote();

                assertThat(result).isSameAs(quote);
                assertThat(mocked.constructed()).hasSize(1);                // exactly one RestTemplate created
                verify(mocked.constructed().get(0)).getForEntity(URL, QuoteApiResponseDto.class);
            }
            // outside the try-block, "new RestTemplate()" is real again
        }

        @Test
        @DisplayName("TC-S41 API down: exception propagates (no handling yet - open items B2/B9)")
        void getQuote_apiDown_throws() {
            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> when(mock.getForEntity(URL, QuoteApiResponseDto.class))
                            .thenThrow(new ResourceAccessException("Connection refused")))) {

                assertThatThrownBy(() -> service.getQuote())
                        .isInstanceOf(ResourceAccessException.class)
                        .hasMessageContaining("Connection refused");
            }
        }

        @Test
        @DisplayName("TC-S42 API answers with no body: returns null (controller then answers 'Quote Fetch Failed')")
        void getQuote_emptyBody_returnsNull() {
            ResponseEntity<QuoteApiResponseDto> noBody = ResponseEntity.noContent().build();

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> when(mock.getForEntity(URL, QuoteApiResponseDto.class))
                            .thenReturn(noBody))) {

                assertThat(service.getQuote()).isNull();
            }
        }
    }
}
