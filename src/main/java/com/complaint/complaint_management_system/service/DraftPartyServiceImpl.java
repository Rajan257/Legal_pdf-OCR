package com.complaint.complaint_management_system.service;

import com.complaint.complaint_management_system.model.DraftPartyId;
import com.complaint.complaint_management_system.model.Draft_party;
import com.complaint.complaint_management_system.repository.Draft_partyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DraftPartyServiceImpl implements DraftPartyService {

    @Autowired
    private Draft_partyRepository draftPartyRepository;

    @Override
    public Draft_party saveDraftParty(Draft_party draftParty) {
        return draftPartyRepository.save(draftParty);
    }

    @Override
    public List<Draft_party> getAllDraftParties() {
        return draftPartyRepository.findAll();
    }

    @Override
    public List<Draft_party> getPartiesByNoticeNo(String noticeNo) {
        return draftPartyRepository.findByNoticeNo(noticeNo);
    }

    @Override
    public Draft_party getDraftPartyById(DraftPartyId partyId) {
        return draftPartyRepository.findById(partyId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Draft Party not found with partyId: " + partyId
                        ));
    }

    @Override
    public void deleteDraftParty(DraftPartyId partyId) {
        draftPartyRepository.deleteById(partyId);
    }
}

