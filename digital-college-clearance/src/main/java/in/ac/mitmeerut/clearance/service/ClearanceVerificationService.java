package in.ac.mitmeerut.clearance.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.mitmeerut.clearance.dto.ClearanceVerificationDTO;
import in.ac.mitmeerut.clearance.entity.ClearanceAuthority;
import in.ac.mitmeerut.clearance.entity.ClearanceRequest;
import in.ac.mitmeerut.clearance.entity.ClearanceVerification;
import in.ac.mitmeerut.clearance.entity.Staff;
import in.ac.mitmeerut.clearance.enums.ClearanceStatus;
import in.ac.mitmeerut.clearance.exception.ClearanceAuthorityNotFoundException;
import in.ac.mitmeerut.clearance.exception.ClearanceRequestNotFoundException;
import in.ac.mitmeerut.clearance.exception.ClearanceVerificationNotFoundException;
import in.ac.mitmeerut.clearance.exception.StaffNotFoundException;
import in.ac.mitmeerut.clearance.repository.ClearanceAuthorityRepository;
import in.ac.mitmeerut.clearance.repository.ClearanceRequestRepository;
import in.ac.mitmeerut.clearance.repository.ClearanceVerificationRepository;
import in.ac.mitmeerut.clearance.repository.StaffRepository;

@Service
@Transactional
public class ClearanceVerificationService {

    private final ClearanceVerificationRepository clearanceVerificationRepository;
    private final ClearanceRequestRepository clearanceRequestRepository;
    private final ClearanceAuthorityRepository clearanceAuthorityRepository;
    private final StaffRepository staffRepository;

    public ClearanceVerificationService(
            ClearanceVerificationRepository clearanceVerificationRepository,
            ClearanceRequestRepository clearanceRequestRepository,
            ClearanceAuthorityRepository clearanceAuthorityRepository,
            StaffRepository staffRepository) {

        this.clearanceVerificationRepository = clearanceVerificationRepository;
        this.clearanceRequestRepository = clearanceRequestRepository;
        this.clearanceAuthorityRepository = clearanceAuthorityRepository;
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

    // CREATE VERIFICATION
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
                                "Clearance request not found with ID: "
                                        + requestId));

        ClearanceAuthority authority = clearanceAuthorityRepository
                .findById(authorityId)
                .orElseThrow(() ->
                        new ClearanceAuthorityNotFoundException(
                                "Clearance authority not found with ID: "
                                        + authorityId));

        verification.setClearanceRequest(request);
        verification.setAuthority(authority);
        verification.setStatus(ClearanceStatus.PENDING);
        verification.setVerifiedDate(null);
        verification.setVerifiedBy(null);

        ClearanceVerification saved =
                clearanceVerificationRepository.save(verification);

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
                                        "Clearance verification not found with ID: "
                                                + id));

        return convertToDTO(verification);
    }

    // GET VERIFICATIONS BY CLEARANCE REQUEST ID
    @Transactional(readOnly = true)
    public List<ClearanceVerificationDTO> getVerificationsByRequestId(
            Long requestId) {

        if (!clearanceRequestRepository.existsById(requestId)) {

            throw new ClearanceRequestNotFoundException(
                    "Clearance request not found with ID: " + requestId);
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
                                        "Clearance verification not found with ID: "
                                                + id));

        if (verification.getStatus() == null) {

            throw new IllegalArgumentException(
                    "Verification status is required");
        }

        if (verification.getStatus() == ClearanceStatus.PENDING) {

            throw new IllegalArgumentException(
                    "Verification cannot be updated to PENDING");
        }

        if (verification.getVerifiedBy() == null
                || verification.getVerifiedBy().getId() == null) {

            throw new IllegalArgumentException(
                    "Staff ID is required");
        }

        // FIND STAFF
        Long staffId = verification.getVerifiedBy().getId();

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() ->
                        new StaffNotFoundException(
                                "Staff not found with ID: " + staffId));

        // CHECK STAFF DEPARTMENT
        if (staff.getDepartment() == null) {

            throw new IllegalStateException(
                    "Staff member is not assigned to a department");
        }

        // CHECK AUTHORITY DEPARTMENT
        if (existing.getAuthority().getDepartment() == null) {

            throw new IllegalStateException(
                    "Clearance authority is not assigned to a department");
        }

        Long staffDepartmentId = staff.getDepartment().getId();

        Long authorityDepartmentId =
                existing.getAuthority().getDepartment().getId();

        // PREVENT CROSS-DEPARTMENT APPROVAL
        if (!staffDepartmentId.equals(authorityDepartmentId)) {

            throw new IllegalStateException(
                    "You are not authorized to verify this department's clearance");
        }

        // UPDATE VERIFICATION
        existing.setStatus(verification.getStatus());
        existing.setRemarks(verification.getRemarks());
        existing.setVerifiedBy(staff);
        existing.setVerifiedDate(LocalDateTime.now());

        ClearanceVerification saved =
                clearanceVerificationRepository.save(existing);

        // UPDATE OVERALL REQUEST STATUS
        updateOverallRequestStatus(
                existing.getClearanceRequest().getId());

        return convertToDTO(saved);
    }

    // UPDATE OVERALL CLEARANCE REQUEST STATUS
    private void updateOverallRequestStatus(Long requestId) {

        ClearanceRequest request = clearanceRequestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new ClearanceRequestNotFoundException(
                                "Clearance request not found with ID: "
                                        + requestId));

        List<ClearanceVerification> verifications =
                clearanceVerificationRepository
                        .findByClearanceRequest_Id(requestId);

        if (verifications.isEmpty()) {
            return;
        }

        boolean rejected = verifications.stream()
                .anyMatch(v ->
                        v.getStatus() == ClearanceStatus.REJECTED);

        boolean allCleared = verifications.stream()
                .allMatch(v ->
                        v.getStatus() == ClearanceStatus.CLEARED);

        boolean underReview = verifications.stream()
                .anyMatch(v ->
                        v.getStatus() == ClearanceStatus.UNDER_REVIEW);

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
                                        "Clearance verification not found with ID: "
                                                + id));

        Long requestId = verification.getClearanceRequest().getId();

        clearanceVerificationRepository.delete(verification);

        updateOverallRequestStatus(requestId);
    }
}