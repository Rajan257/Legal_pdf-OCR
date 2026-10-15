package com.complaint.complaint_management_system.repository;

import com.complaint.complaint_management_system.model.DraftPartyId;
import com.complaint.complaint_management_system.model.Draft_party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Draft_partyRepository extends JpaRepository<Draft_party, DraftPartyId> {

    List<Draft_party> findByNoticeNo(String noticeNo);

    void deleteByNoticeNo(String noticeNo);
}