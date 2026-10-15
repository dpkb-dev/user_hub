package com.spring.rest;

import com.spring.dto.CityDto;
import com.spring.dto.CountryDto;
import com.spring.dto.QuoteApiResponseDto;
import com.spring.dto.ResetPwdDto;
import com.spring.dto.StateDto;
import com.spring.dto.UserDto;
import com.spring.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller "slice" tests.
 * - @WebMvcTest starts ONLY the web layer: DispatcherServlet, Jackson, UserRestController.
 *   No database, no Flyway, no mail, no service implementation.
 * - UserService is replaced by a Mockito mock (@MockitoBean) inside the Spring context.
 * - MockMvc sends fake HTTP requests without starting Tomcat.
 * Expected statuses describe CURRENT behaviour (e.g. 500 for "not found");
 * tests marked "(B1)" change when rectification B1 (correct status codes) is applied.
 */
@WebMvcTest(UserRestController.class)
class UserRestControllerTest {

    @Autowired
    private MockMvc mockMvc;              // fake HTTP client, ready-made by @WebMvcTest

    @MockitoBean
    private UserService userService;      // mock that replaces the real service bean

    // ---------- helpers ----------
    private static CountryDto countryDto(int id, String name) {
        CountryDto c = new CountryDto();
        c.setCountryId(id);
        c.setCountryName(name);
        return c;
    }
    private static StateDto stateDto(int id, String name) {
        StateDto s = new StateDto();
        s.setStateId(id);
        s.setStateName(name);
        return s;
    }
    private static CityDto cityDto(int id, String name) {
        CityDto c = new CityDto();
        c.setCityId(id);
        c.setCityName(name);
        return c;
    }

    // ==========================================================
    // Lookups  (TC-C01 .. TC-C05)
    // ==========================================================
    @Nested
    @DisplayName("GET lookups")
    class Lookups {

        @Test
        @DisplayName("TC-C01 GET /countries -> 200 with wrapper and list")
        void countries_ok() throws Exception {
            when(userService.getCountries()).thenReturn(List.of(countryDto(1, "India"), countryDto(2, "USA")));

            mockMvc.perform(get("/countries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Fetched Countries Successfully"))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].countryName").value("India"));
        }

