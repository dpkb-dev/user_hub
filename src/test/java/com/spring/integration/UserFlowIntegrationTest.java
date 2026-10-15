package com.spring.integration;

import com.spring.entities.City;
import com.spring.entities.Country;
import com.spring.entities.State;
import com.spring.repo.CityRepository;
import com.spring.repo.CountryRepository;
import com.spring.repo.StateRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-I02 - full user journey through the WHOLE application:
 *   real controller + real service + real ModelMapper + real repositories + real MySQL (userhub_test)
 *   Only the mail server is fake: JavaMailSender is a @MockitoBean, so we can READ the
 *   "sent" mail and take the temporary password out of it - exactly like a user would.
 * @Transactional: everything the test writes is rolled back at the end (DB stays clean).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserFlowIntegrationTest {

    private static final String EMAIL = "flow.test@example.com";
    private static final String NEW_PWD = "Deeps@123";

    @Autowired private MockMvc mockMvc;
    @Autowired private CountryRepository countryRepository;
    @Autowired private StateRepository stateRepository;
    @Autowired private CityRepository cityRepository;

    @MockitoBean
    private JavaMailSender mailSender;     // fake SMTP: nothing leaves the machine

    @Test
    @DisplayName("TC-I02 register -> mail -> login(temp) -> reset -> login(new) -> old pwd rejected")
    void fullUserJourney() throws Exception {
        // ---------- Arrange: real ids from Flyway seed data + fake mail server ----------
        Country india = countryRepository.findAll().stream()
                .filter(c -> c.getCountryName().equals("India")).findFirst().orElseThrow();
        State karnataka = stateRepository.findByCountryCountryId(india.getCountryId()).stream()
                .filter(s -> s.getStateName().equals("Karnataka")).findFirst().orElseThrow();
        City bengaluru = cityRepository.findByStateStateId(karnataka.getStateId()).stream()
                .filter(c -> c.getCityName().equals("Bengaluru")).findFirst().orElseThrow();

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        // ---------- 1. Register ----------
        String registerJson = """
                {"name":"Flow Test","email":"%s","phno":9876543210,
                 "countryId":%d,"stateId":%d,"cityId":%d}
                """.formatted(EMAIL, india.getCountryId(), karnataka.getStateId(), bengaluru.getCityId());

        mockMvc.perform(post("/user").contentType(MediaType.APPLICATION_JSON).content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("SUCCESS"));

        // ---------- 2. "Open the mailbox": read the temp password from the sent mail ----------
        ArgumentCaptor<MimeMessage> mail = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().getSubject()).contains("Account Created");
        String tempPwd = extractTempPassword(mail.getValue().getContent().toString());

        // ---------- 3. Email is now taken ----------
        mockMvc.perform(get("/unique/" + EMAIL))
                .andExpect(jsonPath("$.data").value("DUPLICATE"));

        // ---------- 4. Login with temp password -> must reset ----------
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(tempPwd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pwdUpdated").value("NO"))
                .andExpect(jsonPath("$.data.name").value("Flow Test"))
                .andExpect(jsonPath("$.data.phno").value(9876543210L))
                .andExpect(jsonPath("$.data.pwd").doesNotExist());

        // ---------- 5. Reset password ----------
        String resetJson = """
                {"email":"%s","oldPwd":"%s","newPwd":"%s","confirmPwd":"%s"}
                """.formatted(EMAIL, tempPwd, NEW_PWD, NEW_PWD);

        mockMvc.perform(post("/resetpwd").contentType(MediaType.APPLICATION_JSON).content(resetJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("SUCCESS"));

        // ---------- 6. Login with new password -> normal user now ----------
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(NEW_PWD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pwdUpdated").value("YES"));

        // ---------- 7. Old temp password no longer works ----------
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(tempPwd)))
                .andExpect(jsonPath("$.message").value("User Login Failed"));
    }

    // ---------- helpers ----------
    private static String loginJson(String pwd) {
        return """
                {"email":"%s","pwd":"%s"}
                """.formatted(EMAIL, pwd);
    }

    /** Mail body looks like: <h2> Your Temporary Pwd :X7K2P</h2> */
    private static String extractTempPassword(String body) {
        Matcher m = Pattern.compile("Pwd\\s*:\\s*([A-Z1-9]{5})").matcher(body);
        assertThat(m.find()).as("temp password in mail body: %s", body).isTrue();
        return m.group(1);
    }
}
