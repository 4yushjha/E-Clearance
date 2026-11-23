package in.ac.mitmeerut.clearance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import in.ac.mitmeerut.clearance.entity.ClearanceVerification;

public interface ClearanceVerificationRepository
        extends JpaRepository<ClearanceVerification, Long> {

    List<ClearanceVerification> findByClearanceRequest_Id(Long requestId);

    List<ClearanceVerification> findByAuthority_Department_Id(Long departmentId);

    // Find a particular department verification in an application
    Optional<ClearanceVerification> findByClearanceRequest_IdAndAuthority_Id(
            Long requestId, Long authorityId);

    // Check whether a verification already exists
    boolean existsByClearanceRequest_IdAndAuthority_Id(
            Long requestId, Long authorityId);
}