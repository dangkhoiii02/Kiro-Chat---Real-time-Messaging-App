/*
 * Copyright 2026 trung-kieen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kiro.kiro_backend.common.websocket.infrastructure.primary.security;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.kiro.kiro_backend.common.authentication.domain.KeycloakPrincipal;
import com.kiro.kiro_backend.common.authentication.infrastructure.primary.keycloak.KeycloakTokenVerifier;
import com.kiro.kiro_backend.common.user.domain.aggregate.Authority;
import com.kiro.kiro_backend.common.user.domain.aggregate.User;
import com.kiro.kiro_backend.common.user.domain.aggregate.UserBuilder;
import com.kiro.kiro_backend.common.user.domain.service.UserSynchronizeService;
import com.kiro.kiro_backend.common.user.domain.vo.AuthorityName;
import com.kiro.kiro_backend.common.user.domain.vo.PublicId;
import com.kiro.kiro_backend.common.user.domain.vo.UserEmail;
import com.kiro.kiro_backend.common.user.domain.vo.UserFirstname;
import com.kiro.kiro_backend.common.user.domain.vo.UserLastname;
import com.kiro.kiro_backend.common.user.infrastructure.secondary.entity.UserEntity;
import com.kiro.kiro_backend.common.user.infrastructure.secondary.repository.JpaUserRepository;
import com.kiro.kiro_backend.common.websocket.application.WebSocketTokenValicationException;
import com.kiro.kiro_backend.common.websocket.domain.aggregate.JWSAuthentication;
import com.kiro.kiro_backend.common.websocket.domain.vo.BearerToken;

import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Use keycloak to authenticate user token in websocket connection
 */
@Slf4j
// @Component
// @Qualifier("websocket")
@RequiredArgsConstructor
public class WebSocketAuthenticationManager implements AuthenticationManager {

  private final KeycloakTokenVerifier tokenVerifier;
  private final JpaUserRepository userRepository;
  private final UserSynchronizeService userSynchronizeService;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    log.info("Authentication websocket connection");

    JWSAuthentication token = (JWSAuthentication) authentication;
    String tokenString = (String) token.getCredentials();
    try {
      AccessToken accessToken = tokenVerifier.verifyToken(tokenString);
      List<GrantedAuthority> authorities = accessToken.getRealmAccess().getRoles().stream()
          .map(SimpleGrantedAuthority::new).collect(Collectors.toList());
      KeycloakPrincipal identityAccess = KeycloakPrincipal.fromKeycloakAccessToken(accessToken);
      UserEntity user = findOrSyncUser(accessToken, identityAccess);
      PublicId userPublicId = new PublicId(user.getPublicId());
      token = new JWSAuthentication(new BearerToken(tokenString), identityAccess, authorities, userPublicId);

      token.setAuthenticated(true);
    } catch (VerificationException e) {
      log.debug("Exception authenticating the token {}:", tokenString, e);
      throw new WebSocketTokenValicationException();
    }
    return token;
  }

  private UserEntity findOrSyncUser(AccessToken accessToken, KeycloakPrincipal identityAccess) {
    return userRepository.findByUsername(identityAccess.getUsername().value())
        .or(() -> userRepository.findByEmail(identityAccess.getUserEmail().value()))
        .orElseGet(() -> {
          userSynchronizeService.syncUser(toDomainUser(accessToken));
          return userRepository.findByUsername(identityAccess.getUsername().value())
              .or(() -> userRepository.findByEmail(identityAccess.getUserEmail().value()))
              .orElseThrow(EntityNotFoundException::new);
        });
  }

  private User toDomainUser(AccessToken accessToken) {
    String username = fallback(accessToken.getPreferredUsername(), accessToken.getEmail(), "user");
    String email = fallback(accessToken.getEmail(), username + "@kiro.local");
    String firstName = fallback(accessToken.getGivenName(), accessToken.getName(), username);
    String lastName = fallback(accessToken.getFamilyName(), username);
    Set<Authority> authorities = accessToken.getRealmAccess().getRoles().stream()
        .map(role -> Authority.builder().name(new AuthorityName("ROLE_" + role)).build())
        .collect(Collectors.toSet());

    return UserBuilder.user()
        .userPublicId(null)
        .dbId(null)
        .email(new UserEmail(email))
        .firstname(new UserFirstname(firstName))
        .lastname(new UserLastname(lastName))
        .username(new com.kiro.kiro_backend.common.authentication.domain.Username(username))
        .profilePicture(null)
        .createdDate(null)
        .lastModifiedDate(Instant.now())
        .bio(null)
        .lastActive(Instant.now())
        .authorities(authorities)
        .build();
  }

  private String fallback(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }

}
