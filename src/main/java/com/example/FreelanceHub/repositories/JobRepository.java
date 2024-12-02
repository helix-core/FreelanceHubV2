package com.example.FreelanceHub.repositories;

import com.example.FreelanceHub.models.Client;
import com.example.FreelanceHub.models.Jobs;

import jakarta.transaction.Transactional;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface JobRepository extends JpaRepository<Jobs, Integer> {
	List<Jobs> findByClientIdAndProgress(Client clientId, String progress);
	
	@Transactional
    @Modifying
    @Query("UPDATE Jobs j SET j.progress = :progress WHERE j.jobId.JobId = :jobId")
    void updateJobStatus(@Param("jobId") Integer jobId, @Param("progress") String progress);
}