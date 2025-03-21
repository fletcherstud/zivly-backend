package com.zivly.edge.authorization;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("(hasRole('ROLE_USER') and #request.userId == authentication.principal.id) or hasRole('ROLE_ADMIN')")
public @interface IsUserRequest {}
