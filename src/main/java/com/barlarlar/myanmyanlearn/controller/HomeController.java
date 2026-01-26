package com.barlarlar.myanmyanlearn.controller;

import com.barlarlar.myanmyanlearn.entity.Member;
import com.barlarlar.myanmyanlearn.entity.Role;
import com.barlarlar.myanmyanlearn.repository.AssessmentScoreRecordRepository;
import com.barlarlar.myanmyanlearn.repository.MemberRepository;
import com.barlarlar.myanmyanlearn.repository.OtpVerificationRepository;
import com.barlarlar.myanmyanlearn.repository.PasswordResetTokenRepository;
import com.barlarlar.myanmyanlearn.repository.RoleRepository;
import com.barlarlar.myanmyanlearn.model.Content;
import com.barlarlar.myanmyanlearn.model.Course;
import com.barlarlar.myanmyanlearn.service.AssessmentScoreRecordService;
import com.barlarlar.myanmyanlearn.service.CourseService;
import com.barlarlar.myanmyanlearn.service.LoginAttemptService;
import com.barlarlar.myanmyanlearn.service.RegistrationSettingsService;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class HomeController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private AssessmentScoreRecordService scoreRecordService;

    @Autowired
    private AssessmentScoreRecordRepository assessmentScoreRecordRepository;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private UserDetailsManager userDetailsManager;

    @Autowired
    private RegistrationSettingsService registrationSettingsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private volatile boolean memberStudentMetaInitialized;
    private volatile boolean memberStudentMetaAvailable;

    private static final Set<String> ALLOWED_USER_ROLES = Set.of("ADMIN", "TEACHER", "STUDENT");
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_TEACHER = "ROLE_TEACHER";

    @GetMapping("/home")
    public String homePage(Model model) {
        System.out.println("=== HomeController.homePage() called ===");

        // Get authenticated user information
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Authentication: " + (authentication != null ? authentication.getName() : "null"));
        System.out.println("Is authenticated: " + (authentication != null && authentication.isAuthenticated()));
        System.out.println(
                "Is anonymous: " + (authentication != null && authentication.getName().equals("anonymousUser")));

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {

            String username = authentication.getName();
            System.out.println("Username: " + username);
            model.addAttribute("username", username);
            model.addAttribute("userInitials", getInitials(username));

            // Fetch user's full data from database
            Optional<Member> memberOpt = memberRepository.findById(Objects.requireNonNull(username));
            System.out.println("Fetching user data for: " + username);
            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();
                System.out.println("User found: " + member.getFirstName() + " " + member.getLastName() + " - "
                        + member.getEmail());
                model.addAttribute("userFirstName", member.getFirstName());
                model.addAttribute("userLastName", member.getLastName());
                model.addAttribute("userEmail", member.getEmail());
                model.addAttribute("userFullName", getFullName(member.getFirstName(), member.getLastName()));
                model.addAttribute("userInitials", getInitialsFromName(member.getFirstName(), member.getLastName()));
            } else {
                System.out.println("User not found in database: " + username);
            }
        } else {
            System.out.println("User not authenticated or is anonymous");
        }

        // Add courses from JSON data model
        List<Course> courses = courseService.getAllCourses();
        model.addAttribute("courses", courses);

        Map<String, Integer> courseProgress = new HashMap<>();
        for (Course course : courses) {
            if (course == null || course.getId() == null) {
                continue;
            }
            Optional<JsonNode> scoreJsonOpt = scoreRecordService
                    .latestScoreJsonForCurrentUser(Optional.ofNullable(course.getId()));
            Map<Integer, Integer> chapterProgress = scoreJsonOpt.isPresent()
                    ? computeChapterProgress(scoreJsonOpt.get())
                    : Map.of();
            int coursePercent = computeCourseProgress(course, chapterProgress);
            courseProgress.put(course.getId(), coursePercent);
        }
        model.addAttribute("courseProgress", courseProgress);

        return "home";
    }

    @GetMapping("/admin")
    public String adminPanelPage(
            @RequestParam(name = "section", required = false) String section,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "field", required = false) String field,
            @RequestParam(name = "role", required = false) String role,
            Model model) {
        String effectiveSection = (section == null || section.isBlank()) ? "courses" : section;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if ("users".equals(effectiveSection) && isAdmin(auth)) {
            model.addAttribute("currentUserId", auth != null ? auth.getName() : null);
            String query = q != null ? q.trim() : "";
            String fieldKey = (field == null || field.isBlank()) ? "name" : field.trim().toLowerCase(Locale.ROOT);
            String roleKey = role != null ? role.trim() : "";
            String roleKeyNorm = roleKey.isBlank() ? "" : roleKey.toUpperCase(Locale.ROOT);
            model.addAttribute("userQ", query);
            model.addAttribute("userField", fieldKey);
            model.addAttribute("userRole", roleKeyNorm);

            List<Member> members = memberRepository.findAll();
            List<String> userIds = new ArrayList<>();
            for (Member m : members) {
                if (m != null && m.getUserId() != null) {
                    userIds.add(m.getUserId());
                }
            }

            Map<String, List<String>> rolesByUser = new HashMap<>();
            if (!userIds.isEmpty()) {
                List<Role> roles = roleRepository.findByUserIdIn(userIds);
                for (Role r : roles) {
                    if (r == null || r.getUserId() == null || r.getRole() == null) {
                        continue;
                    }
                    rolesByUser.computeIfAbsent(r.getUserId(), k -> new ArrayList<>()).add(r.getRole());
                }
                for (List<String> rs : rolesByUser.values()) {
                    rs.sort(String::compareTo);
                }
            }

            members.sort((a, b) -> {
                if (a == null && b == null) {
                    return 0;
                }
                if (a == null) {
                    return 1;
                }
                if (b == null) {
                    return -1;
                }
                String aId = a.getUserId() != null ? a.getUserId() : "";
                String bId = b.getUserId() != null ? b.getUserId() : "";

                int aRank = roleRankFromKey(resolvePrimaryRoleKey(rolesByUser.get(aId)));
                int bRank = roleRankFromKey(resolvePrimaryRoleKey(rolesByUser.get(bId)));
                if (aRank != bRank) {
                    return Integer.compare(aRank, bRank);
                }
                return aId.compareToIgnoreCase(bId);
            });

            Map<String, StudentMeta> studentMetaByUser = loadStudentMetaByUserId(userIds);

            List<AdminUserRow> rows = new ArrayList<>();
            for (Member m : members) {
                if (m == null) {
                    continue;
                }
                if (!roleKeyNorm.isBlank()) {
                    List<String> rs = rolesByUser.get(m.getUserId());
                    if (!matchesRoleFilter(rs, roleKeyNorm)) {
                        continue;
                    }
                }

                if (!query.isBlank() && !matchesUserSearch(m, fieldKey, query)) {
                    continue;
                }

                List<String> rs = rolesByUser.get(m.getUserId());
                String rolesDisplay = (rs == null || rs.isEmpty()) ? "-" : String.join(", ", rs);
                String primaryRoleKey = resolvePrimaryRoleKey(rs);
                StudentMeta meta = studentMetaByUser.get(m.getUserId());
                rows.add(new AdminUserRow(
                        m.getUserId(),
                        m.getFirstName(),
                        m.getLastName(),
                        m.getEmail(),
                        m.getActive(),
                        m.getEmailVerified(),
                        rolesDisplay,
                        primaryRoleKey,
                        m.getProfileImage(),
                        meta != null ? meta.currentClass : null,
                        meta != null ? meta.schoolYear : null));
            }
            model.addAttribute("adminUsers", rows);
        }

        if ("settings".equals(effectiveSection) && isAdmin(auth)) {
            RegistrationSettingsService.SettingsView settings = registrationSettingsService.getSettings();
            model.addAttribute("registrationDomainEnforced", settings.isEnforceDomain());
            model.addAttribute("registrationDomainDisplay", registrationSettingsService.getDisplayDomain());
        }

        model.addAttribute("section", effectiveSection);
        return "admin-panel";
    }

    @PostMapping("/admin/settings/registration-domain")
    @Transactional
    public String updateRegistrationDomainSettings(
            @RequestParam(name = "allowedDomain", required = false) String allowedDomain,
            @RequestParam(name = "enforceDomain", required = false) Boolean enforceDomain) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            return "redirect:/admin?section=settings";
        }

        boolean enforce = enforceDomain != null && enforceDomain;
        registrationSettingsService.updateSettings(allowedDomain, enforce);

        return "redirect:/admin?section=settings";
    }

    private int roleRankFromKey(String roleKey) {
        if ("ADMIN".equals(roleKey)) {
            return 0;
        }
        if ("TEACHER".equals(roleKey)) {
            return 1;
        }
        if ("STUDENT".equals(roleKey)) {
            return 2;
        }
        return 3;
    }

    @PostMapping("/admin/users/role")
    @Transactional
    public String adminUpdateUserRole(
            @RequestParam("userId") String userId,
            @RequestParam("role") String role,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "field", required = false) String field,
            @RequestParam(name = "roleFilter", required = false) String roleFilter,
            HttpServletRequest request,
            HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            return "redirect:/admin?section=users";
        }

        String targetUserId = userId != null ? userId.trim() : "";
        String wantedKey = role != null ? role.trim().toUpperCase(Locale.ROOT) : "";
        if (targetUserId.isBlank() || wantedKey.isBlank() || !ALLOWED_USER_ROLES.contains(wantedKey)) {
            return redirectAdminUsers(q, field, roleFilter);
        }

        String currentUserId = auth != null ? auth.getName() : null;
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            return redirectAdminUsers(q, field, roleFilter);
        }

        Optional<Member> memberOpt = memberRepository.findById(targetUserId);
        if (memberOpt.isEmpty()) {
            return redirectAdminUsers(q, field, roleFilter);
        }

        if ("ADMIN".equals(wantedKey)) {
            if (currentUserId == null || currentUserId.isBlank()) {
                return redirectAdminUsers(q, field, roleFilter);
            }

            roleRepository.deleteByUserId(targetUserId);
            roleRepository.save(new Role(targetUserId, ROLE_ADMIN));

            roleRepository.deleteByUserId(currentUserId);
            roleRepository.save(new Role(currentUserId, ROLE_TEACHER));

            refreshCurrentAuthentication(currentUserId, auth);
            new SecurityContextLogoutHandler().logout(request, response, auth);
            return "redirect:/login?logout=true";
        }

        boolean targetIsAdmin = roleRepository.existsByUserIdAndRole(targetUserId, ROLE_ADMIN);
        if (targetIsAdmin) {
            long adminCount = roleRepository.countByRole(ROLE_ADMIN);
            if (adminCount <= 1) {
                return redirectAdminUsers(q, field, roleFilter);
            }
        }

        String roleValue = "ROLE_" + wantedKey;
        roleRepository.deleteByUserId(targetUserId);
        roleRepository.save(new Role(targetUserId, roleValue));

        return redirectAdminUsers(q, field, roleFilter);
    }

    @PostMapping("/admin/users/delete")
    @Transactional
    public String adminDeleteUser(
            @RequestParam("userId") String userId,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "field", required = false) String field,
            @RequestParam(name = "roleFilter", required = false) String roleFilter) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            return "redirect:/admin?section=users";
        }

        String targetUserId = userId != null ? userId.trim() : "";
        if (targetUserId.isBlank()) {
            return redirectAdminUsers(q, field, roleFilter);
        }

        String currentUserId = auth != null ? auth.getName() : null;
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            return redirectAdminUsers(q, field, roleFilter);
        }

        Optional<Member> memberOpt = memberRepository.findById(targetUserId);
        if (memberOpt.isEmpty()) {
            return redirectAdminUsers(q, field, roleFilter);
        }

        boolean targetIsAdmin = roleRepository.existsByUserIdAndRole(targetUserId, ROLE_ADMIN);
        if (targetIsAdmin) {
            long adminCount = roleRepository.countByRole(ROLE_ADMIN);
            if (adminCount <= 1) {
                return redirectAdminUsers(q, field, roleFilter);
            }
        }

        Member member = memberOpt.get();
        String email = member.getEmail();

        assessmentScoreRecordRepository.deleteByUserId(targetUserId);
        roleRepository.deleteByUserId(targetUserId);
        otpVerificationRepository.deleteByUserId(targetUserId);
        if (email != null && !email.isBlank()) {
            passwordResetTokenRepository.deleteByEmail(email);
            otpVerificationRepository.deleteByEmail(email);
        }
        loginAttemptService.deleteAttemptsForUser(targetUserId);

        memberRepository.delete(member);

        return redirectAdminUsers(q, field, roleFilter);
    }

    @PostMapping("/admin/users/active")
    @Transactional
    public Object adminUpdateUserActive(
            @RequestParam("userId") String userId,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "field", required = false) String field,
            @RequestParam(name = "roleFilter", required = false) String roleFilter,
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            if (isAjax(request)) {
                return org.springframework.http.ResponseEntity.status(403).build();
            }
            return "redirect:/admin?section=users";
        }

        String targetUserId = userId != null ? userId.trim() : "";
        if (targetUserId.isBlank()) {
            if (isAjax(request)) {
                return org.springframework.http.ResponseEntity.badRequest().build();
            }
            return redirectAdminUsers(q, field, roleFilter);
        }

        Optional<Member> memberOpt = memberRepository.findById(targetUserId);
        if (memberOpt.isEmpty()) {
            if (isAjax(request)) {
                return org.springframework.http.ResponseEntity.notFound().build();
            }
            return redirectAdminUsers(q, field, roleFilter);
        }

        boolean newActive = active != null && active;

        boolean targetIsAdmin = roleRepository.existsByUserIdAndRole(targetUserId, ROLE_ADMIN);
        if (targetIsAdmin && !newActive) {
            if (isAjax(request)) {
                return org.springframework.http.ResponseEntity.status(409).build();
            }
            return redirectAdminUsers(q, field, roleFilter);
        }

        Member member = memberOpt.get();
        member.setActive(newActive);
        memberRepository.save(member);

        if (isAjax(request)) {
            return org.springframework.http.ResponseEntity.noContent().build();
        }
        return redirectAdminUsers(q, field, roleFilter);
    }

    @PostMapping("/admin/users/student-meta")
    @Transactional
    public Object adminUpdateStudentMeta(
            @RequestParam("userId") String userId,
            @RequestParam(name = "currentClass", required = false) String currentClass,
            @RequestParam(name = "schoolYear", required = false) String schoolYear,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "field", required = false) String field,
            @RequestParam(name = "roleFilter", required = false) String roleFilter,
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            if (isAjax(request)) {
                return org.springframework.http.ResponseEntity.status(403).build();
            }
            return "redirect:/admin?section=users";
        }

        String targetUserId = userId != null ? userId.trim() : "";
        if (targetUserId.isBlank()) {
            if (isAjax(request)) {
                return org.springframework.http.ResponseEntity.badRequest().build();
            }
            return redirectAdminUsers(q, field, roleFilter);
        }

        String currentUserId = auth != null ? auth.getName() : null;
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            if (isAjax(request)) {
                return org.springframework.http.ResponseEntity.status(403).build();
            }
            return redirectAdminUsers(q, field, roleFilter);
        }

        boolean targetIsAdmin = roleRepository.existsByUserIdAndRole(targetUserId, ROLE_ADMIN);
        if (targetIsAdmin) {
            if (isAjax(request)) {
                return org.springframework.http.ResponseEntity.status(403).build();
            }
            return redirectAdminUsers(q, field, roleFilter);
        }
        boolean targetIsTeacher = roleRepository.existsByUserIdAndRole(targetUserId, ROLE_TEACHER);
        if (targetIsTeacher) {
            if (isAjax(request)) {
                return org.springframework.http.ResponseEntity.status(403).build();
            }
            return redirectAdminUsers(q, field, roleFilter);
        }

        ensureMemberStudentMetaColumns();
        if (!memberStudentMetaAvailable) {
            if (isAjax(request)) {
                return org.springframework.http.ResponseEntity.status(500).build();
            }
            return redirectAdminUsers(q, field, roleFilter);
        }

        boolean hasCurrentClassParam = request != null
                && request.getParameterMap() != null
                && request.getParameterMap().containsKey("currentClass");
        boolean hasSchoolYearParam = request != null
                && request.getParameterMap() != null
                && request.getParameterMap().containsKey("schoolYear");

        if (hasCurrentClassParam && hasSchoolYearParam) {
            String cls = normalizeNullable(currentClass, 100);
            String year = normalizeNullable(schoolYear, 20);
            jdbcTemplate.update(
                    "UPDATE members SET current_class = ?, school_year = ? WHERE user_id = ?",
                    cls,
                    year,
                    targetUserId);
        } else if (hasCurrentClassParam) {
            String cls = normalizeNullable(currentClass, 100);
            jdbcTemplate.update(
                    "UPDATE members SET current_class = ? WHERE user_id = ?",
                    cls,
                    targetUserId);
        } else if (hasSchoolYearParam) {
            String year = normalizeNullable(schoolYear, 20);
            jdbcTemplate.update(
                    "UPDATE members SET school_year = ? WHERE user_id = ?",
                    year,
                    targetUserId);
        }

        if (isAjax(request)) {
            return org.springframework.http.ResponseEntity.noContent().build();
        }
        return redirectAdminUsers(q, field, roleFilter);
    }

    private boolean isAjax(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String xrw = request.getHeader("X-Requested-With");
        if (xrw != null && "XMLHttpRequest".equalsIgnoreCase(xrw.trim())) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private String normalizeNullable(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLen) {
            return trimmed.substring(0, maxLen);
        }
        return trimmed;
    }

    private boolean matchesUserSearch(Member m, String fieldKey, String query) {
        if (m == null) {
            return false;
        }
        String q = query.toLowerCase(Locale.ROOT);

        if ("username".equals(fieldKey)) {
            return containsIgnoreCase(m.getUserId(), q);
        }
        if ("email".equals(fieldKey)) {
            return containsIgnoreCase(m.getEmail(), q);
        }

        String first = m.getFirstName() != null ? m.getFirstName() : "";
        String last = m.getLastName() != null ? m.getLastName() : "";
        String full = (first + " " + last).trim();
        return containsIgnoreCase(full, q) || containsIgnoreCase(first, q) || containsIgnoreCase(last, q);
    }

    private boolean matchesRoleFilter(List<String> roles, String roleKey) {
        if (roleKey == null || roleKey.isBlank()) {
            return true;
        }
        String wanted = roleKey.trim().toUpperCase(Locale.ROOT);
        String wantedRole = wanted.startsWith("ROLE_") ? wanted : "ROLE_" + wanted;
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (String r : roles) {
            if (r == null) {
                continue;
            }
            String rr = r.trim().toUpperCase(Locale.ROOT);
            if (rr.equals(wanted) || rr.equals(wantedRole)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String value, String loweredQuery) {
        if (value == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(loweredQuery);
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            String r = a != null ? a.getAuthority() : null;
            if ("ROLE_ADMIN".equals(r)) {
                return true;
            }
        }
        return false;
    }

    private void refreshCurrentAuthentication(String userId, Authentication currentAuth) {
        if (userId == null || userId.isBlank() || currentAuth == null) {
            return;
        }
        try {
            UserDetails userDetails = userDetailsManager.loadUserByUsername(userId);
            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    currentAuth.getCredentials(),
                    userDetails.getAuthorities());
            if (currentAuth instanceof AbstractAuthenticationToken) {
                newAuth.setDetails(((AbstractAuthenticationToken) currentAuth).getDetails());
            }
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        } catch (Exception e) {
        }
    }

    private String resolvePrimaryRoleKey(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }

        boolean admin = false;
        boolean teacher = false;
        boolean student = false;
        for (String r : roles) {
            if (r == null) {
                continue;
            }
            String rr = r.trim().toUpperCase(Locale.ROOT);
            if ("ROLE_ADMIN".equals(rr) || "ADMIN".equals(rr)) {
                admin = true;
            } else if ("ROLE_TEACHER".equals(rr) || "TEACHER".equals(rr)) {
                teacher = true;
            } else if ("ROLE_STUDENT".equals(rr) || "STUDENT".equals(rr)) {
                student = true;
            }
        }
        if (admin) {
            return "ADMIN";
        }
        if (teacher) {
            return "TEACHER";
        }
        if (student) {
            return "STUDENT";
        }
        return "";
    }

    private String redirectAdminUsers(String q, String field, String roleFilter) {
        StringBuilder sb = new StringBuilder("redirect:/admin?section=users");
        if (q != null && !q.isBlank()) {
            sb.append("&q=").append(urlEncode(q.trim()));
        }
        if (field != null && !field.isBlank()) {
            sb.append("&field=").append(urlEncode(field.trim()));
        }
        if (roleFilter != null && !roleFilter.isBlank()) {
            sb.append("&role=").append(urlEncode(roleFilter.trim()));
        }
        return sb.toString();
    }

    private String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private void ensureMemberStudentMetaColumns() {
        if (memberStudentMetaInitialized) {
            return;
        }
        synchronized (this) {
            if (memberStudentMetaInitialized) {
                return;
            }

            boolean available = false;
            try {
                if (!hasColumn("members", "current_class")) {
                    jdbcTemplate.execute("ALTER TABLE members ADD COLUMN current_class VARCHAR(100) NULL");
                }
                if (!hasColumn("members", "school_year")) {
                    jdbcTemplate.execute("ALTER TABLE members ADD COLUMN school_year VARCHAR(20) NULL");
                }
                available = hasColumn("members", "current_class") && hasColumn("members", "school_year");
            } catch (Exception ignored) {
                available = false;
            }

            memberStudentMetaAvailable = available;
            memberStudentMetaInitialized = true;
        }
    }

    private boolean hasColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }

    private Map<String, StudentMeta> loadStudentMetaByUserId(List<String> userIds) {
        ensureMemberStudentMetaColumns();
        if (!memberStudentMetaAvailable || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        try {
            String placeholders = userIds.stream().map(x -> "?").collect(Collectors.joining(","));
            String sql = "SELECT user_id, current_class, school_year FROM members WHERE user_id IN (" + placeholders
                    + ")";
            Object[] args = userIds.toArray();
            Map<String, StudentMeta> out = new HashMap<>();
            jdbcTemplate.query(sql, (rs, rowNum) -> {
                String userId = rs.getString("user_id");
                if (userId != null) {
                    out.put(userId, new StudentMeta(
                            rs.getString("current_class"),
                            rs.getString("school_year")));
                }
                return 1;
            }, args);
            return out;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private static class StudentMeta {
        private final String currentClass;
        private final String schoolYear;

        private StudentMeta(String currentClass, String schoolYear) {
            this.currentClass = currentClass;
            this.schoolYear = schoolYear;
        }
    }

    public static class AdminUserRow {
        private final String userId;
        private final String firstName;
        private final String lastName;
        private final String email;
        private final Boolean active;
        private final Boolean emailVerified;
        private final String roles;
        private final String roleKey;
        private final String profileImage;
        private final String currentClass;
        private final String schoolYear;

        public AdminUserRow(String userId, String firstName, String lastName, String email,
                Boolean active, Boolean emailVerified, String roles, String roleKey, String profileImage,
                String currentClass,
                String schoolYear) {
            this.userId = userId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.active = active;
            this.emailVerified = emailVerified;
            this.roles = roles;
            this.roleKey = roleKey;
            this.profileImage = profileImage;
            this.currentClass = currentClass;
            this.schoolYear = schoolYear;
        }

        public String getUserId() {
            return userId;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getEmail() {
            return email;
        }

        public Boolean getActive() {
            return active;
        }

        public Boolean getEmailVerified() {
            return emailVerified;
        }

        public String getRoles() {
            return roles;
        }

        public String getRoleKey() {
            return roleKey;
        }

        public String getProfileImage() {
            return profileImage;
        }

        public String getCurrentClass() {
            return currentClass;
        }

        public String getSchoolYear() {
            return schoolYear;
        }
    }

    private int computeCourseProgress(Course course, Map<Integer, Integer> chapterProgress) {
        if (course == null || course.getContents() == null || course.getContents().isEmpty()) {
            return 0;
        }
        if (chapterProgress == null) {
            chapterProgress = Map.of();
        }

        int sum = 0;
        int count = 0;
        for (Content content : course.getContents()) {
            if (content == null) {
                continue;
            }
            Integer pct = chapterProgress.get(content.getOrder());
            if (pct == null) {
                pct = 0;
            }
            sum += pct;
            count++;
        }
        int percent = count == 0 ? 0 : (int) Math.round(sum / (double) count);
        if (percent < 0) {
            percent = 0;
        } else if (percent > 100) {
            percent = 100;
        }
        return percent;
    }

    private Map<Integer, Integer> computeChapterProgress(JsonNode scoreJsonRoot) {
        Map<Integer, Integer> out = new HashMap<>();
        if (scoreJsonRoot == null || !scoreJsonRoot.isObject()) {
            return out;
        }
        JsonNode chapters = scoreJsonRoot.get("chapters");
        if (chapters == null || !chapters.isArray()) {
            return out;
        }

        for (JsonNode chapterNode : chapters) {
            if (chapterNode == null || !chapterNode.isObject()) {
                continue;
            }
            JsonNode chapterNoNode = chapterNode.get("chapter_no");
            if (chapterNoNode == null || !chapterNoNode.canConvertToInt()) {
                continue;
            }
            int chapterNo = chapterNoNode.asInt();

            JsonNode questions = chapterNode.get("questions");
            if (questions == null || !questions.isArray()) {
                continue;
            }

            int total = 0;
            int correct = 0;
            for (JsonNode qNode : questions) {
                if (qNode == null || !qNode.isObject()) {
                    continue;
                }
                JsonNode slopes = qNode.get("slopes");
                if (slopes == null || !slopes.isArray()) {
                    continue;
                }
                for (JsonNode slopeNode : slopes) {
                    if (slopeNode == null || !slopeNode.isObject()) {
                        continue;
                    }
                    JsonNode isCorrectNode = slopeNode.get("is_correct");
                    if (isCorrectNode == null || !isCorrectNode.isBoolean()) {
                        continue;
                    }
                    total++;
                    if (isCorrectNode.asBoolean()) {
                        correct++;
                    }
                }
            }

            int percent = total == 0 ? 0 : (int) Math.round((correct * 100.0) / total);
            if (percent < 0) {
                percent = 0;
            } else if (percent > 100) {
                percent = 100;
            }
            out.put(chapterNo, percent);
        }
        return out;
    }

    /**
     * Get user initials from username
     */
    private String getInitials(String username) {
        if (username == null || username.isEmpty()) {
            return "U";
        }

        // If username has multiple words, take first letter of each word
        String[] words = username.split("\\s+");
        if (words.length >= 2) {
            return (words[0].charAt(0) + "" + words[1].charAt(0)).toUpperCase();
        }

        // If single word, take first two characters
        if (username.length() >= 2) {
            return username.substring(0, 2).toUpperCase();
        }

        // If single character, use it twice
        return (username.charAt(0) + "" + username.charAt(0)).toUpperCase();
    }

    /**
     * Get full name from first and last name
     */
    private String getFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return "User";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    /**
     * Get initials from first and last name
     */
    private String getInitialsFromName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return "U";
        }
        if (firstName == null) {
            return lastName.substring(0, 1).toUpperCase();
        }
        if (lastName == null) {
            return firstName.substring(0, 1).toUpperCase();
        }
        return (firstName.substring(0, 1) + lastName.substring(0, 1)).toUpperCase();
    }
}
