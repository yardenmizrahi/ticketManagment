package com.att.tdp.issueflow.tickets;

import com.att.tdp.issueflow.auth.CurrentUser;
import com.att.tdp.issueflow.tickets.TicketDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<TicketResponse> findByProject(@RequestParam Long projectId) {
        return ticketService.findByProject(projectId);
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TicketResponse> deleted(@RequestParam Long projectId) {
        return ticketService.deleted(projectId);
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(@RequestParam Long projectId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(ticketService.exportCsv(projectId));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CsvImportResponse importCsv(@RequestParam MultipartFile file, @RequestParam Long projectId) {
        return ticketService.importCsv(file, projectId, currentUser.get());
    }

    @GetMapping("/{ticketId}")
    public TicketResponse findById(@PathVariable Long ticketId) {
        return ticketService.findById(ticketId);
    }

    @PostMapping
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request) {
        return ticketService.create(request, currentUser.get());
    }

    @PatchMapping("/{ticketId}")
    public void update(@PathVariable Long ticketId, @Valid @RequestBody UpdateTicketRequest request) {
        ticketService.update(ticketId, request, currentUser.get());
    }

    @DeleteMapping("/{ticketId}")
    public void delete(@PathVariable Long ticketId) {
        ticketService.delete(ticketId, currentUser.get());
    }

    @PostMapping("/{ticketId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public void restore(@PathVariable Long ticketId) {
        ticketService.restore(ticketId, currentUser.get());
    }

    @PostMapping("/{ticketId}/dependencies")
    public void addDependency(@PathVariable Long ticketId, @Valid @RequestBody DependencyRequest request) {
        ticketService.addDependency(ticketId, request.blockedBy(), currentUser.get());
    }

    @GetMapping("/{ticketId}/dependencies")
    public List<DependencyResponse> listDependencies(@PathVariable Long ticketId) {
        return ticketService.listDependencies(ticketId);
    }

    @DeleteMapping("/{ticketId}/dependencies/{blockerId}")
    public void removeDependency(@PathVariable Long ticketId, @PathVariable Long blockerId) {
        ticketService.removeDependency(ticketId, blockerId, currentUser.get());
    }
}
