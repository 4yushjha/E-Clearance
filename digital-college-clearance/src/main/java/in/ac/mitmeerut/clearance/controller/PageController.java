package in.ac.mitmeerut.clearance.controller;

import java.util.List;

import in.ac.mitmeerut.clearance.entity.ClearanceVerification;
import in.ac.mitmeerut.clearance.dto.ClearanceVerificationDTO;
import in.ac.mitmeerut.clearance.entity.Student;
import in.ac.mitmeerut.clearance.entity.User;
import in.ac.mitmeerut.clearance.entity.Staff;
import in.ac.mitmeerut.clearance.entity.ClearanceRequest;

import in.ac.mitmeerut.clearance.enums.Role;
import in.ac.mitmeerut.clearance.enums.ClearanceStatus;

import in.ac.mitmeerut.clearance.service.StudentService;
import in.ac.mitmeerut.clearance.service.UserService;
import in.ac.mitmeerut.clearance.service.StaffService;
import in.ac.mitmeerut.clearance.service.ClearanceRequestService;
import in.ac.mitmeerut.clearance.service.ClearanceVerificationService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {

    private final UserService userService;
    private final StudentService studentService;
    private final StaffService staffService;
    private final ClearanceRequestService clearanceRequestService;
    private final ClearanceVerificationService clearanceVerificationService;

    // Constructor
    public PageController(
            UserService userService,
            StudentService studentService,
            StaffService staffService,
            ClearanceRequestService clearanceRequestService,
            ClearanceVerificationService clearanceVerificationService) {

        this.userService = userService;
        this.studentService = studentService;
        this.staffService = staffService;
        this.clearanceRequestService = clearanceRequestService;
        this.clearanceVerificationService = clearanceVerificationService;
    }

    // Open login page
    @GetMapping("/login")
    public String loginPage(HttpSession session) {

        if (session.getAttribute("loggedInUserId") != null) {

            String role = (String) session.getAttribute("loggedInRole");

            if ("STUDENT".equals(role)) {
                return "redirect:/student-dashboard";
            }

            if ("STAFF".equals(role)) {
                return "redirect:/staff-dashboard";
            }

            if ("ADMIN".equals(role)) {
                return "redirect:/admin-dashboard";
            }
        }

        return "login";
    }

    // Process login
    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            Model model,
            HttpSession session) {

        try {

            User user = userService.getUserByEmail(email);

            if (user.getPassword() == null
                    || !user.getPassword().equals(password)) {

                model.addAttribute("error", "Invalid email or password.");
                return "login";
            }

            session.setAttribute("loggedInUserId", user.getId());
            session.setAttribute("loggedInRole", user.getRole().name());

            if (user.getRole() == Role.STUDENT) {

                studentService.getStudentByUserId(user.getId());

                return "redirect:/student-dashboard";
            }

            if (user.getRole() == Role.STAFF) {

                return "redirect:/staff-dashboard";
            }

            if (user.getRole() == Role.ADMIN) {

                return "redirect:/admin-dashboard";
            }

            session.invalidate();

            model.addAttribute("error", "Invalid user role.");
            return "login";

        } catch (RuntimeException exception) {

            session.removeAttribute("loggedInUserId");
            session.removeAttribute("loggedInRole");

            model.addAttribute("error", "Invalid email or password.");

            return "login";
        }
    }


    @GetMapping("/student-dashboard")
    public String studentDashboard(
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId == null || !"STUDENT".equals(role)) {
            return "redirect:/login";
        }

        // Fetch logged-in student
        Student student = studentService.getStudentByUserId(userId);

        model.addAttribute("student", student);

        // Fetch student statistics
        model.addAttribute(
                "statistics",
                clearanceRequestService.getStudentRequestStatistics(student.getId())
        );

        // Fetch student's clearance requests
        List<ClearanceRequest> requests =
                clearanceRequestService.getRequestsByStudentId(student.getId());

        if (!requests.isEmpty()) {

            // Get the latest clearance request
            ClearanceRequest latestRequest = requests.get(0);

            model.addAttribute("hasRequest", true);
            model.addAttribute("latestRequest", latestRequest);

            // Fetch department-wise verification records
            List<ClearanceVerificationDTO> verifications =
                    clearanceVerificationService.getVerificationsByRequestId(
                            latestRequest.getId()
                    );

            model.addAttribute("verifications", verifications);

        } else {

            model.addAttribute("hasRequest", false);
            model.addAttribute("verifications", List.of());

        }

        return "student-dashboard";
    }
   

    // Staff Dashboard
    @GetMapping("/staff-dashboard")
    public String staffDashboard(
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId == null || !"STAFF".equals(role)) {
            return "redirect:/login";
        }

        // Get logged-in staff member
        Staff staff = staffService.getStaffByUserId(userId);

        model.addAttribute("staff", staff);

        // Check department
        if (staff.getDepartment() == null) {

            model.addAttribute("verifications", List.of());
            model.addAttribute("totalTasks", 0);
            model.addAttribute("pendingTasks", 0);
            model.addAttribute("completedTasks", 0);
            model.addAttribute("rejectedTasks", 0);

            model.addAttribute(
                    "error",
                    "No department has been assigned to your account."
            );

            return "staff-dashboard";
        }

        Long departmentId = staff.getDepartment().getId();

        // Get department-wise verifications
        List<ClearanceVerificationDTO> verifications =
                clearanceVerificationService.getVerificationsByDepartment(
                        departmentId
                );

        // Calculate total tasks
        long totalTasks = verifications.size();

        // Calculate pending and under-review tasks
        long pendingTasks = verifications.stream()
                .filter(v ->
                        v.getStatus() == ClearanceStatus.PENDING
                        || v.getStatus() == ClearanceStatus.UNDER_REVIEW
                )
                .count();

        // Calculate completed tasks
        long completedTasks = verifications.stream()
                .filter(v ->
                        v.getStatus() == ClearanceStatus.CLEARED
                )
                .count();

        // Calculate rejected tasks
        long rejectedTasks = verifications.stream()
                .filter(v ->
                        v.getStatus() == ClearanceStatus.REJECTED
                )
                .count();

        // Send data to dashboard
        model.addAttribute("verifications", verifications);

        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("rejectedTasks", rejectedTasks);

        return "staff-dashboard";
    }

    // Admin Dashboard
    @GetMapping("/admin-dashboard")
    public String adminDashboard(HttpSession session) {

        String role = (String) session.getAttribute("loggedInRole");

        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        return "admin-dashboard";
    }

    // Display Apply for Clearance page
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

    // Process clearance application
    @PostMapping("/apply-clearance")
    public String submitClearanceApplication(
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        if (userId == null || !"STUDENT".equals(role)) {
            return "redirect:/login";
        }

        Student student = studentService.getStudentByUserId(userId);

        try {

            clearanceRequestService.applyForClearance(student.getId());

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Your clearance application has been submitted successfully!"
            );

            return "redirect:/student-dashboard";

        } catch (IllegalStateException exception) {

            model.addAttribute("student", student);
            model.addAttribute("error", exception.getMessage());

            return "apply-clearance";
        }
    }

    // Display Track Clearance page
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

        List<ClearanceRequest> requests =
                clearanceRequestService.getRequestsByStudentId(student.getId());

        model.addAttribute("student", student);

        if (requests.isEmpty()) {

            model.addAttribute("hasRequest", false);

        } else {

            ClearanceRequest latestRequest = requests.get(0);

            model.addAttribute("hasRequest", true);
            model.addAttribute("latestRequest", latestRequest);

            model.addAttribute(
                    "verifications",
                    clearanceVerificationService.getVerificationsByRequestId(
                            latestRequest.getId())
            );
        }

        return "track-clearance";
    }

    // Display Request History page
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
                clearanceRequestService.getRequestsByStudentId(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("requests", requests);

        return "request-history";
    }

    // View individual clearance application details
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

        // Ensure request belongs to logged-in student
        if (!request.getStudent().getId().equals(student.getId())) {
            return "redirect:/request-history";
        }

        model.addAttribute("student", student);
        model.addAttribute("request", request);

        model.addAttribute(
                "verifications",
                clearanceVerificationService.getVerificationsByRequestId(requestId)
        );

        return "request-details";
    }

 // STAFF APPROVE OR REJECT CLEARANCE
    @PostMapping("/staff/verify/{id}")
    public String verifyClearance(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String remarks,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // GET LOGGED-IN USER DETAILS
        Long userId = (Long) session.getAttribute("loggedInUserId");
        String role = (String) session.getAttribute("loggedInRole");

        // CHECK STAFF LOGIN
        if (userId == null || !"STAFF".equals(role)) {
            return "redirect:/login";
        }

        try {

            // GET STAFF FROM SESSION USER ID
            Staff staff = staffService.getStaffByUserId(userId);

            // ACCEPT ONLY CLEARED OR REJECTED
            ClearanceStatus clearanceStatus =
                    ClearanceStatus.valueOf(status);

            if (clearanceStatus != ClearanceStatus.CLEARED
                    && clearanceStatus != ClearanceStatus.REJECTED) {

                throw new IllegalArgumentException(
                        "Invalid verification status.");
            }

            // CREATE VERIFICATION UPDATE
            ClearanceVerification verification =
                    new ClearanceVerification();

            verification.setStatus(clearanceStatus);
            verification.setRemarks(remarks);

            // USE LOGGED-IN STAFF
            verification.setVerifiedBy(staff);

            // UPDATE VERIFICATION
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
    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/login";
    }
}