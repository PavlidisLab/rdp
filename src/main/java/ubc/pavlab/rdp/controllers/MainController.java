package ubc.pavlab.rdp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.repositories.UserRepository;

@Controller
public class MainController {

	@RequestMapping(value = "/")
	public String index() {
		return "home";
	}

	@RequestMapping(value = "/hello")
	public String hello() {
		return "hello";
	}

	@RequestMapping(value = "/login")
	public String login() {
		return "login";
	}
}
