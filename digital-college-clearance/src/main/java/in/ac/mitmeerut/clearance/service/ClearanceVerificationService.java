package in.ac.mitmeerut.clearance.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.mitmeerut.clearance.dto.ClearanceVerificationDTO;
import in.ac.mitmeerut.clearance.entity.ClearanceAuthority;
import in.ac.mitmeerut.clearance.entity.ClearanceRequest;
import in.ac.mitmeerut.clearance.entity.ClearanceVerification;
import in.ac.mitmeerut.clearance.entity.ClearanceVerificationHistory;
import in.ac.mitmeerut.clearance.entity.Staff;
import in.ac.mitmeerut.clearance.enums.ClearanceStatus;
import in.ac.mitmeerut.clearance.exception.ClearanceAuthorityNotFoundException;
import in.ac.mitmeerut.clearance.exception.ClearanceRequestNotFoundException;
import in.ac.mitmeerut.clearance.exception.ClearanceVerificationNotFoundException;
import in.ac.mitmeerut.clearance.exception.StaffNotFoundException;
import in.ac.mitmeerut.clearance.repository.ClearanceAuthorityRepository;
import in.ac.mitmeerut.clearance.repository.ClearanceRequestRepository;
import in.ac.mitmeerut.clearance.repository.ClearanceVerificationHistoryRepository;
import in.ac.mitmeerut.clearance.repository.ClearanceVerificationRepository;
import in.ac.mitmeerut.clearance.repository.StaffRepository;

@Service
@Transactional
public class ClearanceVerificationService {

    private final ClearanceVerificationRepository clearanceVerificationRepository;
    private final ClearanceRequestRepository clearanceRequestRepository;
    private final ClearanceAuthorityRepository clearanceAuthorityRepository;
    private final ClearanceVerificationHistoryRepository historyRepository;
    private final StaffRepository staffRepository;

    public ClearanceVerificationService(
            ClearanceVerificationRepository clearanceVerificationRepository,
            ClearanceRequestRepository clearanceRequestRepository,
            ClearanceAuthorityRepository clearanceAuthorityRepository,
            ClearanceVerificationHistoryRepository historyRepository,
            StaffRepository staffRepository) {

        this.clearanceVerificationRepository = clearanceVerificationRepository;
        this.clearanceRequestRepository = clearanceRequestRepository;
        this.clearanceAuthorityRepository = clearanceAuthorityRepository;
        this.historyRepository = historyRepository;
        this.staffRepository = staffRepository;
    }

    // CONVERT ENTITY INTO DTO
    private ClearanceVerificationDTO convertToDTO(
            ClearanceVerification verification) {

        return new ClearanceVerificationDTO(
                verification.getId(),
                verification.getClearanceRequest().getId(),
                verification.getAuthority().getName(),
                verification.getClearanceRequest()
                        .getStudent().getUser().getName(),
                verification.getClearanceRequest()
                        .getStudent().getRollNumber(),
                verification.getStatus(),
                verification.getRemarks(),
                verification.getVerifiedBy() != null
                        ? verification.getVerifiedBy()
                                .getUser().getName()
                        : null,
                verification.getVerifiedDate()
        );
    }

    // SAVE VERIFICATION HISTORY
    private void saveHistory(
            ClearanceVerification verification,
            ClearanceStatus status,
            String remarks,
            Staff staff,
            Integer attemptNumber) {

        ClearanceVerificationHistory history =
                new ClearanceVerificationHistory();

        history.setVerification(verification);
        history.setStatus(status);
        history.setRemarks(remarks);
        history.setStaff(staff);
        history.setActionDate(LocalDateTime.now());
        history.setAttemptNumber(attemptNumber);

        historyRepository.save(history);
    }

    // GET NEXT ATTEMPT NUMBER
    private Integer getNextAttemptNumber(Long verificationId) {

        List<ClearanceVerificationHistory> histories =
                historyRepository
                        .findByVerification_IdOrderByAttemptNumberAsc(
                                verificationId);

        System.out.println("========== HISTORY DEBUG ==========");
        System.out.println("Verification ID: " + verificationId);
        System.out.println("History records: " + histories.size());

        for (ClearanceVerificationHistory history : histories) {
            System.out.println(
                    "History ID: " + history.getId()
                    + " | Status: " + history.getStatus()
                    + " | Attempt: " + history.getAttemptNumber()
            );
        }

        Integer nextAttempt = histories.stream()
                .mapToInt(ClearanceVerificationHistory::getAttemptNumber)
                .max()
                .orElse(0) + 1;

        System.out.println("Next attempt number: " + nextAttempt);
        System.out.println("===================================");

        return nextAttempt;
    }

    // STUDENT SENDS REQUEST TO INDIVIDUAL DEPARTMENT
    public ClearanceVerificationDTO sendDepartmentRequest(
            Long requestId,
            Long authorityId,
            Long studentId) {

        ClearanceRequest request = clearanceRequestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new ClearanceRequestNotFoundException(
                                "Clearance request not found: " + requestId));

