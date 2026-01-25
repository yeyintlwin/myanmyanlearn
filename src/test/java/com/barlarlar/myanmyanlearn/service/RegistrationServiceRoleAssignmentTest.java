package com.barlarlar.myanmyanlearn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barlarlar.myanmyanlearn.entity.Member;
import com.barlarlar.myanmyanlearn.entity.Role;
import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import com.barlarlar.myanmyanlearn.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceRoleAssignmentTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void registerUser_assignsAdminRole_whenFirstUser() {
        when(memberRepository.existsByUserId(eq("user1"))).thenReturn(false);
        when(memberRepository.existsByEmail(eq("user1@example.com"))).thenReturn(false);
        when(memberRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(eq("pw"))).thenReturn("encoded");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(otpService.generateOtp()).thenReturn("123456");

        registrationService.registerUser("user1", "pw", "First", "User", "user1@example.com");

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(roleCaptor.capture());
        assertEquals("ROLE_ADMIN", roleCaptor.getValue().getRole());
        assertEquals("user1", roleCaptor.getValue().getUserId());
        verify(emailService).sendOtpEmail("user1@example.com", "123456");
    }

    @Test
    void registerUser_assignsStudentRole_whenNotFirstUser() {
        when(memberRepository.existsByUserId(eq("user2"))).thenReturn(false);
        when(memberRepository.existsByEmail(eq("user2@example.com"))).thenReturn(false);
        when(memberRepository.count()).thenReturn(1L);
        when(passwordEncoder.encode(eq("pw"))).thenReturn("encoded");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(otpService.generateOtp()).thenReturn("654321");

        registrationService.registerUser("user2", "pw", "Second", "User", "user2@example.com");

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(roleCaptor.capture());
        assertEquals("ROLE_STUDENT", roleCaptor.getValue().getRole());
        assertEquals("user2", roleCaptor.getValue().getUserId());
        verify(emailService).sendOtpEmail("user2@example.com", "654321");
    }
}

