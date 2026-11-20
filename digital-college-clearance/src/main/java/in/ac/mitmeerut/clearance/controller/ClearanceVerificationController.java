package in.ac.mitmeerut.clearance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import in.ac.mitmeerut.clearance.dto.ClearanceVerificationDTO;
import in.ac.mitmeerut.clearance.entity.ClearanceVerification;
import in.ac.mitmeerut.clearance.service.ClearanceVerificationService;

@RestController
@RequestMapping("/clearance-verifications")
public class ClearanceVerificationController {

    private final ClearanceVerificationService clearanceVerificationService;

    public ClearanceVerificationController(
            ClearanceVerificationService clearanceVerificationService) {

        this.clearanceVerificationService = clearanceVerificationService;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<ClearanceVerificationDTO> createClearanceVerification(
            @RequestBody ClearanceVerification verification) {

        ClearanceVerificationDTO savedVerification =
                clearanceVerificationService.saveClearanceVerification(verification);

        return new ResponseEntity<>(
                savedVerification,
                HttpStatus.CREATED
        );
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<ClearanceVerificationDTO>>
            getAllClearanceVerifications() {

        return ResponseEntity.ok(
                clearanceVerificationService.getAllClearanceVerifications()
        );
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<ClearanceVerificationDTO>
            getClearanceVerificationById(@PathVariable Long id) {

        return ResponseEntity.ok(
                clearanceVerificationService.getClearanceVerificationById(id)
        );
    }

    // GET BY CLEARANCE REQUEST ID
    @GetMapping("/request/{requestId}")
    public ResponseEntity<List<ClearanceVerificationDTO>>
            getVerificationsByRequestId(@PathVariable Long requestId) {

        return ResponseEntity.ok(
                clearanceVerificationService.getVerificationsByRequestId(requestId)
        );
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<ClearanceVerificationDTO> updateClearanceVerification(
            @PathVariable Long id,
            @RequestBody ClearanceVerification verification) {

        return ResponseEntity.ok(
                clearanceVerificationService.updateClearanceVerification(
                        id, verification
                )
        );
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteClearanceVerification(
            @PathVariable Long id) {

        clearanceVerificationService.deleteClearanceVerification(id);

        return ResponseEntity.ok(
                "Clearance verification deleted successfully"
        );
    }
}