package com.zivly.edge;

import com.zivly.edge.model.request.UserRequest;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class UserFT extends BaseTest {

    @Test
    void canCreateUser() {
        UserRequest request = UserRequest.builder()
                .firstName("John")
                .lastName("Fletcher")
                .email("johnfletcher831@gmail.com")
                .password("Mike1958")
                .birthDate(LocalDate.now().minusYears(20))
                .build();

        given()
                .body(request)
                .contentType(ContentType.JSON)
        .when()
                .post("/auth/register")
        .then()
                .statusCode(HttpStatus.SC_OK)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }
}
