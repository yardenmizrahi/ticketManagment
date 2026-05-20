package com.att.tdp.issueflow.attachments;

import com.att.tdp.issueflow.audit.AuditService;
import com.att.tdp.issueflow.attachments.AttachmentDtos.AttachmentResponse;
import com.att.tdp.issueflow.common.ApiException;
import com.att.tdp.issueflow.common.Require;
import com.att.tdp.issueflow.tickets.Ticket;
import com.att.tdp.issueflow.tickets.TicketService;
import com.att.tdp.issueflow.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AttachmentService {
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "application/pdf",
            "text/plain"
    );

    private final AttachmentRepository attachmentRepository;
    private final TicketService ticketService;
    private final AuditService auditService;

    @Transactional
    public AttachmentResponse upload(Long ticketId, MultipartFile file, User actor) {
        Ticket ticket = ticketService.loadActive(ticketId);
        Require.valid(!file.isEmpty(), "Attachment file is required");
        Require.valid(file.getSize() <= MAX_SIZE_BYTES, "Attachment exceeds 10 MB limit");
        Require.valid(ALLOWED_TYPES.contains(file.getContentType()), "Unsupported attachment content type");

        try {
            Attachment attachment = new Attachment();
            attachment.setTicket(ticket);
            attachment.setFilename(file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename());
            attachment.setContentType(file.getContentType());
            attachment.setSizeBytes(file.getSize());
            attachment.setContent(file.getBytes());
            Attachment saved = attachmentRepository.save(attachment);
            auditService.user("UPLOAD_ATTACHMENT", "TICKET", ticketId, actor);
            return AttachmentResponse.from(saved);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read attachment");
        }
    }

    @Transactional
    public void delete(Long ticketId, Long attachmentId, User actor) {
        ticketService.loadActive(ticketId);
        Attachment attachment = Require.found(
                attachmentRepository.findByIdAndTicketId(attachmentId, ticketId), "Attachment not found");
        attachmentRepository.delete(attachment);
        auditService.user("DELETE_ATTACHMENT", "TICKET", ticketId, actor);
    }
}
