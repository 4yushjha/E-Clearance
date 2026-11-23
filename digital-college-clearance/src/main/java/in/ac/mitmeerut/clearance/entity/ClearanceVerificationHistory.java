package in.ac.mitmeerut.clearance.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import in.ac.mitmeerut.clearance.enums.ClearanceStatus;

@Entity
@Table(name = "clearance_verification_history")
public class ClearanceVerificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "verification_id", nullable = false)
    private ClearanceVerification verification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClearanceStatus status;

    private String remarks;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(nullable = false)
    private LocalDateTime actionDate;

    @Column(nullable = false)
    private Integer attemptNumber;

    public ClearanceVerificationHistory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClearanceVerification getVerification() {
        return verification;
    }

    public void setVerification(ClearanceVerification verification) {
        this.verification = verification;
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

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public LocalDateTime getActionDate() {
        return actionDate;
    }

    public void setActionDate(LocalDateTime actionDate) {
        this.actionDate = actionDate;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }
}