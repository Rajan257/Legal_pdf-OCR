package com.complaint.complaint_management_system.service;

import com.complaint.complaint_management_system.dto.ExtractedComplaintData;
import com.complaint.complaint_management_system.model.Draft_notice;
import com.complaint.complaint_management_system.model.Draft_party;
import com.complaint.complaint_management_system.repository.Draft_noticeRepository;
import com.complaint.complaint_management_system.repository.Draft_partyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DraftNoticeServiceImpl implements DraftNoticeService {

    @Autowired
    private Draft_noticeRepository draftNoticeRepository;

    @Autowired
    private Draft_partyRepository draftPartyRepository;

    @Override
    public Draft_notice saveDraftNotice(Draft_notice draftNotice) {
        return draftNoticeRepository.save(draftNotice);
    }

    @Transactional
    @Override
    public Draft_notice saveExtractedComplaintData(ExtractedComplaintData data) {
        if (data == null) {
            throw new IllegalArgumentException("Complaint data cannot be null");
        }

        String noticeNo = data.getNotice_no();
        if (noticeNo == null || noticeNo.isBlank() || "Not found".equalsIgnoreCase(noticeNo.trim())) {
            throw new IllegalArgumentException("A valid Notice Number is required to save to database.");
        }
        noticeNo = noticeNo.trim();

        Draft_notice notice = new Draft_notice();
        notice.setNoticeNo(noticeNo);
        notice.setEstCode(data.getEst_code());
        notice.setPrayerDetails(data.getPrayer());
        notice.setDistrict(data.getDistrict());
        notice.setPoliceStn(data.getPoliceStation());
        notice.setCrimeNo(data.getCrimeNo());
        notice.setOrderNo(data.getOrder_no());
        notice.setOrderDetails(data.getOrder_details());
        notice.setTrialCourtName(data.getTrial_court_name());

        notice.setCrimeYear(parseIntegerSafe(data.getCrimeYear()));
        notice.setStNo(parseIntegerSafe(data.getStNo()));
        notice.setStYear(parseIntegerSafe(data.getStYear()));

        Draft_notice savedNotice = draftNoticeRepository.save(notice);

        if (data.getParties() != null && !data.getParties().isEmpty()) {
            draftPartyRepository.deleteByNoticeNo(noticeNo);

            int pIndex = 1;
            int rIndex = 1;
            for (Draft_party party : data.getParties()) {
                party.setNoticeNo(noticeNo);
                party.setEstCode(data.getEst_code());
                String type = party.getType() != null && !party.getType().isBlank()
                        ? party.getType().trim().toUpperCase()
                        : "P";
                party.setType(type);
                if ("P".equalsIgnoreCase(type)) {
                    party.setSerialNo(pIndex++);
                } else {
                    party.setSerialNo(rIndex++);
                }
            }
            draftPartyRepository.saveAll(data.getParties());
        }

        return savedNotice;
    }

    private Integer parseIntegerSafe(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            String digits = str.replaceAll("[^0-9]", "").trim();
            return digits.isEmpty() ? null : Integer.parseInt(digits);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Draft_notice> getAllDraftNotices() {
        return draftNoticeRepository.findAll();
    }

    @Override
    public Draft_notice getDraftNoticeById(String noticeNo) {
        return draftNoticeRepository.findById(noticeNo)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Draft Notice not found with noticeNo: " + noticeNo
                        ));
    }

    @Override
    public void deleteDraftNotice(String noticeNo) {
        draftPartyRepository.deleteByNoticeNo(noticeNo);
        draftNoticeRepository.deleteById(noticeNo);
    }
}