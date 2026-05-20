package com.att.tdp.issueflow.projects;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByDeletedAtIsNullOrderByIdAsc();

    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    List<Project> findByDeletedAtIsNotNullOrderByIdAsc();
}
