package com.complaint.complaint_management_system.service;

import com.complaint.complaint_management_system.dto.ExtractedComplaintData;
import com.complaint.complaint_management_system.model.Draft_notice;

import java.util.List;

public interface DraftNoticeService {

    Draft_notice saveDraftNotice(Draft_notice draftNotice);

    Draft_notice saveExtractedComplaintData(ExtractedComplaintData data);

    List<Draft_notice> getAllDraftNotices();

    Draft_notice getDraftNoticeById(String noticeNo);

    void deleteDraftNotice(String noticeNo);
}