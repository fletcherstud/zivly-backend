package com.zivly.edge;

import com.zivly.edge.model.entity.User;
import com.zivly.edge.model.request.UserRequest;
import com.zivly.edge.model.response.UserCreateResponse;
import com.zivly.edge.repository.UserRepository;
import com.zivly.edge.security.JwtUtil;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class BaseTest {

    @LocalServerPort
    private int serverPort;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtUtil jwtUtil;

    protected UUID userId;

    @BeforeEach
    void setUp() {
        RestAssured.port = serverPort;
        RestAssured.baseURI = "http://localhost";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        userId = createUser().getUserResponse().getId();
    }

    protected void addAuth(UUID userId) {
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()) {
            User userAuth = new User();
            userAuth.setId(userId);
            userAuth.setPassword(passwordEncoder.encode("user"));
            userAuth.setEmail("newUser@test.com");
            userRepository.save(userAuth);
            addAuth(userAuth);
        } else {
            addAuth(user.get());
        }

    }

    protected void addAuth(User user) {
        RestAssured.replaceFiltersWith(Collections.emptyList());
        RestAssured.filters(Arrays.asList(new Filter[] {
                (paramFilterableRequestSpecification, paramFilterableResponseSpecification, paramFilterContext) -> {
                    String token = jwtUtil.generateAccessToken(user);
                    if (!paramFilterableRequestSpecification.getHeaders().hasHeaderWithName("Authorization")) {
                        paramFilterableRequestSpecification.header("Authorization", String.format("Bearer %s", token));
                    }
                    return paramFilterContext.next(paramFilterableRequestSpecification, paramFilterableResponseSpecification);
                }
        }));
    }

    protected UserCreateResponse createUser() {
        UserRequest request = UserRequest.builder()
                .firstName("John")
                .lastName("Fletcher")
                .email("johnfletcher831@gmail.com")
                .password("password")
                .birthDate(LocalDate.now().minusYears(20))
                .build();

        return given()
                .body(request)
                .contentType(ContentType.JSON)
        .when()
                .post("/auth/register")
        .then()
                .statusCode(HttpStatus.SC_OK)
                .body("userResponse.id", notNullValue())
                .body("userResponse.firstName", equalTo(request.getFirstName()))
                .body("userResponse.lastName", equalTo(request.getLastName()))
                .body("userResponse.email", equalTo(request.getEmail()))
                .body("userResponse.birthdate", equalTo(request.getBirthDate().toString()))
                .body("tokenResponse.accessToken", notNullValue())
                .body("tokenResponse.refreshToken", notNullValue())
                .extract().as(UserCreateResponse.class);
    }
}