        if (!request.getStudent().getId().equals(studentId)) {
            throw new IllegalStateException(
                    "You are not authorized to access this application.");
        }

        ClearanceAuthority authority = clearanceAuthorityRepository
                .findById(authorityId)
                .orElseThrow(() ->
                        new ClearanceAuthorityNotFoundException(
                                "Clearance authority not found: " + authorityId));

        Optional<ClearanceVerification> existingOptional =
                clearanceVerificationRepository
                        .findByClearanceRequest_IdAndAuthority_Id(
                                requestId, authorityId);

        ClearanceVerification verification;
        Integer attemptNumber;

        if (existingOptional.isEmpty()) {

            verification = new ClearanceVerification();

            verification.setClearanceRequest(request);
            verification.setAuthority(authority);
            verification.setStatus(ClearanceStatus.PENDING);
            verification.setVerifiedBy(null);
            verification.setVerifiedDate(null);
            verification.setRemarks(null);

            verification =
                    clearanceVerificationRepository.save(verification);

            attemptNumber = 1;

        } else {

            verification = existingOptional.get();

            if (verification.getStatus() != ClearanceStatus.REJECTED) {
                throw new IllegalStateException(
                        "You can only resend a rejected department request.");
            }

            attemptNumber = getNextAttemptNumber(verification.getId());

            verification.setStatus(ClearanceStatus.PENDING);
            verification.setRemarks(null);
            verification.setVerifiedBy(null);
            verification.setVerifiedDate(null);

            verification =
                    clearanceVerificationRepository.save(verification);
        }

        saveHistory(
                verification,
                ClearanceStatus.PENDING,
                "Request submitted by student",
                null,
                attemptNumber
        );

        updateOverallRequestStatus(requestId);

