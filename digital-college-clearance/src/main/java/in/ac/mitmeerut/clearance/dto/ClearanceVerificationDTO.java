package in.ac.mitmeerut.clearance.dto;

import java.time.LocalDateTime;

import in.ac.mitmeerut.clearance.enums.ClearanceStatus;

public class ClearanceVerificationDTO {

    private Long id;
    private Long requestId;
    private String authorityName;
    private String studentName;
    private String rollNumber;
    private ClearanceStatus status;
    private String remarks;
    private String verifiedBy;
    private LocalDateTime verifiedDate;

    public ClearanceVerificationDTO() {
    }

    public ClearanceVerificationDTO(
            Long id,
            Long requestId,
            String authorityName,
            String studentName,
            String rollNumber,
            ClearanceStatus status,
            String remarks,
            String verifiedBy,
            LocalDateTime verifiedDate) {

        this.id = id;
        this.requestId = requestId;
        this.authorityName = authorityName;
        this.studentName = studentName;
        this.rollNumber = rollNumber;
        this.status = status;
        this.remarks = remarks;
        this.verifiedBy = verifiedBy;
        this.verifiedDate = verifiedDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getAuthorityName() {
        return authorityName;
    }

    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public ClearanceStatus getStatus() {
        return status;
    }

    public void setStatus(ClearanceStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getVerifiedDate() {
        return verifiedDate;
    }

    public void setVerifiedDate(LocalDateTime verifiedDate) {
        this.verifiedDate = verifiedDate;
    }
}