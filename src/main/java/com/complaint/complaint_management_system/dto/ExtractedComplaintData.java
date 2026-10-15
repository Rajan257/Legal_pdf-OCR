package com.complaint.complaint_management_system.dto;

import com.complaint.complaint_management_system.model.Draft_party;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedComplaintData {

    private String ocrText;

    private String est_code;

    private String notice_no;

    private String prayer;

    private List<Draft_party> parties;

    private String stNo;
    private String stYear;
    private String policeStation;
    private String district;
    private String crimeYear;
    private String crimeNo;


    private String order_no;
    private String order_details;
    private String case_title;
    private String trial_court_name;
    private String passed_by;


}