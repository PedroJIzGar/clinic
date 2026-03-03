package com.clinic.app.common;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrivateController {

	@GetMapping("/api/me")
	public String me (Authentication auth) {
		return "uid=" + (auth == null ? "null" : auth.getName());
	}
}
