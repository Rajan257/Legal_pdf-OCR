package com.complaint.complaint_management_system.repository;


import com.complaint.complaint_management_system.model.Draft_notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Draft_noticeRepository
        extends JpaRepository<Draft_notice, String> {
}
