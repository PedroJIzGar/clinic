package com.clinic.app.shared.util;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicController {

	
	@GetMapping("/api/public/ping")
	public String ping() {
		return "pong";
	}
}
