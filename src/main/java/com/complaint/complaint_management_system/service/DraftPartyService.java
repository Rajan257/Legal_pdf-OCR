package com.complaint.complaint_management_system.service;

import com.complaint.complaint_management_system.model.DraftPartyId;
import com.complaint.complaint_management_system.model.Draft_party;

import java.util.List;

public interface DraftPartyService {

    Draft_party saveDraftParty(Draft_party draftParty);

    List<Draft_party> getAllDraftParties();

    List<Draft_party> getPartiesByNoticeNo(String noticeNo);

    Draft_party getDraftPartyById(DraftPartyId partyId);

    void deleteDraftParty(DraftPartyId partyId);
}