        @Test
        @DisplayName("TC-C02 GET /countries empty -> 500 today (B1: should be 200 with [])")
        void countries_empty_500() throws Exception {
            when(userService.getCountries()).thenReturn(List.of());

            mockMvc.perform(get("/countries"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("No countries found"))
                    .andExpect(jsonPath("$.data").value(nullValue()));
        }

        @Test
        @DisplayName("TC-C03 GET /states/1 -> 200, path variable passed to the service")
        void states_ok() throws Exception {
            when(userService.getStates(1)).thenReturn(List.of(stateDto(11, "Karnataka")));

            mockMvc.perform(get("/states/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].stateName").value("Karnataka"));
            verify(userService).getStates(1);
        }

        @Test
        @DisplayName("TC-C04 GET /states/abc -> 400 (type mismatch), service never called")
        void states_badId_400() throws Exception {
            mockMvc.perform(get("/states/abc"))
                    .andExpect(status().isBadRequest());
            verify(userService, never()).getStates(any());
        }

        @Test
        @DisplayName("TC-C05 GET /cities/11 -> 200 with real city names (no {} objects)")
        void cities_ok() throws Exception {
            when(userService.getCities(11)).thenReturn(List.of(cityDto(1, "Bengaluru"), cityDto(2, "Mysuru")));

            mockMvc.perform(get("/cities/11"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].cityId").value(1))
                    .andExpect(jsonPath("$.data[0].cityName").value("Bengaluru"));
        }
    }

    // ==========================================================
    // Email uniqueness  (TC-C06 .. TC-C07)
    // ==========================================================
    @Nested
    @DisplayName("GET /unique/{email}")
    class UniqueEmail {

        @Test
        @DisplayName("TC-C06 unique email -> 200 UNIQUE")
        void unique_ok() throws Exception {
            when(userService.isEmailUnique("new@gmail.com")).thenReturn(true);

            mockMvc.perform(get("/unique/new@gmail.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("UNIQUE"));
        }

        @Test
        @DisplayName("TC-C07 duplicate email -> 500 DUPLICATE today (B1: 200 or 409)")
        void unique_duplicate() throws Exception {
            when(userService.isEmailUnique("deeps@gmail.com")).thenReturn(false);

            mockMvc.perform(get("/unique/deeps@gmail.com"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.data").value("DUPLICATE"));
        }
    }

    // ==========================================================
    // Register  (TC-C08 .. TC-C10)
    // ==========================================================
    @Nested
    @DisplayName("POST /user")
    class Register {

        private static final String BODY = """
                {"name":"Deeps","email":"deeps@gmail.com","phno":9876543210,
                 "countryId":1,"stateId":11,"cityId":1}
                """;

        @Test
        @DisplayName("TC-C08 valid JSON -> 200 SUCCESS (B1: should be 201)")
        void register_ok() throws Exception {
            when(userService.register(any(UserDto.class))).thenReturn(true);

            mockMvc.perform(post("/user").contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("SUCCESS"));
        }

        @Test
        @DisplayName("TC-C09 service says no (duplicate / mail failed) -> 500 FAILED (B1: 409)")
        void register_failed() throws Exception {
            when(userService.register(any(UserDto.class))).thenReturn(false);

            mockMvc.perform(post("/user").contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.data").value("FAILED"));
        }

        @Test
        @DisplayName("TC-C10 malformed JSON (trailing comma) -> 400, service never called")
        void register_malformedJson_400() throws Exception {
            mockMvc.perform(post("/user").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"deeps@gmail.com\",}"))
                    .andExpect(status().isBadRequest());
            verify(userService, never()).register(any());
        }
    }

    // ==========================================================
    // Login  (TC-C11 .. TC-C13)
    // ==========================================================
    @Nested
    @DisplayName("POST /login")
    class Login {

        private static final String BODY = """
                {"email":"deeps@gmail.com","pwd":"X7K2P"}
                """;

        @Test
        @DisplayName("TC-C11 success -> 200, pwdUpdated present, password NEVER in JSON")
        void login_ok_noPasswordInResponse() throws Exception {
            UserDto dto = new UserDto();
            dto.setUserId(2);
            dto.setName("Deeps");
            dto.setEmail("deeps@gmail.com");
            dto.setPwd("X7K2P");                 // even if the service leaked it...
            dto.setPwdUpdated("NO");
            when(userService.login("deeps@gmail.com", "X7K2P")).thenReturn(dto);

            mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pwdUpdated").value("NO"))
                    .andExpect(jsonPath("$.data.pwd").doesNotExist());   // ...WRITE_ONLY hides it
        }

        @Test
        @DisplayName("TC-C12 wrong credentials -> 500 today (B1: 401)")
        void login_wrong() throws Exception {
            when(userService.login("deeps@gmail.com", "X7K2P")).thenReturn(null);

            mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("User Login Failed"));
        }

        @Test
        @DisplayName("TC-C13 JSON body reaches the service (regression: missing @RequestBody)")
        void login_bodyIsBound() throws Exception {
            mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(BODY));

            verify(userService).login("deeps@gmail.com", "X7K2P");     // not (null, null)
        }
    }

    // ==========================================================
    // Reset password + quote  (TC-C14 .. TC-C16)
    // ==========================================================
    @Nested
    @DisplayName("POST /resetpwd and GET /quote")
    class ResetAndQuote {

        private static final String RESET_BODY = """
                {"email":"deeps@gmail.com","oldPwd":"X7K2P",
                 "newPwd":"Deeps@123","confirmPwd":"Deeps@123"}
                """;

        @Test
        @DisplayName("TC-C14 reset ok -> 200 SUCCESS")
        void reset_ok() throws Exception {
            when(userService.resetPwd(any(ResetPwdDto.class))).thenReturn(true);

            mockMvc.perform(post("/resetpwd").contentType(MediaType.APPLICATION_JSON).content(RESET_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("SUCCESS"));
        }

        @Test
        @DisplayName("TC-C15 reset rejected -> 500 FAILED today (B1: 400/404)")
        void reset_failed() throws Exception {
            when(userService.resetPwd(any(ResetPwdDto.class))).thenReturn(false);

            mockMvc.perform(post("/resetpwd").contentType(MediaType.APPLICATION_JSON).content(RESET_BODY))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.data").value("FAILED"));
        }

        @Test
        @DisplayName("TC-C16 GET /quote -> 200 with quote fields")
        void quote_ok() throws Exception {
            QuoteApiResponseDto q = new QuoteApiResponseDto();
            q.setId(12);
            q.setQuote("Simplicity is the soul of efficiency.");
            q.setAuthor("Austin Freeman");
            when(userService.getQuote()).thenReturn(q);

            mockMvc.perform(get("/quote"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.author").value("Austin Freeman"));
        }
    }
}
