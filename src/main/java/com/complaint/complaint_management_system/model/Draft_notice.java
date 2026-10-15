package com.complaint.complaint_management_system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "draft_notice")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Draft_notice {

    @Id
    @Column(name = "notice_no", length = 25)
    private String noticeNo;

    @Column(name = "est_code", length = 10)
    private String estCode;

    @Column(name = "prayer_details", columnDefinition = "TEXT")
    private String prayerDetails;

    @Column(name = "crime_no", length = 10)
    private String crimeNo;

    @Column(name = "crime_year")
    private Integer crimeYear;

    @Column(name = "st_no")
    private Integer stNo;

    @Column(name = "st_year")
    private Integer stYear;

    @Column(name = "police_stn", length = 60)
    private String policeStn;

    @Column(name = "district", length = 50)
    private String district;

    @Column(name = "order_no", length = 20)
    private String orderNo;

    @Column(name = "order_details", length = 100)
    private String orderDetails;

    @Column(name = "passed_by")
    private LocalDate passedBy;

    @Column(name = "trial_court_name", length = 50)
    private String trialCourtName;

    // OCR raw output stored for auditing / re-processing
    @Column(name = "ocr_text", columnDefinition = "LONGTEXT")
    private String ocrText;

    // Parsed case title (petitioner vs respondent heading)
    @Column(name = "case_title", columnDefinition = "TEXT")
    private String caseTitle;

    @CreationTimestamp
    @Column(name = "create_modify", updatable = false)
    private LocalDateTime createModify;
}
