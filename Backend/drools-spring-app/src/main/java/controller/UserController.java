package controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import DTO.BookDTO;
import DTO.BookTagDTO;
import sbnz.integracija.example.SampleAppService;
import sbnz.integracija.example.facts.BookTag;
import sbnz.integracija.example.facts.ReviewRequest;
import sbnz.integracija.example.facts.SearchRequest;
import sbnz.integracija.example.facts.User;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
public class UserController {
	private static Logger log = LoggerFactory.getLogger(UserController.class);

	private final SampleAppService sampleService;

	@Autowired
	public UserController(SampleAppService sampleService) {
		this.sampleService = sampleService;
	}
	
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public ResponseEntity<?> login(@RequestBody  User u) {
		User user = sampleService.login(u);
		if(user != null) {
			System.out.println("User has logged in");
			return new ResponseEntity<>(user, HttpStatus.OK);
		}
		else 
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	@RequestMapping(value = "/register", method = RequestMethod.POST)
	public ResponseEntity<?> register(@RequestBody  User u) {
		User user = sampleService.register(u);
		if(user != null) {
			this.sampleService.startMembershipCheck(user.getId());
			return new ResponseEntity<>(user, HttpStatus.OK);	
		}
		else 
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}
	
	@RequestMapping(value="/allUsers", method = RequestMethod.GET)
	public ResponseEntity<List<User>> getUsers() {
		List<User> user = sampleService.findAll();
		return new ResponseEntity<>(user, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/addUser", method = RequestMethod.POST)
	public ResponseEntity<?> saveUser(@RequestBody  User u) {
		User user = sampleService.saveUser(u);
		if(user != null) 
			return new ResponseEntity<>(user, HttpStatus.OK);
		else 
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

//	@RequestMapping(value = "/item", method = RequestMethod.GET, produces = "application/json")
//	public Item getQuestions(@RequestParam(required = true) String id, @RequestParam(required = true) String name,
//			@RequestParam(required = true) double cost, @RequestParam(required = true) double salePrice) {
//
//		Item newItem = new Item(Long.parseLong(id), name, cost, salePrice);
//
//		log.debug("Item request received for: " + newItem);
//
//		Item i2 = sampleService.getClassifiedItem(newItem);
//
//		return i2;
//	}
	
	
	
	@RequestMapping(value = "/review", method = RequestMethod.POST)
	public ResponseEntity bookReview(@RequestBody ReviewRequest reviewRequest) {
		System.out.println("reviewing...");
		this.sampleService.bookReview(reviewRequest);
		return new ResponseEntity(HttpStatus.OK);
	}
	

	@RequestMapping(value = "/manageTag", method = RequestMethod.POST)
	public ResponseEntity manageTag(@RequestBody BookTagDTO bookTagDTO) {
		if(bookTagDTO.isApproved()) {
			this.sampleService.approveTag(bookTagDTO.getId());
		} else {
			this.sampleService.deleteTag(bookTagDTO.getId());
		}
		
		return new ResponseEntity(HttpStatus.OK);
	}
	
	@RequestMapping(value = "/payMembership/{uId}", method = RequestMethod.GET)
	public ResponseEntity payMembership(@PathVariable("uId") Long uId) {
		this.sampleService.payMembership(uId);
		return new ResponseEntity(HttpStatus.OK);
	}
}