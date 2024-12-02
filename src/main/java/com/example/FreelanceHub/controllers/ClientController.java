package com.example.FreelanceHub.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.FreelanceHub.models.Client;
import com.example.FreelanceHub.models.ClientJob;
import com.example.FreelanceHub.models.Freelancer;
import com.example.FreelanceHub.models.FreelancerJob;
import com.example.FreelanceHub.models.Jobs;
import com.example.FreelanceHub.repositories.ClientJobRepository;
import com.example.FreelanceHub.repositories.FreeJobRepository;
import com.example.FreelanceHub.repositories.JobRepository;
import com.example.FreelanceHub.services.ClientJobService;
import com.example.FreelanceHub.services.ClientService;
import com.example.FreelanceHub.services.FreelancerService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ClientController {
	
	@Autowired
    private ClientJobRepository clientJobRepository;
	
	@Autowired
	private FreeJobRepository freelancerJobRepository;
	
	@Autowired
	private JobRepository jobRepository;
		
	@Autowired
	private ClientJobService clientJobService;
	
	@Autowired
	private FreelancerService freeService;
	
	@Autowired
    private ClientService clientService;

    @Autowired
    private HttpSession session;

    // Display the job creation form
    @GetMapping("/postjob")
    public String showJobForm(Model model) {
    	String clientId = (String) session.getAttribute("clientId"); // Get clientId from session
        model.addAttribute("clientId", clientId);
        model.addAttribute("clientJob", new ClientJob());
        return "postjob"; 
    }

    // Handle form submission
    @PostMapping("/postjob")
	public String createJob(@ModelAttribute ClientJob clientJob) {
    	String clientId = (String) session.getAttribute("clientId");
    	clientJob.setClientId(clientId);
    	clientJobRepository.save(clientJob); 
        return "redirect:/posted-jobs"; 
    }
    
    @GetMapping("/posted-jobs")
    public String getPostedJobs(Model model) {
    	String clientId = (String) session.getAttribute("clientId");
    	List<ClientJob> jobs = clientJobService.findByClientId(clientId);
        model.addAttribute("jobs", jobs);
        
        return "postedjobs"; 
    }
    
    @GetMapping("/bidding")
    public String showAllBids(
            @RequestParam(name = "sortBy", required = false, defaultValue = "duration") String sortBy, 
            Model model) {
        String clientId = (String) session.getAttribute("clientId");
        // Fetch all jobs for the client
        List<ClientJob> jobs = clientJobService.findByClientId(clientId);
        if (jobs == null || jobs.isEmpty()) {
            model.addAttribute("jobsWithBids", new ArrayList<>()); // Send an empty list if no jobs are found
            return "bidding";
        }

        // Map to hold jobs and their corresponding freelancer bids
        List<Map<String, Object>> jobsWithBids = jobs.stream()
        		.filter(job -> "pending".equals(job.getJobStat()))
        		.map(job -> {
            // Fetch freelancer bids for the job
            List<FreelancerJob> freelancerBids = freelancerJobRepository.findByJobId(job);

            // Enrich bids with freelancer details
            List<Map<String, Object>> enrichedBids = freelancerBids.stream().map(freelancerJob -> {
                Freelancer freelancer = freelancerJob.getFreeId();
                Map<String, Object> bidData = new HashMap<>();
                bidData.put("freelancerJob", freelancerJob);
                bidData.put("freelancerName", freelancer != null ? freelancer.getFreeName() : "Unknown");
                bidData.put("freelancerJobDuration", freelancerJob.getDuration());
                bidData.put("freelancerJobSalary", freelancerJob.getSalary());
                bidData.put("freelancerJobExp", freelancerJob.getJobExp());
                bidData.put("freelancerSkillMatch", freelancerJob.getSkillMatch());
                return bidData;
            }).collect(Collectors.toList());

            // Sort based on the chosen criterion
            switch (sortBy) {
                case "duration":
                	enrichedBids.sort((bid1, bid2) -> Integer.compare(
                		    (int) bid2.get("freelancerJobDuration"),  // reverse order
                		    (int) bid1.get("freelancerJobDuration")));
                    break;
                case "salary":
                	enrichedBids.sort((bid1, bid2) -> Long.compare(
                		    (long) bid2.get("freelancerJobSalary"),  // reverse order
                		    (long) bid1.get("freelancerJobSalary")));
                    break;
                case "experience":
                	enrichedBids.sort((bid1, bid2) -> Integer.compare(
                		    (int) bid2.get("freelancerJobExp"),  // reverse order
                		    (int) bid1.get("freelancerJobExp")));

                    break;
                case "skillMatch":
                	enrichedBids.sort((bid1, bid2) -> Float.compare(
                		    (float) bid2.get("freelancerSkillMatch"),  // reverse order
                		    (float) bid1.get("freelancerSkillMatch")));
                    break;
                default:
                	enrichedBids.sort((bid1, bid2) -> Integer.compare(
                		    (int) bid2.get("freelancerJobDuration"),  // reverse order
                		    (int) bid1.get("freelancerJobDuration")));
                    break;
            }

            // Create a map containing the job and its bids
            Map<String, Object> jobWithBids = new HashMap<>();
            jobWithBids.put("job", job);
            jobWithBids.put("bids", enrichedBids);

            return jobWithBids;
        }).collect(Collectors.toList());

        model.addAttribute("jobsWithBids", jobsWithBids);
        model.addAttribute("sortBy", sortBy);  // Send the selected sort criterion to the view
        return "bidding";
    }

    @PostMapping("/acceptBid")
    public String acceptBid(@RequestParam("jobId") int jobId,
                            @RequestParam("freelancerId") String freelancerId,
                            Model model) {
        
        String clientId = (String) session.getAttribute("clientId");
        Freelancer freelancer = freeService.findByFreeId(freelancerId);
        
        // Step 1: Fetch the ClientJob and FreelancerJob for the given jobId and freelancerId
        ClientJob clientJob = clientJobRepository.findById(jobId);
        FreelancerJob acceptedBid = freelancerJobRepository.findByJobIdAndFreeId(clientJob, freelancerId);

        if (clientJob == null || acceptedBid == null) {
            model.addAttribute("error", "Invalid job or freelancer.");
            return "bidding";  // Return an error view if job or freelancer is not found
        }

        // Step 2: Create a new Job record (Jobs table) for the accepted freelancer
        Jobs newJob = new Jobs();
        newJob.setClientId(clientJob.getClients());
        newJob.setFreeId(acceptedBid.getFreeId());
        newJob.setJobId(clientJob);
        newJob.setProgress("ongoing");

        // Save the new job entry
        jobRepository.save(newJob);

        // Step 3: Update the ClientJob's status to 'assigned'
        clientJob.setJobStat("assigned");
        clientJobRepository.save(clientJob);  // Save the updated ClientJob

        // Step 4: Update the FreelancerJob's status to 'accepted' and the others to 'rejected'
        acceptedBid.setStatus("accepted");
        freelancerJobRepository.save(acceptedBid);  // Save the accepted freelancer job

        // Update all other freelancer bids for this job to 'rejected'
        List<FreelancerJob> allBids = freelancerJobRepository.findByJobId(clientJob);
        for (FreelancerJob bid : allBids) {
            if (!bid.getFreeId().getFreeId().equals(freelancerId)) {
                bid.setStatus("rejected");
                freelancerJobRepository.save(bid);
            }
        }

        model.addAttribute("success", "Bid accepted successfully!");
        return "redirect:/bidding";  // Redirect to the bidding page after accepting the bid
    }
    
    @GetMapping("/assignedjobs")
    public String getAssignedProjects(Model model, HttpSession session) {
        // Get clientId from session
        String clientId = (String) session.getAttribute("clientId");
        Client client=clientService.findByClientId(clientId);
        
        // Fetch ongoing and completed jobs for the client
        List<Jobs> ongoingJobs = jobRepository.findByClientIdAndProgress(client, "ongoing");
        for(Jobs job: jobRepository.findByClientIdAndProgress(client, "Unverified")){
        	ongoingJobs.add(job);
        }
        List<Jobs> completedJobs = jobRepository.findByClientIdAndProgress(client, "completed");

     // Prepare additional details (duration and salary)
        Map<String, Map<String, Object>> jobDetails = new HashMap<>();

        for (Jobs job : ongoingJobs) {
            String freeId = job.getFreeId().getFreeId();
            FreelancerJob freeJob = freelancerJobRepository.findByJobIdAndFreeId(job.getJobId(), freeId);

            if (freeJob != null) {
                Map<String, Object> jobInfo = new HashMap<>();
                jobInfo.put("duration", freeJob.getDuration());
                jobInfo.put("salary", freeJob.getSalary());
                jobDetails.put(freeId, jobInfo);
            }
        }

        // Add jobs to the model
        model.addAttribute("ongoingJobs", ongoingJobs);
        model.addAttribute("completedJobs", completedJobs);
        model.addAttribute("jobDetails", jobDetails);

        return "assignedjobs"; // Return the HTML page
    }




}
