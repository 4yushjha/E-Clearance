package in.ac.mitmeerut.clearance.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.mitmeerut.clearance.entity.ClearanceAuthority;
import in.ac.mitmeerut.clearance.entity.Department;
import in.ac.mitmeerut.clearance.exception.ClearanceAuthorityNotFoundException;
import in.ac.mitmeerut.clearance.repository.ClearanceAuthorityRepository;
import in.ac.mitmeerut.clearance.repository.DepartmentRepository;

@Service
public class ClearanceAuthorityService {

    private final ClearanceAuthorityRepository clearanceAuthorityRepository;
    private final DepartmentRepository departmentRepository;

    public ClearanceAuthorityService(
            ClearanceAuthorityRepository clearanceAuthorityRepository,
            DepartmentRepository departmentRepository) {

        this.clearanceAuthorityRepository = clearanceAuthorityRepository;
        this.departmentRepository = departmentRepository;
    }

    // Create Clearance Authority
    public ClearanceAuthority saveClearanceAuthority(
            ClearanceAuthority authority) {

        return clearanceAuthorityRepository.save(authority);
    }

    // Get all Clearance Authorities
    public List<ClearanceAuthority> getAllClearanceAuthorities() {

        return clearanceAuthorityRepository.findAll();
    }

    // Get Clearance Authority by ID
    public ClearanceAuthority getClearanceAuthorityById(Long id) {

        return clearanceAuthorityRepository.findById(id)
                .orElseThrow(() ->
                        new ClearanceAuthorityNotFoundException(
                                "Clearance Authority not found with id: " + id
                        )
                );
    }

    // Update Clearance Authority
    @Transactional
    public ClearanceAuthority updateClearanceAuthority(
            Long id,
            ClearanceAuthority authority) {

        ClearanceAuthority existingAuthority =
                clearanceAuthorityRepository.findById(id)
                        .orElseThrow(() ->
                                new ClearanceAuthorityNotFoundException(
                                        "Clearance Authority not found with id: " + id
                                )
                        );

        existingAuthority.setName(authority.getName());

        existingAuthority.setDescription(
                authority.getDescription()
        );

        // Assign Department
        if (authority.getDepartment() != null
                && authority.getDepartment().getId() != null) {

            Long departmentId = authority.getDepartment().getId();

            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Department not found with id: " + departmentId
                            )
                    );

            existingAuthority.setDepartment(department);
        }

        return clearanceAuthorityRepository.save(existingAuthority);
    }

    // Delete Clearance Authority
    public void deleteClearanceAuthority(Long id) {

        ClearanceAuthority existingAuthority =
                clearanceAuthorityRepository.findById(id)
                        .orElseThrow(() ->
                                new ClearanceAuthorityNotFoundException(
                                        "Clearance Authority not found with id: " + id
                                )
                        );

        clearanceAuthorityRepository.delete(existingAuthority);
    }
}