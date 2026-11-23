package in.ac.mitmeerut.clearance.service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import in.ac.mitmeerut.clearance.dto.ClearanceVerificationDTO;
import in.ac.mitmeerut.clearance.entity.ClearanceAuthority;
import in.ac.mitmeerut.clearance.entity.ClearanceRequest;
import in.ac.mitmeerut.clearance.entity.Student;
import in.ac.mitmeerut.clearance.enums.ClearanceStatus;

@Service
public class CertificateService {

    private final ClearanceRequestService clearanceRequestService;
    private final ClearanceVerificationService clearanceVerificationService;
    private final ClearanceAuthorityService clearanceAuthorityService;

    public CertificateService(
            ClearanceRequestService clearanceRequestService,
            ClearanceVerificationService clearanceVerificationService,
            ClearanceAuthorityService clearanceAuthorityService) {

        this.clearanceRequestService = clearanceRequestService;
        this.clearanceVerificationService = clearanceVerificationService;
        this.clearanceAuthorityService = clearanceAuthorityService;
    }

    public byte[] generateCertificate(Long requestId) {

        ClearanceRequest request =
                clearanceRequestService.getClearanceRequestById(requestId);

        // Check overall clearance status
        if (request.getOverallStatus() != ClearanceStatus.CLEARED) {
            throw new IllegalStateException(
                    "Certificate cannot be generated. Clearance is not completed.");
        }

        List<ClearanceAuthority> authorities =
                clearanceAuthorityService.getAllClearanceAuthorities();

        List<ClearanceVerificationDTO> verifications =
                clearanceVerificationService.getVerificationsByRequestId(requestId);

        // Verify that every required authority has approved
        boolean allCleared = !authorities.isEmpty()
                && authorities.size() == verifications.size()
                && authorities.stream().allMatch(authority ->
                    verifications.stream().filter(verification ->
                        authority.getName().equals(verification.getAuthorityName())
                        && verification.getStatus() == ClearanceStatus.CLEARED
                    ).count() == 1
                );

        if (!allCleared) {
            throw new IllegalStateException(
                    "All department approvals are required before downloading the certificate.");
        }

        Student student = request.getStudent();

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4, 45, 45, 45, 45);

            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Fonts
            Font collegeFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 22);

            Font subtitleFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 12);

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 20);

            Font normalFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 11);

            Font boldFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 11);

            // College heading
            Paragraph collegeName = new Paragraph(
                    "MEERUT INSTITUTE OF TECHNOLOGY", collegeFont);

            collegeName.setAlignment(Element.ALIGN_CENTER);
            document.add(collegeName);

            Paragraph collegeAddress = new Paragraph(
                    "Meerut, Uttar Pradesh", subtitleFont);

            collegeAddress.setAlignment(Element.ALIGN_CENTER);
            document.add(collegeAddress);

            document.add(new Paragraph(" "));

            // Certificate title
            Paragraph title = new Paragraph(
                    "COLLEGE CLEARANCE CERTIFICATE", titleFont);

            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));

            Paragraph introduction = new Paragraph(
                    "This is to certify that the following student has "
                    + "successfully completed all required departmental "
                    + "clearance formalities.", normalFont);

            introduction.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(introduction);

            document.add(new Paragraph(" "));

            // Student details table
            PdfPTable studentTable = new PdfPTable(2);
            studentTable.setWidthPercentage(100);
            studentTable.setWidths(new float[]{30, 70});

            addDetail(studentTable, "Student Name",
                    student.getUser().getName(), boldFont, normalFont);

            addDetail(studentTable, "Roll Number",
                    student.getRollNumber(), boldFont, normalFont);

            addDetail(studentTable, "Course",
                    student.getCourse(), boldFont, normalFont);

            addDetail(studentTable, "Branch",
                    student.getBranch(), boldFont, normalFont);

            addDetail(studentTable, "Clearance Request ID",
                    String.valueOf(request.getId()), boldFont, normalFont);

            document.add(studentTable);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Departmental Clearance Details", boldFont));
            document.add(new Paragraph(" "));

            // Department approval table
            PdfPTable approvalTable = new PdfPTable(3);
            approvalTable.setWidthPercentage(100);
            approvalTable.setWidths(new float[]{35, 30, 35});

            addHeader(approvalTable, "Department", boldFont);
            addHeader(approvalTable, "Status", boldFont);
            addHeader(approvalTable, "Verified Date", boldFont);

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("dd MMM yyyy");

            for (ClearanceAuthority authority : authorities) {

                ClearanceVerificationDTO verification =
                        verifications.stream()
                                .filter(v -> authority.getName()
                                        .equals(v.getAuthorityName()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                        "Department approval record is missing."));

                approvalTable.addCell(
                        new Phrase(authority.getName(), normalFont));

                approvalTable.addCell(
                        new Phrase("CLEARED", normalFont));

                String verifiedDate = verification.getVerifiedDate() != null
                        ? verification.getVerifiedDate().format(formatter)
                        : "Approved";

                approvalTable.addCell(
                        new Phrase(verifiedDate, normalFont));
            }

            document.add(approvalTable);

            document.add(new Paragraph(" "));

            // Issue date
            LocalDateTime issueDate = request.getCompletionDate() != null
                    ? request.getCompletionDate()
                    : LocalDateTime.now();

            Paragraph dateParagraph = new Paragraph(
                    "Certificate Issue Date: "
                    + issueDate.format(formatter), normalFont);

            document.add(dateParagraph);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Signature section
            PdfPTable signatureTable = new PdfPTable(2);
            signatureTable.setWidthPercentage(100);

            PdfPCell studentSignature = new PdfPCell(
                    new Phrase("____________________\nStudent Signature",
                            normalFont));

            PdfPCell authoritySignature = new PdfPCell(
                    new Phrase("____________________\nAuthorized Signature",
                            normalFont));

            studentSignature.setBorder(PdfPCell.NO_BORDER);
            authoritySignature.setBorder(PdfPCell.NO_BORDER);

            signatureTable.addCell(studentSignature);
            signatureTable.addCell(authoritySignature);

            document.add(signatureTable);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error while generating clearance certificate.", e);
        }
    }

    private void addDetail(
            PdfPTable table,
            String label,
            String value,
            Font boldFont,
            Font normalFont) {

        table.addCell(new Phrase(label, boldFont));
        table.addCell(new Phrase(
                value != null ? value : "-", normalFont));
    }

    private void addHeader(
            PdfPTable table,
            String heading,
            Font font) {

        PdfPCell cell = new PdfPCell(new Phrase(heading, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}