package com.complaint.complaint_management_system.controller;

import com.complaint.complaint_management_system.model.Draft_party;
import com.complaint.complaint_management_system.service.DraftPartyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS, RequestMethod.DELETE, RequestMethod.PUT})
@RequestMapping("/api/draft-parties")
public class Draft_partyController {

    @Autowired
    private DraftPartyService draftPartyService;

    @GetMapping
    public List<Draft_party> getAllDraftParties() {
        return draftPartyService.getAllDraftParties();
    }

    @GetMapping("/by-notice/{noticeNo}")
    public List<Draft_party> getPartiesByNoticeNo(@PathVariable String noticeNo) {
        return draftPartyService.getPartiesByNoticeNo(noticeNo);
    }

    @PostMapping
    public Draft_party saveDraftParty(@RequestBody Draft_party draftParty) {
        return draftPartyService.saveDraftParty(draftParty);
    }
}