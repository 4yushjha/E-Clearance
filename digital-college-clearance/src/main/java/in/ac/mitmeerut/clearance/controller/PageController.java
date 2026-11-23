package in.ac.mitmeerut.clearance.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.ac.mitmeerut.clearance.entity.User;
import in.ac.mitmeerut.clearance.entity.Student;
import in.ac.mitmeerut.clearance.entity.Staff;
import in.ac.mitmeerut.clearance.entity.ClearanceRequest;
import in.ac.mitmeerut.clearance.entity.ClearanceVerification;
import in.ac.mitmeerut.clearance.entity.ClearanceAuthority;

import in.ac.mitmeerut.clearance.dto.ClearanceVerificationDTO;

import in.ac.mitmeerut.clearance.enums.Role;
import in.ac.mitmeerut.clearance.enums.ClearanceStatus;

import in.ac.mitmeerut.clearance.service.UserService;
import in.ac.mitmeerut.clearance.service.StudentService;
import in.ac.mitmeerut.clearance.service.StaffService;
import in.ac.mitmeerut.clearance.service.ClearanceRequestService;
import in.ac.mitmeerut.clearance.service.ClearanceVerificationService;
import in.ac.mitmeerut.clearance.service.ClearanceAuthorityService;
import in.ac.mitmeerut.clearance.service.DepartmentService;
import in.ac.mitmeerut.clearance.service.CertificateService;

import jakarta.servlet.http.HttpSession;

@Controller
public class PageController {

    private final UserService userService;
    private final StudentService studentService;
    private final StaffService staffService;
    private final ClearanceRequestService clearanceRequestService;
    private final ClearanceVerificationService clearanceVerificationService;
    private final ClearanceAuthorityService clearanceAuthorityService;
    private final DepartmentService departmentService;
    private final CertificateService certificateService;

    public PageController(
            UserService userService,
            StudentService studentService,
            StaffService staffService,
            ClearanceRequestService clearanceRequestService,
            ClearanceVerificationService clearanceVerificationService,
            ClearanceAuthorityService clearanceAuthorityService,
            DepartmentService departmentService,
            CertificateService certificateService) {

        this.userService = userService;
        this.studentService = studentService;
        this.staffService = staffService;
        this.clearanceRequestService = clearanceRequestService;
        this.clearanceVerificationService = clearanceVerificationService;
        this.clearanceAuthorityService = clearanceAuthorityService;
        this.departmentService = departmentService;
        this.certificateService = certificateService;
    }

    // ==========================================
    // LOGIN PAGE
    // ==========================================

