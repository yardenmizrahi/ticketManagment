package com.att.tdp.issueflow.attachments;

public final class AttachmentDtos {
    private AttachmentDtos() {
    }

    public record AttachmentResponse(Long id, Long ticketId, String filename, String contentType) {
        public static AttachmentResponse from(Attachment attachment) {
            return new AttachmentResponse(
                    attachment.getId(),
                    attachment.getTicket().getId(),
                    attachment.getFilename(),
                    attachment.getContentType());
        }
    }
}
