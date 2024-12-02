package com.example.FreelanceHub.controllers;

import org.springframework.stereotype.Controller;
import com.example.FreelanceHub.models.ClientJob;
import com.example.FreelanceHub.models.Freelancer;
import com.example.FreelanceHub.models.FreelancerJob;
import com.example.FreelanceHub.repositories.ClientJobRepository;
import com.example.FreelanceHub.repositories.FreeJobRepository;
import com.example.FreelanceHub.repositories.FreelancerRepository;
import com.example.FreelanceHub.services.ClientService;
import com.example.FreelanceHub.services.FreelancerService;

import com.example.FreelanceHub.services.JobService;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Controller
public class FreelancerController {
	
	@Autowired
    private ClientService clientService;

    @Autowired
    private FreelancerService freeService;
    
    @Autowired
    private HttpSession session;

    @Autowired
    private ClientJobRepository clientJobRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private FreelancerRepository freelancerRepository;

    @Autowired
    private FreeJobRepository freeJobRepository;
    
	@GetMapping("/apply")
    public String showApplyJob(@RequestParam Integer id, Model model, HttpSession session) {
        Optional<ClientJob> optionalJob = clientJobRepository.findById(id);

        if (optionalJob.isEmpty()) {
            model.addAttribute("error", "Job not found.");
            return "error";
        }

        ClientJob job = optionalJob.get();
        String freelancerId = (String) session.getAttribute("freelancerId");
        Freelancer freelancer = freelancerRepository.findByFreeId(freelancerId).orElse(null);
        if (freelancer == null) {
            model.addAttribute("error", "Freelancer not found.");
            return "error";
        }

        List<String> jobSkills = job.getSkillsAsList();
        List<String> freelancerSkills = freelancer.getSkillsAsList();
        List<String> missingSkills = jobSkills.stream()
                .filter(skill -> !freelancerSkills.contains(skill))
                .toList();

        // Calculate matched skills
        long matchedSkillsCount = jobSkills.stream()
                .filter(freelancerSkills::contains)
                .count();
        int matchedPercentage = (int) ((matchedSkillsCount * 100.0) / jobSkills.size());

        // Pass all the necessary values to the model
        model.addAttribute("job", job);
        model.addAttribute("salaryMin", job.getCostMin());
        model.addAttribute("salaryMax", job.getCostMax());
        model.addAttribute("durationMin", job.getDurMin());
        model.addAttribute("durationMax", job.getDurMax());
        model.addAttribute("experienceMin", job.getExpMin());
        model.addAttribute("experienceMax", 50); // You can set this as per your requirement
        model.addAttribute("matchedSkillsPercentage", matchedPercentage);
        model.addAttribute("missingSkills", missingSkills);

        return "applyjob";
    }


    @PostMapping("/apply")
    public String handleJobSubmission(
            @RequestParam("salary") long salary,
            @RequestParam("duration") int duration,
            @RequestParam("experience") int experience,
            @RequestParam("previousWorks") MultipartFile[] previousWorks,
            @RequestParam("jobId") Integer jobId,
            Model model,
            HttpSession session) {

        // Retrieve job details
        Optional<ClientJob> optionalJob = clientJobRepository.findById(jobId);
        if (optionalJob.isEmpty()) {
            model.addAttribute("error", "Job not found.");
            return "error";
        }
        ClientJob job = optionalJob.get();

        // Retrieve freelancer details from session
        String freelancerId = (String) session.getAttribute("freelancerId");
        if (freelancerId == null) {
            model.addAttribute("error", "Freelancer session is invalid.");
            return "error";
        }
        Freelancer freelancer = freelancerRepository.findByFreeId(freelancerId).orElse(null);
        if (freelancer == null) {
            model.addAttribute("error", "Freelancer not found.");
            return "error";
        }

        // Match skills
        List<String> jobSkills = job.getSkillsAsList();
        List<String> freelancerSkills = freelancer.getSkillsAsList();
        long matchedSkillsCount = jobSkills.stream()
                .filter(freelancerSkills::contains)
                .count();
        int matchedPercentage = (int) ((matchedSkillsCount * 100.0) / jobSkills.size());

        // Save FreelancerJob
        FreelancerJob freelancerJob = new FreelancerJob(salary, duration, experience, matchedPercentage, "ongoing");
        freelancerJob.setJobId(job); // Set the ClientJob
        freelancerJob.setFreeId(freelancer); // Set the Freelancer
        try {
            freeJobRepository.save(freelancerJob);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to save job application: " + e.getMessage());
            return "error";
        }

        // Prepare attributes for Thymeleaf
        model.addAttribute("job", freelancerJob); // Pass the saved job
        model.addAttribute("clientName", job.getClientName());
        model.addAttribute("status", "pending");

        // Forward to the new page
        return "redirect:/applied-jobs";
    }



    @GetMapping("/applied-jobs")
    public String getAppliedJobs(Model model) {
        String freelancerId = (String) session.getAttribute("freelancerId");
        if (freelancerId == null) {
            return "error";
        }

        // Fetch the applied jobs by freelancer ID
        List<FreelancerJob> appliedJobs = jobService.getJobsByFreelancer(freelancerId);
        System.out.println(appliedJobs);

        if (appliedJobs == null || appliedJobs.isEmpty()) {
            appliedJobs = new ArrayList<>();
        }

        model.addAttribute("appliedJobs", appliedJobs);
        return "appliedjobs";
    }

    @GetMapping("/accepted-jobs")
    public String getAcceptedJobs(Model model) {
        String freelancerId = (String) session.getAttribute("freelancerId");
        if (freelancerId == null) {
            return "error"; // Redirect to an error page if session is invalid
        }

        // Fetch accepted jobs for the freelancer
        List<FreelancerJob> acceptedJobs = jobService.getAcceptedJobsByFreelancer(freelancerId);

        if (acceptedJobs == null || acceptedJobs.isEmpty()) {
            acceptedJobs = new ArrayList<>(); // To avoid null pointer issues in Thymeleaf
        }

        model.addAttribute("acceptedJobs", acceptedJobs);
        return "acceptedjobs"; // Thymeleaf view name
    }

    @PostMapping("/upload-project")
    public String uploadProject(
            @RequestParam("jobId") Integer jobId,
            @RequestParam("file") MultipartFile file) {
        try {
            // Save the file and update the status
            jobService.uploadProject(jobId, file);
            return "redirect:/accepted-jobs"; // Redirect back to the accepted jobs page
        } catch (Exception e) {
            e.printStackTrace();
            return "error"; // Handle errors appropriately
        }
    }




    @GetMapping("/search")
    public ResponseEntity<List<ClientJob>> searchJobs(@RequestParam String query) {
        List<ClientJob> jobs = jobService.searchJobs(query);

        if (jobs.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(jobs);
    }
    @GetMapping("/explore")
    public String showExplorePage(Model model){
        List<ClientJob> clientJobs = clientJobRepository.findAll();
        model.addAttribute("clientJobs", clientJobs);
        String role = (String) session.getAttribute("role");
        model.addAttribute("role", role);
        return "explore";
    }
}
