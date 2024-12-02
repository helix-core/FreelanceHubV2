package com.example.FreelanceHub.controllers;

import com.example.FreelanceHub.models.Client;
import com.example.FreelanceHub.models.Freelancer;
import com.example.FreelanceHub.services.ClientService;
import com.example.FreelanceHub.services.FreelancerService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class UserController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private FreelancerService freeService;
    
    @Autowired
    private HttpSession session;
    
    @GetMapping("")
    public String showLandingpage(Model model) {
		 String role = (String) session.getAttribute("role");
		 model.addAttribute("role", role);
		 
        return "landing";
    }

    // Selection Page
    @GetMapping("/signup/selection")
    public String showSignupSelectionPage() {
        return "selection"; // Render the signup-selection.html page
    }

    // Client Signup Page
    @GetMapping("/signup/client")
    public String showClientSignupPage(Model model) {
        model.addAttribute("client", new Client());
        return "signupclient"; // Render the signup-client.html page
    }

    @PostMapping("/signup/client")
    public String registerClient(@ModelAttribute Client client, Model model) {
        boolean isRegistered = clientService.registerClient(client);
        if (isRegistered) {
            return "redirect:/login"; // Redirect to the common login page
        } else {
            model.addAttribute("error", "Failed to register as a client. Please try again.");
            return "signupclient";
        }
    }

    // Freelancer Signup Page
    @GetMapping("/signup/freelancer")
    public String showFreelancerSignupPage(Model model) {
        model.addAttribute("freelancer", new Freelancer());
        return "signupfree"; // Render the signup-freelancer.html page
    }

    @PostMapping("/signup/freelancer")
    public String registerFreelancer(@ModelAttribute Freelancer freelancer, Model model) {
        boolean isRegistered = freeService.registerFreelancer(freelancer);
        if (isRegistered) {
            return "redirect:/login"; // Redirect to the common login page
        } else {
            model.addAttribute("error", "Failed to register as a freelancer. Please try again.");
            return "signupfree";
        }
    }

    // Common Login Page
    @GetMapping("/login")
    public String showLoginPage() {
        return "common-login"; // Render the common login.html page
    }

    @PostMapping("/login")
    public String login(@RequestParam("email") String email,
                        @RequestParam("password") String password,
                        Model model) {
    	
        // Check in Client table
        if (clientService.validateClient(email, password)) {
        	Client client=clientService.clientRepository.findBycompEmail(email);
        	String role=clientService.getUserRole(client.getClientId());
        	session.setAttribute("role", role);
        	session.setAttribute("clientId", client.getClientId());
            return "redirect:"; // Redirect to the dashboard
        }
        
        // Check in Freelancer table
        if (freeService.validateFreelancer(email, password)) {
        	Freelancer free=freeService.freeRepository.findByfreeEmail(email);
        	String role=freeService.getUserRole(free.getFreeId());
        	session.setAttribute("role", role);
        	session.setAttribute("freelancerId", free.getFreeId());
            return "redirect:"; // Redirect to the dashboard
        }
        // Invalid login
        model.addAttribute("error", "Invalid email or password");
        return "common-login";
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Invalidate the session to remove all attributes (including the role)
        session.invalidate();
        
        // Redirect to the landing or login page
        return "redirect:/";
    }
    
    @GetMapping("/profile/client")
    public String getClientProfile(Model model) {
        String clientId = (String) session.getAttribute("clientId");

        if (clientId == null) {
            return "redirect:/login"; // Redirect to login if no email is found in the session
        }

        Client client = clientService.findByClientId(clientId);
        if (client == null) {
            model.addAttribute("error", "Client not found.");
            return "error"; // Display an error page if client is not found
        }

        model.addAttribute("client", client);
        return "clientprofile"; // Render client profile page
    }
    
    @GetMapping("/profile/freelancer")
    public String showFreelancerProfile(Model model) {
        String freeId = (String) session.getAttribute("freelancerId"); // Ensure "FreeId" matches the login session key

        if (freeId == null) {
            return "redirect:/login"; // Redirect to login if FreeId is not found
        }

        Freelancer freelancer = freeService.findByFreeId(freeId); // Ensure findByFreeId exists and matches case

        if (freelancer == null) {
            model.addAttribute("error", "Freelancer not found.");
            return "error"; // Display error page if freelancer doesn't exist
        }

        model.addAttribute("freelancer", freelancer);
        return "freelancerprofile";
    }

    

    
}
