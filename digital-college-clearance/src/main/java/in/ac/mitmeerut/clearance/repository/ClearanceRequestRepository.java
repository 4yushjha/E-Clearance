package in.ac.mitmeerut.clearance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import in.ac.mitmeerut.clearance.entity.ClearanceRequest;
import in.ac.mitmeerut.clearance.enums.ClearanceStatus;

public interface ClearanceRequestRepository
        extends JpaRepository<ClearanceRequest, Long> {

    // Count all requests of a student
    long countByStudent_Id(Long studentId);

    // Count requests by status
    long countByStudent_IdAndOverallStatus(
            Long studentId, ClearanceStatus status);

    // Check whether an active request already exists
    boolean existsByStudent_IdAndOverallStatusIn(
            Long studentId, List<ClearanceStatus> statuses);

    // Get all requests of a student, newest first
    List<ClearanceRequest> findByStudent_IdOrderByRequestDateDesc(
            Long studentId);
}