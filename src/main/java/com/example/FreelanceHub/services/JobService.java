package com.example.FreelanceHub.services;

import com.example.FreelanceHub.models.ClientJob;
import com.example.FreelanceHub.models.Freelancer;
import com.example.FreelanceHub.models.FreelancerJob;
import com.example.FreelanceHub.repositories.ClientJobRepository;
import com.example.FreelanceHub.repositories.FreeJobRepository;
import com.example.FreelanceHub.repositories.FreelancerRepository;
import com.example.FreelanceHub.repositories.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class JobService {

    @Autowired
    private ClientJobRepository clientJobRepository;

    @Autowired
    private FreeJobRepository freelancerJobsRepository;

    @Autowired
    private FreelancerRepository freelancerRepository;

    @Autowired
    private JobRepository jobRepository;

    public List<FreelancerJob> getJobsByFreelancer(String freelancerId) {
        return freelancerJobsRepository.findJobsByFreelancerId(freelancerId,"ongoing");
    }

    public List<FreelancerJob> getAcceptedJobsByFreelancer(String freelancerId) {
        return freelancerJobsRepository.findByFreeIdAndStatus(freelancerId, "accepted");
    }

    public void uploadProject(Integer jobId, MultipartFile file) throws Exception {
        // Save the file to a directory (or cloud storage)
//        String uploadDir = "/path/to/upload/directory/";
//        file.transferTo(new java.io.File(uploadDir + file.getOriginalFilename()));

        // Update the progress status in the database
        System.out.println("Updating job with ID: " + jobId);
        jobRepository.updateJobStatus(jobId, "Unverified");
    }

    public List<ClientJob> searchJobs(String query){
        return clientJobRepository.searchJobs(query);
    }
}
