package com.barlarlar.myanmyanlearn.controller;

import com.barlarlar.myanmyanlearn.entity.Member;
import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerChangePasswordTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ProfileController profileController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void changePassword_returns401_whenAnonymous() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("anonymousUser", "N/A"));

        Map<String, String> request = new HashMap<>();
        request.put("oldPassword", "old");
        request.put("newPassword", "new-password-123");

        ResponseEntity<Map<String, Object>> response = profileController.changePassword(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("success"));
    }

    @Test
    void changePassword_returnsBadRequest_whenOldPasswordIncorrect() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user1", "N/A"));

        Member member = new Member("user1", "$2a$10$hash", true);
        when(memberRepository.findByUserIdAndActive(eq("user1"), eq(true))).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(eq("wrong-old"), any())).thenReturn(false);

        Map<String, String> request = new HashMap<>();
        request.put("oldPassword", "wrong-old");
        request.put("newPassword", "new-password-123");

        ResponseEntity<Map<String, Object>> response = profileController.changePassword(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("success"));
    }

    @Test
    void changePassword_updatesPassword_whenValid() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user1", "N/A"));

        Member member = new Member("user1", "$2a$10$hash", true);
        when(memberRepository.findByUserIdAndActive(eq("user1"), eq(true))).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(eq("old"), eq("$2a$10$hash"))).thenReturn(true);
        when(passwordEncoder.matches(eq("new-password-123"), eq("$2a$10$hash"))).thenReturn(false);
        when(passwordEncoder.encode(eq("new-password-123"))).thenReturn("$2a$10$newHash");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> request = new HashMap<>();
        request.put("oldPassword", "old");
        request.put("newPassword", "new-password-123");

        ResponseEntity<Map<String, Object>> response = profileController.changePassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertTrue(member.getPassword().startsWith("$2a$10$"));
        verify(memberRepository).save(any(Member.class));
    }
}

