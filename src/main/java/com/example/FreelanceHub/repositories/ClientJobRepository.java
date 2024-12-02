package com.example.FreelanceHub.repositories;

import com.example.FreelanceHub.models.ClientJob;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientJobRepository extends JpaRepository<ClientJob, Integer> {
	List<ClientJob> findByClientId(String clientId);
	ClientJob findById(int JobId);
	
	@Query("SELECT cj FROM ClientJob cj WHERE " +
            "LOWER(cj.JobTitle) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(cj.jobDesc) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(cj.SkillReq) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ClientJob> searchJobs(@Param("query") String query);
}
