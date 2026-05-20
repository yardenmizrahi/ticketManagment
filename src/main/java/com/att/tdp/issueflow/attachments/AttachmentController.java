package com.att.tdp.issueflow.attachments;

import com.att.tdp.issueflow.auth.CurrentUser;
import com.att.tdp.issueflow.attachments.AttachmentDtos.AttachmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {
    private final AttachmentService attachmentService;
    private final CurrentUser currentUser;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentResponse upload(@PathVariable Long ticketId, @RequestParam MultipartFile file) {
        return attachmentService.upload(ticketId, file, currentUser.get());
    }

    @DeleteMapping("/{attachmentId}")
    public void delete(@PathVariable Long ticketId, @PathVariable Long attachmentId) {
        attachmentService.delete(ticketId, attachmentId, currentUser.get());
    }
}
