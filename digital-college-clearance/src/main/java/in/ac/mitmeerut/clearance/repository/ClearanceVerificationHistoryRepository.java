package in.ac.mitmeerut.clearance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import in.ac.mitmeerut.clearance.entity.ClearanceVerificationHistory;

public interface ClearanceVerificationHistoryRepository
        extends JpaRepository<ClearanceVerificationHistory, Long> {

    // Get complete history of a department verification
    List<ClearanceVerificationHistory>
    findByVerification_IdOrderByAttemptNumberAsc(Long verificationId);

    // Get the latest attempt number
    java.util.Optional<ClearanceVerificationHistory>
    findTopByVerification_IdOrderByAttemptNumberDesc(Long verificationId);
}