package com.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")          // loads application-test.properties on top of main
class UserHubApplicationTests {

    @Test
    void contextLoads() {
        // TC-I01: passes if the whole app starts:
        // Flyway migrates userhub_test to V6, Hibernate validate passes,
        // all beans (controller, service, repos, ModelMapper, mail) are created.
    }

}
