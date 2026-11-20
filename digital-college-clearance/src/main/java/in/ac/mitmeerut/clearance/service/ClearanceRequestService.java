package in.ac.mitmeerut.clearance.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.mitmeerut.clearance.entity.ClearanceRequest;
import in.ac.mitmeerut.clearance.entity.ClearanceVerification;
import in.ac.mitmeerut.clearance.entity.ClearanceAuthority;
import in.ac.mitmeerut.clearance.entity.Student;

import in.ac.mitmeerut.clearance.enums.ClearanceStatus;

import in.ac.mitmeerut.clearance.exception.ClearanceRequestNotFoundException;
import in.ac.mitmeerut.clearance.exception.StudentNotFoundException;

import in.ac.mitmeerut.clearance.repository.ClearanceRequestRepository;
import in.ac.mitmeerut.clearance.repository.StudentRepository;
import in.ac.mitmeerut.clearance.repository.ClearanceAuthorityRepository;
import in.ac.mitmeerut.clearance.repository.ClearanceVerificationRepository;

@Service
public class ClearanceRequestService {

    private final ClearanceRequestRepository clearanceRequestRepository;
    private final StudentRepository studentRepository;
    private final ClearanceAuthorityRepository clearanceAuthorityRepository;
    private final ClearanceVerificationRepository clearanceVerificationRepository;

    public ClearanceRequestService(
            ClearanceRequestRepository clearanceRequestRepository,
            StudentRepository studentRepository,
            ClearanceAuthorityRepository clearanceAuthorityRepository,
            ClearanceVerificationRepository clearanceVerificationRepository) {

        this.clearanceRequestRepository = clearanceRequestRepository;
        this.studentRepository = studentRepository;
        this.clearanceAuthorityRepository = clearanceAuthorityRepository;
        this.clearanceVerificationRepository = clearanceVerificationRepository;
    }

    // CREATE THROUGH REST API
    public ClearanceRequest saveClearanceRequest(
            ClearanceRequest clearanceRequest) {

        Student student = studentRepository
                .findById(clearanceRequest.getStudent().getId())
                .orElseThrow(() ->
                        new StudentNotFoundException(
                                "Student not found with id: "
                                        + clearanceRequest.getStudent().getId()
                        )
                );

        clearanceRequest.setStudent(student);

        return clearanceRequestRepository.save(clearanceRequest);
    }

    // APPLY FOR CLEARANCE
    @Transactional
    public ClearanceRequest applyForClearance(Long studentId) {

        // Find student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new StudentNotFoundException(
                                "Student not found with id: " + studentId
                        )
                );

        // Prevent duplicate active applications
        boolean alreadyApplied =
                clearanceRequestRepository.existsByStudent_IdAndOverallStatusIn(
                        studentId,
                        List.of(
                                ClearanceStatus.PENDING,
                                ClearanceStatus.UNDER_REVIEW
                        )
                );

        if (alreadyApplied) {
            throw new IllegalStateException(
                    "You already have an active clearance request."
            );
        }

        // Get all clearance authorities
        List<ClearanceAuthority> authorities =
                clearanceAuthorityRepository.findAll();

        if (authorities.isEmpty()) {
            throw new IllegalStateException(
                    "No clearance authorities are available."
            );
        }

        // Create clearance request
        ClearanceRequest request = new ClearanceRequest();
        request.setStudent(student);
        request.setRequestDate(java.time.LocalDateTime.now());
        request.setOverallStatus(ClearanceStatus.PENDING);
        request.setCompletionDate(null);

        ClearanceRequest savedRequest =
                clearanceRequestRepository.save(request);

        // Create verification for every authority
        for (ClearanceAuthority authority : authorities) {

            ClearanceVerification verification =
                    new ClearanceVerification();

            verification.setClearanceRequest(savedRequest);
            verification.setAuthority(authority);
            verification.setStatus(ClearanceStatus.PENDING);
            verification.setVerifiedBy(null);
            verification.setVerifiedDate(null);

            clearanceVerificationRepository.save(verification);
        }

        return savedRequest;
    }

    // GET ALL
    public List<ClearanceRequest> getAllClearanceRequests() {
        return clearanceRequestRepository.findAll();
    }

    // GET BY ID
    public ClearanceRequest getClearanceRequestById(Long id) {

        return clearanceRequestRepository.findById(id)
                .orElseThrow(() ->
                        new ClearanceRequestNotFoundException(
                                "Clearance request not found with id: " + id
                        )
                );
    }

    // UPDATE
    public ClearanceRequest updateClearanceRequest(
            Long id,
            ClearanceRequest clearanceRequest) {

        ClearanceRequest existingRequest =
                clearanceRequestRepository.findById(id)
                        .orElseThrow(() ->
                                new ClearanceRequestNotFoundException(
                                        "Clearance request not found with id: "
                                                + id
                                )
                        );

        existingRequest.setOverallStatus(
                clearanceRequest.getOverallStatus()
        );

        existingRequest.setCompletionDate(
                clearanceRequest.getCompletionDate()
        );

        return clearanceRequestRepository.save(existingRequest);
    }

    // DELETE
    public void deleteClearanceRequest(Long id) {

        ClearanceRequest existingRequest =
                clearanceRequestRepository.findById(id)
                        .orElseThrow(() ->
                                new ClearanceRequestNotFoundException(
                                        "Clearance request not found with id: "
                                                + id
                                )
                        );

        clearanceRequestRepository.delete(existingRequest);
    }
    
    @Transactional(readOnly = true)
    public List<ClearanceRequest> getRequestsByStudentId(Long studentId) {
        return clearanceRequestRepository
                .findByStudent_IdOrderByRequestDateDesc(studentId);
    }
    
    
    // STUDENT DASHBOARD STATISTICS
    public Map<String, Long> getStudentRequestStatistics(Long studentId) {

        long total = clearanceRequestRepository
                .countByStudent_Id(studentId);

        long completed = clearanceRequestRepository
                .countByStudent_IdAndOverallStatus(
                        studentId, ClearanceStatus.CLEARED);

        long pending = clearanceRequestRepository
                .countByStudent_IdAndOverallStatus(
                        studentId, ClearanceStatus.PENDING);

        long rejected = clearanceRequestRepository
                .countByStudent_IdAndOverallStatus(
                        studentId, ClearanceStatus.REJECTED);

        return Map.of(
                "total", total,
                "completed", completed,
                "pending", pending,
                "rejected", rejected
        );
    }
}