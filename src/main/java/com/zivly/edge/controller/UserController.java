package com.zivly.edge.controller;

import com.zivly.edge.model.request.UserRequest;
import com.zivly.edge.model.response.UserResponse;
import com.zivly.edge.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(path = "/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping(path = "/{userId}")
    ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        log.info("Attempting to get user id: {}", userId);

        return ResponseEntity.ok(userService.getUser(userId));
    }
}
