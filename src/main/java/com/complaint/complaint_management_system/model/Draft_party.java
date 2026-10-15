package com.complaint.complaint_management_system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "draft_party")
@IdClass(DraftPartyId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Draft_party {

    @Id
    @Column(name = "notice_no", length = 25)
    private String noticeNo;

    @Id
    @Column(name = "type_", length = 2)
    private String type;

    @Id
    @Column(name = "serial_no")
    private Integer serialNo;

    @Column(name = "est_code", length = 10)
    private String estCode;

    @Column(name = "name_", columnDefinition = "TEXT", nullable = false)
    private String name;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "district", length = 50)
    private String district;

    @CreationTimestamp
    @Column(name = "create_modify", updatable = false)
    private LocalDateTime createModify;
}