    @GetMapping("/login")
    public String loginPage(HttpSession session) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId != null && role != null) {

            if (role.equals("STUDENT")) {
                return "redirect:/student-dashboard";
            }

            if (role.equals("STAFF")) {
                return "redirect:/staff-dashboard";
            }

            if (role.equals("ADMIN")) {
                return "redirect:/admin-dashboard";
            }
        }

        return "login";
    }

    // ==========================================
    // LOGIN AUTHENTICATION
    // ==========================================

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        try {

            User user = userService.getUserByEmail(email);

            if (user == null || !user.getPassword().equals(password)) {

                model.addAttribute(
                        "error",
                        "Invalid email or password."
                );

                return "login";
            }

            session.setAttribute("loggedInUserId", user.getId());
            session.setAttribute("loggedInRole", user.getRole().name());

            if (user.getRole() == Role.STUDENT) {
                return "redirect:/student-dashboard";
            }

            if (user.getRole() == Role.STAFF) {
                return "redirect:/staff-dashboard";
            }

            if (user.getRole() == Role.ADMIN) {
                return "redirect:/admin-dashboard";
            }

            model.addAttribute("error", "Invalid user role.");

            return "login";

        } catch (RuntimeException exception) {

            model.addAttribute(
                    "error",
                    "Invalid email or password."
            );

            return "login";
        }
    }

    // ==========================================
    // STUDENT DASHBOARD
    // ==========================================

    @GetMapping("/student-dashboard")
    public String studentDashboard(
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId == null || !"STUDENT".equals(role)) {
            return "redirect:/login";
        }

        Student student = studentService.getStudentByUserId(userId);

        model.addAttribute("student", student);

        model.addAttribute(
                "statistics",
                clearanceRequestService.getStudentRequestStatistics(
                        student.getId()
                )
        );

        List<ClearanceAuthority> authorities =
                clearanceAuthorityService.getAllClearanceAuthorities();

        model.addAttribute("authorities", authorities);

        List<ClearanceRequest> requests =
                clearanceRequestService.getRequestsByStudentId(
                        student.getId()
                );

        if (!requests.isEmpty()) {

            ClearanceRequest latestRequest = requests.get(0);

            model.addAttribute("hasRequest", true);
            model.addAttribute("latestRequest", latestRequest);

            List<ClearanceVerificationDTO> verifications =
                    clearanceVerificationService.getVerificationsByRequestId(
                            latestRequest.getId()
                    );

            model.addAttribute("verifications", verifications);

            boolean allAuthoritiesCleared =
                    !authorities.isEmpty()
                    && authorities.size() == verifications.size()
                    && authorities.stream().allMatch(authority ->
                        verifications.stream().anyMatch(verification ->
                            authority.getName().equals(
                                    verification.getAuthorityName()
                            )
                            && verification.getStatus() == ClearanceStatus.CLEARED
                        )
                    );

            boolean certificateEligible =
                    latestRequest.getOverallStatus() == ClearanceStatus.CLEARED
                    && allAuthoritiesCleared;

            model.addAttribute(
                    "certificateEligible",
                    certificateEligible
            );

        } else {

            model.addAttribute("hasRequest", false);
            model.addAttribute("verifications", List.of());
            model.addAttribute("certificateEligible", false);
        }

        return "student-dashboard";
    }

    // ==========================================
    // STUDENT SENDS REQUEST TO A DEPARTMENT
    // ==========================================

    @PostMapping("/student/send-clearance/{requestId}/{authorityId}")
    public String sendDepartmentRequest(
            @PathVariable Long requestId,
            @PathVariable Long authorityId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId == null || !"STUDENT".equals(role)) {
            return "redirect:/login";
        }

        try {

            Student student = studentService.getStudentByUserId(userId);

            clearanceVerificationService.sendDepartmentRequest(
                    requestId,
                    authorityId,
                    student.getId()
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Department clearance request sent successfully!"
            );

        } catch (IllegalArgumentException | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

        } catch (RuntimeException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to send department clearance request."
            );
        }

        return "redirect:/student-dashboard";
    }

    // ==========================================
    // STAFF DASHBOARD
    // ==========================================

    @GetMapping("/staff-dashboard")
    public String staffDashboard(
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId == null || !"STAFF".equals(role)) {
            return "redirect:/login";
        }

        Staff staff = staffService.getStaffByUserId(userId);

        model.addAttribute("staff", staff);

        if (staff.getDepartment() == null) {

            model.addAttribute("verifications", List.of());
            model.addAttribute("totalTasks", 0);
            model.addAttribute("pendingTasks", 0);
            model.addAttribute("completedTasks", 0);
            model.addAttribute("rejectedTasks", 0);

            model.addAttribute(
                    "errorMessage",
                    "You are not assigned to any department."
            );

            return "staff-dashboard";
        }

        Long departmentId = staff.getDepartment().getId();

        List<ClearanceVerificationDTO> verifications =
                clearanceVerificationService.getVerificationsByDepartment(
                        departmentId
                );

        long totalTasks = verifications.size();

        long pendingTasks = verifications.stream()
                .filter(v ->
                        v.getStatus() == ClearanceStatus.PENDING
                        || v.getStatus() == ClearanceStatus.UNDER_REVIEW)
                .count();

        long completedTasks = verifications.stream()
                .filter(v ->
                        v.getStatus() == ClearanceStatus.CLEARED)
                .count();

        long rejectedTasks = verifications.stream()
                .filter(v ->
                        v.getStatus() == ClearanceStatus.REJECTED)
                .count();

        model.addAttribute("verifications", verifications);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("rejectedTasks", rejectedTasks);

        return "staff-dashboard";
    }

    // ==========================================
    // STAFF APPROVE OR REJECT CLEARANCE
    // ==========================================

    @PostMapping("/staff/verify/{id}")
    public String verifyClearance(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String remarks,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId == null || !"STAFF".equals(role)) {
            return "redirect:/login";
        }

        try {

            Staff staff = staffService.getStaffByUserId(userId);

            ClearanceStatus clearanceStatus =
                    ClearanceStatus.valueOf(status);

            if (clearanceStatus != ClearanceStatus.CLEARED
                    && clearanceStatus != ClearanceStatus.REJECTED) {

                throw new IllegalArgumentException(
                        "Invalid verification status."
                );
            }

            ClearanceVerification verification =
                    new ClearanceVerification();

            verification.setStatus(clearanceStatus);
            verification.setRemarks(remarks);
            verification.setVerifiedBy(staff);

            clearanceVerificationService.updateClearanceVerification(
                    id,
                    verification
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Clearance verification updated successfully!"
            );

        } catch (IllegalArgumentException | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

        } catch (RuntimeException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to update clearance verification."
            );
        }

        return "redirect:/staff-dashboard";
    }

    // ==========================================
    // ADMIN DASHBOARD
    // ==========================================

    @GetMapping("/admin-dashboard")
    public String adminDashboard(
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId == null || !"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        var users = userService.getAllUsers();

        List<Student> students =
                studentService.getAllStudents();

        List<Staff> staff =
                staffService.getAllStaff();

        var departments =
                departmentService.getAllDepartments();

        List<ClearanceRequest> requests =
                clearanceRequestService.getAllClearanceRequests();

        long totalApplications = requests.size();

        long pendingApplications = requests.stream()
                .filter(r -> r.getOverallStatus() == ClearanceStatus.PENDING)
                .count();

        long underReviewApplications = requests.stream()
                .filter(r -> r.getOverallStatus() == ClearanceStatus.UNDER_REVIEW)
                .count();

        long clearedApplications = requests.stream()
                .filter(r -> r.getOverallStatus() == ClearanceStatus.CLEARED)
                .count();

        long rejectedApplications = requests.stream()
                .filter(r -> r.getOverallStatus() == ClearanceStatus.REJECTED)
                .count();

        model.addAttribute("totalUsers", users.size());
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("totalStaff", staff.size());
        model.addAttribute("totalDepartments", departments.size());

        model.addAttribute("totalApplications", totalApplications);
        model.addAttribute("pendingApplications", pendingApplications);
        model.addAttribute("underReviewApplications", underReviewApplications);
        model.addAttribute("clearedApplications", clearedApplications);
        model.addAttribute("rejectedApplications", rejectedApplications);

        model.addAttribute("students", students);
        model.addAttribute("staffList", staff);
        model.addAttribute("departments", departments);
        model.addAttribute("requests", requests);

        return "admin-dashboard";
    }

    // ==========================================
    // APPLY FOR CLEARANCE PAGE
    // ==========================================

    @GetMapping("/apply-clearance")
    public String applyClearancePage(
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId == null || !"STUDENT".equals(role)) {
            return "redirect:/login";
        }

        Student student = studentService.getStudentByUserId(userId);

        model.addAttribute("student", student);

        return "apply-clearance";
    }

    // ==========================================
    // APPLY FOR CLEARANCE
    // ==========================================

    @PostMapping("/apply-clearance")
    public String applyClearance(
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId == null || !"STUDENT".equals(role)) {
            return "redirect:/login";
        }

        try {

            Student student = studentService.getStudentByUserId(userId);

            clearanceRequestService.applyForClearance(student.getId());

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Clearance application created successfully!"
            );

        } catch (IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

        } catch (RuntimeException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to apply for clearance."
            );
        }

        return "redirect:/student-dashboard";
    }

    // ==========================================
    // TRACK CLEARANCE
    // ==========================================

    @GetMapping("/track-clearance")
    public String trackClearance(
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId == null || !"STUDENT".equals(role)) {
            return "redirect:/login";
        }

        Student student = studentService.getStudentByUserId(userId);

        model.addAttribute("student", student);

        List<ClearanceRequest> requests =
                clearanceRequestService.getRequestsByStudentId(
                        student.getId()
                );

        if (requests.isEmpty()) {

            model.addAttribute("hasRequest", false);

        } else {

            ClearanceRequest latestRequest = requests.get(0);

            model.addAttribute("hasRequest", true);
            model.addAttribute("latestRequest", latestRequest);

            List<ClearanceVerificationDTO> verifications =
                    clearanceVerificationService.getVerificationsByRequestId(
                            latestRequest.getId()
                    );

            model.addAttribute("verifications", verifications);
        }

        return "track-clearance";
    }

    // ==========================================
    // STUDENT REQUEST HISTORY
    // ==========================================

    @GetMapping("/request-history")
    public String requestHistory(
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId == null || !"STUDENT".equals(role)) {
            return "redirect:/login";
        }

        Student student = studentService.getStudentByUserId(userId);

        List<ClearanceRequest> requests =
                clearanceRequestService.getRequestsByStudentId(
                        student.getId()
                );

        model.addAttribute("student", student);
        model.addAttribute("requests", requests);

        return "request-history";
    }

    // ==========================================
    // STUDENT REQUEST DETAILS
    // ==========================================

    @GetMapping("/request-details/{requestId}")
    public String requestDetails(
            @PathVariable Long requestId,
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId == null || !"STUDENT".equals(role)) {
            return "redirect:/login";
        }

        Student student = studentService.getStudentByUserId(userId);

        ClearanceRequest request =
                clearanceRequestService.getClearanceRequestById(requestId);

        if (!request.getStudent().getId().equals(student.getId())) {
            return "redirect:/request-history";
        }

        List<ClearanceVerificationDTO> verifications =
                clearanceVerificationService.getVerificationsByRequestId(
                        requestId
                );

        model.addAttribute("student", student);
        model.addAttribute("request", request);
        model.addAttribute("verifications", verifications);

        return "request-details";
    }

    // ==========================================
    // DOWNLOAD CLEARANCE CERTIFICATE
    // ==========================================

    @GetMapping("/student/download-certificate/{requestId}")
    public ResponseEntity<?> downloadCertificate(
            @PathVariable Long requestId,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        // Check login and student role
        if (userId == null || !"STUDENT".equals(role)) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Please log in as a student.");
        }

        try {

            // Get logged-in student
            Student student =
                    studentService.getStudentByUserId(userId);

            // Get clearance request
            ClearanceRequest request =
                    clearanceRequestService.getClearanceRequestById(requestId);

            // Check request ownership
            if (!request.getStudent().getId().equals(student.getId())) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body("You cannot download another student's certificate.");
            }

            // Generate certificate PDF
            byte[] pdf =
                    certificateService.generateCertificate(requestId);

            // Certificate filename
            String fileName = "E-Clearance-Certificate-"
                    + student.getRollNumber() + ".pdf";

            // Return downloadable PDF
            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\""
                    )
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (IllegalStateException exception) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(exception.getMessage());
        }
    }

    // ==========================================
    // LOGOUT
    // ==========================================

    @GetMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/login";
    }
}