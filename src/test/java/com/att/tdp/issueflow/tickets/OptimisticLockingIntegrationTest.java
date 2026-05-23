package com.att.tdp.issueflow.tickets;

import com.att.tdp.issueflow.projects.Project;
import com.att.tdp.issueflow.projects.ProjectRepository;
import com.att.tdp.issueflow.users.User;
import com.att.tdp.issueflow.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OptimisticLockingIntegrationTest {
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    TicketRepository ticketRepository;

    @Test
    void staleTicketUpdateIsRejectedByJpaVersion() {
        Long ticketId = transactionTemplate.execute(status -> {
            User admin = userRepository.findByUsernameIgnoreCase("admin").orElseThrow();
            Project project = new Project();
            project.setName("Concurrency Project");
            project.setOwner(admin);
            Project savedProject = projectRepository.save(project);

            Ticket ticket = new Ticket();
            ticket.setTitle("Original");
            ticket.setDescription("Original description");
            ticket.setProject(savedProject);
            ticket.setStatus(TicketStatus.TODO);
            ticket.setPriority(TicketPriority.HIGH);
            ticket.setType(TicketType.BUG);
            return ticketRepository.save(ticket).getId();
        });

        Ticket staleCopy = transactionTemplate.execute(status -> ticketRepository.findById(ticketId).orElseThrow());

        transactionTemplate.executeWithoutResult(status -> {
            Ticket freshCopy = ticketRepository.findById(ticketId).orElseThrow();
            freshCopy.setTitle("Fresh update");
        });

        staleCopy.setTitle("Stale update");

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> ticketRepository.saveAndFlush(staleCopy)))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
