package com.complaint.complaint_management_system.controller;

import com.complaint.complaint_management_system.dto.ExtractedComplaintData;
import com.complaint.complaint_management_system.model.Draft_notice;
import com.complaint.complaint_management_system.service.DraftNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS, RequestMethod.DELETE, RequestMethod.PUT})
@RequestMapping("/api/draft-notices")
public class Draft_noticeController {

    @Autowired
    private DraftNoticeService draftNoticeService;

    @GetMapping
    public List<Draft_notice> getAllDraftNotices() {
        return draftNoticeService.getAllDraftNotices();
    }

    @GetMapping("/{noticeNo}")
    public ResponseEntity<?> getDraftNoticeById(@PathVariable String noticeNo) {
        try {
            return ResponseEntity.ok(draftNoticeService.getDraftNoticeById(noticeNo));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public Draft_notice saveDraftNotice(@RequestBody Draft_notice draftNotice) {
        return draftNoticeService.saveDraftNotice(draftNotice);
    }

    @PostMapping("/save-extracted")
    public ResponseEntity<?> saveExtractedData(@RequestBody ExtractedComplaintData data) {
        try {
            Draft_notice saved = draftNoticeService.saveExtractedComplaintData(data);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error saving to database: " + e.getMessage());
        }
    }

    @DeleteMapping("/{noticeNo}")
    public ResponseEntity<?> deleteDraftNotice(@PathVariable String noticeNo) {
        draftNoticeService.deleteDraftNotice(noticeNo);
        return ResponseEntity.ok().build();
    }
}