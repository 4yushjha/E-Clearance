package in.ac.mitmeerut.clearance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import in.ac.mitmeerut.clearance.entity.ClearanceVerification;

public interface ClearanceVerificationRepository
        extends JpaRepository<ClearanceVerification, Long> {

    List<ClearanceVerification> findByClearanceRequest_Id(Long requestId);

    // Find verifications belonging to a department
    List<ClearanceVerification>
    findByAuthority_Department_Id(Long departmentId);
}