        return convertToDTO(verification);
    }

    // CREATE VERIFICATION THROUGH REST API
    public ClearanceVerificationDTO saveClearanceVerification(
            ClearanceVerification verification) {

        if (verification.getClearanceRequest() == null
                || verification.getClearanceRequest().getId() == null) {
            throw new IllegalArgumentException(
                    "Clearance request ID is required");
        }

        if (verification.getAuthority() == null
                || verification.getAuthority().getId() == null) {
            throw new IllegalArgumentException(
                    "Clearance authority ID is required");
        }

        Long requestId = verification.getClearanceRequest().getId();
        Long authorityId = verification.getAuthority().getId();

        ClearanceRequest request = clearanceRequestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new ClearanceRequestNotFoundException(
                                "Clearance request not found: " + requestId));

        ClearanceAuthority authority = clearanceAuthorityRepository
                .findById(authorityId)
                .orElseThrow(() ->
                        new ClearanceAuthorityNotFoundException(
                                "Clearance authority not found: " + authorityId));

        if (clearanceVerificationRepository
                .existsByClearanceRequest_IdAndAuthority_Id(
                        requestId, authorityId)) {
            throw new IllegalStateException(
                    "Verification already exists for this department.");
        }

        verification.setClearanceRequest(request);
        verification.setAuthority(authority);
        verification.setStatus(ClearanceStatus.PENDING);
        verification.setVerifiedDate(null);
        verification.setVerifiedBy(null);

        ClearanceVerification saved =
                clearanceVerificationRepository.save(verification);

        saveHistory(
                saved,
                ClearanceStatus.PENDING,
                "Request submitted",
                null,
                1
        );

        updateOverallRequestStatus(requestId);

        return convertToDTO(saved);
    }

    // GET ALL VERIFICATIONS
    @Transactional(readOnly = true)
    public List<ClearanceVerificationDTO> getAllClearanceVerifications() {

        return clearanceVerificationRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    // GET VERIFICATION BY ID
    @Transactional(readOnly = true)
    public ClearanceVerificationDTO getClearanceVerificationById(Long id) {

        ClearanceVerification verification =
                clearanceVerificationRepository.findById(id)
                        .orElseThrow(() ->
                                new ClearanceVerificationNotFoundException(
                                        "Clearance verification not found: " + id));

        return convertToDTO(verification);
    }

    // GET VERIFICATIONS BY CLEARANCE REQUEST
    @Transactional(readOnly = true)
    public List<ClearanceVerificationDTO> getVerificationsByRequestId(
            Long requestId) {

        if (!clearanceRequestRepository.existsById(requestId)) {
            throw new ClearanceRequestNotFoundException(
                    "Clearance request not found: " + requestId);
        }

        return clearanceVerificationRepository
                .findByClearanceRequest_Id(requestId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    // GET VERIFICATIONS BY DEPARTMENT
    @Transactional(readOnly = true)
    public List<ClearanceVerificationDTO> getVerificationsByDepartment(
            Long departmentId) {

        return clearanceVerificationRepository
                .findByAuthority_Department_Id(departmentId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    // APPROVE OR REJECT VERIFICATION
    public ClearanceVerificationDTO updateClearanceVerification(
            Long id,
            ClearanceVerification verification) {

        ClearanceVerification existing =
                clearanceVerificationRepository.findById(id)
                        .orElseThrow(() ->
                                new ClearanceVerificationNotFoundException(
                                        "Clearance verification not found: " + id));

        if (verification.getStatus() == null
                || verification.getStatus() == ClearanceStatus.PENDING) {
            throw new IllegalArgumentException(
                    "A valid verification status is required.");
        }

        if (existing.getStatus() != ClearanceStatus.PENDING
                && existing.getStatus() != ClearanceStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "This department request has already been processed.");
        }

        if (verification.getVerifiedBy() == null
                || verification.getVerifiedBy().getId() == null) {
            throw new IllegalArgumentException("Staff ID is required.");
        }

        Long staffId = verification.getVerifiedBy().getId();

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() ->
                        new StaffNotFoundException(
                                "Staff not found: " + staffId));

        if (staff.getDepartment() == null
                || existing.getAuthority().getDepartment() == null) {
            throw new IllegalStateException(
                    "Staff member or authority is not assigned to a department.");
        }

        Long staffDepartmentId = staff.getDepartment().getId();
        Long authorityDepartmentId =
                existing.getAuthority().getDepartment().getId();

        if (!staffDepartmentId.equals(authorityDepartmentId)) {
            throw new IllegalStateException(
                    "You are not authorized to verify this department's clearance.");
        }

        existing.setStatus(verification.getStatus());
        existing.setRemarks(verification.getRemarks());
        existing.setVerifiedBy(staff);
        existing.setVerifiedDate(LocalDateTime.now());

        ClearanceVerification saved =
                clearanceVerificationRepository.save(existing);

        Integer attemptNumber = getCurrentAttemptNumber(saved.getId());

        saveHistory(
                saved,
                saved.getStatus(),
                saved.getRemarks(),
                staff,
                attemptNumber
        );

        updateOverallRequestStatus(
                existing.getClearanceRequest().getId());

        return convertToDTO(saved);
    }

    // GET CURRENT ATTEMPT NUMBER
    private Integer getCurrentAttemptNumber(Long verificationId) {

        Optional<ClearanceVerificationHistory> latest =
                historyRepository
                        .findTopByVerification_IdOrderByAttemptNumberDesc(
                                verificationId);

        return latest.map(
                ClearanceVerificationHistory::getAttemptNumber
        ).orElse(1);
    }

    // UPDATE OVERALL APPLICATION STATUS
    private void updateOverallRequestStatus(Long requestId) {

        ClearanceRequest request = clearanceRequestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new ClearanceRequestNotFoundException(
                                "Clearance request not found: " + requestId));

        List<ClearanceVerification> verifications =
                clearanceVerificationRepository
                        .findByClearanceRequest_Id(requestId);

        long totalAuthorities = clearanceAuthorityRepository.count();

        boolean rejected = verifications.stream()
                .anyMatch(v -> v.getStatus() == ClearanceStatus.REJECTED);

        boolean underReview = verifications.stream()
                .anyMatch(v -> v.getStatus() == ClearanceStatus.UNDER_REVIEW);

        boolean allAuthoritiesSubmitted =
                totalAuthorities > 0
                && verifications.size() == totalAuthorities;

        boolean allCleared = allAuthoritiesSubmitted
                && verifications.stream()
                        .allMatch(v -> v.getStatus() == ClearanceStatus.CLEARED);

        if (rejected) {

            request.setOverallStatus(ClearanceStatus.REJECTED);
            request.setCompletionDate(null);

        } else if (allCleared) {

            request.setOverallStatus(ClearanceStatus.CLEARED);
            request.setCompletionDate(LocalDateTime.now());

        } else if (underReview) {

            request.setOverallStatus(ClearanceStatus.UNDER_REVIEW);
            request.setCompletionDate(null);

        } else {

            request.setOverallStatus(ClearanceStatus.PENDING);
            request.setCompletionDate(null);
        }

        clearanceRequestRepository.save(request);
    }

    // DELETE VERIFICATION
    public void deleteClearanceVerification(Long id) {

        ClearanceVerification verification =
                clearanceVerificationRepository.findById(id)
                        .orElseThrow(() ->
                                new ClearanceVerificationNotFoundException(
                                        "Clearance verification not found: " + id));

        Long requestId = verification.getClearanceRequest().getId();

        List<ClearanceVerificationHistory> histories =
                historyRepository
                        .findByVerification_IdOrderByAttemptNumberAsc(id);

        historyRepository.deleteAll(histories);
        clearanceVerificationRepository.delete(verification);

        updateOverallRequestStatus(requestId);
    }
}