package com.zivly.edge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zivly.edge.model.entity.User;
import com.zivly.edge.model.response.UserResponse;
import com.zivly.edge.repository.UserRepository;
import exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;

    public UserResponse getUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Could not find user %s", userId)));

        return objectMapper.convertValue(user, UserResponse.class);
    }
}
