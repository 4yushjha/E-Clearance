package in.ac.mitmeerut.clearance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import in.ac.mitmeerut.clearance.entity.ClearanceVerification;

public interface ClearanceVerificationRepository
        extends JpaRepository<ClearanceVerification, Long> {

    // Find all verifications
    // associated with a particular clearance request
    List<ClearanceVerification> findByClearanceRequest_Id(Long requestId);

}