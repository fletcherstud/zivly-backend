package com.zivly.edge;

import com.zivly.edge.model.request.LoginRequest;
import com.zivly.edge.model.request.UserRequest;
import com.zivly.edge.model.response.UserCreateResponse;
import com.zivly.edge.model.response.UserResponse;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class UserFT extends BaseTest {

    @Test
    void canCreateUser() {
        UserRequest request = UserRequest.builder()
                .firstName("John")
                .lastName("Fletcher")
                .email("johnfletcher831@gmail.com")
                .password("password")
                .birthDate(LocalDate.now().minusYears(20))
                .build();

        given()
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
                .body("tokenResponse.refreshToken", notNullValue());
    }

    @Test
    void canLogin() {
        UserCreateResponse user = createUser();

        LoginRequest loginRequest = LoginRequest.builder()
                .email(user.getUserResponse().getEmail())
                .password("password")
                .build();

        given()
                .body(loginRequest)
                .contentType(ContentType.JSON)
            .when()
                .post("/auth/login")
            .then()
                .statusCode(HttpStatus.SC_OK)
                .body("userResponse.id", notNullValue())
                .body("userResponse.firstName", equalTo(user.getUserResponse().getFirstName()))
                .body("userResponse.lastName", equalTo(user.getUserResponse().getLastName()))
                .body("userResponse.email", equalTo(user.getUserResponse().getEmail()))
                .body("userResponse.birthdate", equalTo(user.getUserResponse().getBirthdate().toString()))
                .body("tokenResponse.accessToken", notNullValue())
                .body("tokenResponse.refreshToken", notNullValue());
    }

    @Test
    void canGetUser() {
        UserCreateResponse user = createUser();
        addAuth(user.getUserResponse().getId());

        given()
                .pathParams("userId", user.getUserResponse().getId())
        .get("/users/{userId}")
        .then()
                .statusCode(HttpStatus.SC_OK)
                .body("id", equalTo(user.getUserResponse().getId().toString()))
                .body("firstName", equalTo(user.getUserResponse().getFirstName()))
                .body("lastName", equalTo(user.getUserResponse().getLastName()))
                .body("email", equalTo(user.getUserResponse().getEmail()));
    }

    @Test
    void cannotGetUserThatDoesNotExist() {
        UserCreateResponse user = createUser();
        addAuth(user.getUserResponse().getId());

        given()
                .pathParams("userId", UUID.randomUUID())
                .get("/users/{userId}")
        .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    void cannotUpdateOtherUser() {
        addAuth(UUID.randomUUID());
        given()
                .pathParams("userId", userId)
        .put("/users/{userId}")
        .then()
            .statusCode(HttpStatus.SC_FORBIDDEN);
    }
}
