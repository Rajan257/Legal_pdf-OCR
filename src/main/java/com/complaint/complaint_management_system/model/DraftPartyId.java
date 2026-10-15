package com.complaint.complaint_management_system.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftPartyId implements Serializable {

    private String noticeNo;
    private String type;
    private Integer serialNo;
}
