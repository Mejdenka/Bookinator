package sbnz.integracija.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.drools.core.ClockType;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import DTO.BookDTO;
import enumeration.RoleEnum;
import events.BookLoanExpiredEvent;
import events.BookLoanMade;
import events.MembershipExpiredEvent;
import events.TransactionEvent;
import sbnz.integracija.example.facts.Book;
import sbnz.integracija.example.facts.BookLoan;
import sbnz.integracija.example.facts.BookRating;
import sbnz.integracija.example.facts.BookTag;
import sbnz.integracija.example.facts.BookTagStatus;
import sbnz.integracija.example.facts.Member;
import sbnz.integracija.example.facts.ReviewRequest;
import sbnz.integracija.example.facts.SearchRequestDTO;
import sbnz.integracija.example.facts.Tag;
import sbnz.integracija.example.facts.User;
import sbnz.integracija.example.repository.BookLoanRepository;
import sbnz.integracija.example.repository.BookRatingRepository;
import sbnz.integracija.example.repository.BookRepository;
import sbnz.integracija.example.repository.BookTagRepository;
import sbnz.integracija.example.repository.MemberRepository;
import sbnz.integracija.example.repository.TagRepository;
import sbnz.integracija.example.repository.userRepository;

@Service
public class SampleAppService {

	private static Logger log = LoggerFactory.getLogger(SampleAppService.class);
	
	@Autowired
	private final BookRepository bookRepository;
	
	@Autowired
	private final BookTagRepository bookTagRepository;
	
	@Autowired
	BookRatingRepository ratingRepo;
	
	@Autowired
	BookLoanRepository bookLoanRepository;
	
	@Autowired
	userRepository userRepo;

	@Autowired
	MemberRepository memberRepo; 
	
	@Autowired 
	TagRepository tagRepo;
	
	private final KieContainer kieContainer;
	
	
	@Autowired
	public SampleAppService(KieContainer kieContainer, BookRepository bookRepository, BookTagRepository bookTagRepository) {
		log.info("Initialising a new example session.");
		this.kieContainer = kieContainer;
		this.bookRepository = bookRepository;
		this.bookTagRepository = bookTagRepository;		 
	}
	
//	public Item getClassifiedItem(Item i) {
//		KieSession kieSession = kieContainer.newKieSession();
//		kieSession.insert(i);
//		kieSession.fireAllRules();
//		kieSession.dispose();
//		return i;
//	}
	
	
	public User login(User user) {
		User u = this.userRepo.findByUsername(user.getUsername());
		if(u != null) {
			if(u.getPassword().equals(user.getPassword())) {
				return u;
			}
		}
		System.out.println("nema");
		return null;
	}
	
	public User register(User user) {
		User u = this.userRepo.findByUsername(user.getUsername());
		if(u != null) {
			return null;
		} 
		user.setUserType(RoleEnum.MEMBER);
		Member member = new Member(user);
		
		this.memberRepo.save(member);
		payMembership(member.getId()); 
		return this.memberRepo.getOne(member.getId());
	}
	

	public void payMembership(Long id) {
		Member member = memberRepo.getOne(id);
		member.setMembershipExpired(false);
		memberRepo.save(member);
		//start the clock again
		startMembershipCheck(id);
	}
	
	public void startMembershipCheck(Long uId) {
		System.out.println("Initializing membership check rule...............................");
		
		KieServices ks = KieServices.Factory.get();
		KieBaseConfiguration kbconf = ks.newKieBaseConfiguration();
        kbconf.setOption(EventProcessingOption.STREAM);		
		KieBase kbase = kieContainer.newKieBase(kbconf);
		
		KieSessionConfiguration ksconf1 = ks.newKieSessionConfiguration();
	    ksconf1.setOption(ClockTypeOption.get(ClockType.REALTIME_CLOCK.getId()));
	    KieSession kSession1 = kbase.newKieSession(ksconf1, null);
	        
	    runRealtimeClock(kSession1, uId);
	   
	}

	private void runRealtimeClock(KieSession kSession1, Long uId) {
        Thread t = new Thread() {
            @Override
            public void run() {
            	TransactionEvent t1 = new TransactionEvent(uId);
            	kSession1.insert(t1);
            	
                kSession1.fireUntilHalt();

                Collection<?> newEvents = kSession1.getObjects(new ClassObjectFilter(MembershipExpiredEvent.class));
        	    for(Object o : newEvents) {
        	    	if(o instanceof MembershipExpiredEvent) {
        	    		MembershipExpiredEvent m = (MembershipExpiredEvent) o;
        	    		Member member = memberRepo.getOne(m.getUserId());
        	    		member.setMembershipExpired(true);
        	    		memberRepo.save(member);
        	    		
        	    	}
        	    		
        	    }
            }
        };
        t.setDaemon(true);
        t.start();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            //do nothing
        }
    }

	
	
	public List<User> findAll() {
		
		return userRepo.findAll();
	}
	
	public User saveUser(User user) {
		User u = this.userRepo.findByUsername(user.getUsername());
		if(u != null) {
			return null;
		} 
		return this.userRepo.save(user);
	}
	
	public ArrayList<BookDTO> getFilteredBooks(SearchRequestDTO searchRequestDTO) {
		ArrayList<Book> books  = startSearch(searchRequestDTO);
		ArrayList<BookDTO> bookDTOs = new ArrayList<>();
		
		Collections.sort(books, Collections.reverseOrder());
		
		for(Book b : books) {
			System.out.println("book - " + b.toString() + " - " + b.getMatch());
			BookDTO bookDTO = new BookDTO(b);
			ArrayList<BookTag> bookTags = this.bookTagRepository.findTagsByBookId(b.getId());
			for(BookTag bt : bookTags) {
				bookDTO.getTags().add(bt);
			}

			bookDTOs.add(bookDTO);	
		}		

		
		return bookDTOs;
	}
	
	public ArrayList<BookDTO> getBookHistory(Long uId) {
		ArrayList<BookLoan> bookLoans = this.bookLoanRepository.findByUserId(uId);
		ArrayList<BookDTO> bookDTOs = new ArrayList<>();
		//Collections.sort(books, Collections.reverseOrder());
		
		for(BookLoan bookLoan : bookLoans) {
			System.out.println(bookLoan.getBookId());
			BookDTO bookDTO = new BookDTO(bookRepository.getOne(bookLoan.getBookId()));
			ArrayList<BookTag> bookTags = this.bookTagRepository.findTagsByBookId(bookLoan.getBookId());
			for(BookTag bt : bookTags) {
				bookDTO.getTags().add(bt);
			}

			bookDTOs.add(bookDTO);	
		}		

		
		return bookDTOs;
	}
	
	public ArrayList<Book> startSearch(SearchRequestDTO searchRequestDTO) {
		System.out.println("Initializing virtual assisant...............................");
		
		KieServices ks = KieServices.Factory.get();
		KieBaseConfiguration kbconf = ks.newKieBaseConfiguration();
        kbconf.setOption(EventProcessingOption.STREAM);		
		KieBase kbase = kieContainer.newKieBase(kbconf);
		
	    KieSession kSession = kbase.newKieSession();
		
	    List<Book> books = bookRepository.findAll();
	    for(Book book : books) {
	    	book.setMatch((long)0);
	    	kSession.getEntryPoint("search").insert(book);
	    }
	    
	    List<Tag> tags =  tagRepo.findAll();
	    for(Tag tag : tags) {
	    	kSession.getEntryPoint("search").insert(tag);
	    }
	    
	    List<BookTag> bookTags = bookTagRepository.findAll();
	    for(BookTag bookTag : bookTags) {
	    	kSession.getEntryPoint("search").insert(bookTag);
	    }   
	 
	    kSession.getEntryPoint("search").insert(searchRequestDTO);
		// can be commented out
		kSession.getAgenda().getAgendaGroup( "startSearch" ).setFocus();
		//
		kSession.fireAllRules();
		List<Book> searchResults=new ArrayList<>();
		QueryResults results = kSession.getQueryResults( "getSearchResults" ); 
		for ( QueryResultsRow row : results ) {
		    Book b = ( Book ) row.get( "$result" ); 
		    searchResults.add(b);
		}
		 
		System.out.println("rezultati:");
		for (Book b: searchResults)
			System.out.println(b.getMatch() + " : " + b.toString());
		
		return (ArrayList<Book>) searchResults;
	}
	
	public void bookReview(ReviewRequest reviewRequest) {
		System.out.println(reviewRequest.toString());
		Book book = bookRepository.getOne(reviewRequest.getBookId());
		
		Iterator it = reviewRequest.getTags().entrySet().iterator();
	    while (it.hasNext()) {
	        HashMap.Entry pair = (HashMap.Entry)it.next();
	        //book.getTags().put(pair.getKey().toString(), pair.getValue().toString());
	        Tag tag = this.tagRepo.findByTagName(pair.getKey().toString());

	        if (tag==null) {
	        	tagRepo.save(new Tag(pair.getKey().toString()));
	        	 tag = this.tagRepo.findByTagName(pair.getKey().toString());
 	        	 tag.setApproved(false); 
	        }
	       
	        this.bookTagRepository.save(new BookTag(reviewRequest.getBookId(), tag.getId(), pair.getValue().toString(), BookTagStatus.PENDING));
	        
	        it.remove(); // avoids a ConcurrentModificationException

	    }
		
		User user =  userRepo.findById(reviewRequest.getUserId()).get();
		BookRating rating=new BookRating(book,user,reviewRequest.getRate());
		this.ratingRepo.save(rating);
		
		List<BookRating> allRatings=ratingRepo.findByBookId(book.getId());
		float avg = 0;
		int count = allRatings.size();
		for (BookRating r:allRatings) {
			avg+=r.getRating();
		}
		float newRating = avg/count;
		book.setRating(newRating);
		
	    this.bookRepository.save(book);
	    System.out.println("Book updated!");
	}
	
	
	public void approveTag(Long id) {
		BookTag bookTag = bookTagRepository.getOne(id);
		bookTag.setStatus(BookTagStatus.APPROVED);
		this.bookTagRepository.save(bookTag);
	}
	
	public void returnBookLoan(Long id) {
		BookLoan bookLoan = this.bookLoanRepository.getOne(id);
		Member member = this.memberRepo.getOne(bookLoan.getUserId());
		member.setLoan(null);
		bookLoan.setReturned(true);
		bookLoanRepository.save(bookLoan);
		member.setCanRent(true);
		memberRepo.save(member);
	}
	
	public void makeBookLoan(Long userId, Long bookId) {
		Member member = this.memberRepo.getOne(userId);
		Book book = this.bookRepository.getOne(bookId);
		BookLoan bookLoan = new BookLoan();
		bookLoan.setReturned(false);
		bookLoan.setBookId(book.getId());
		bookLoan.setUserId(userId);
		bookLoanRepository.save(bookLoan);
		member.setLoan(bookLoan);
		member.setCanRent(false);
		memberRepo.save(member);
		
		System.out.println("Initializing book loan rule...............................");
		
		KieServices ks = KieServices.Factory.get();
		KieBaseConfiguration kbconf = ks.newKieBaseConfiguration();
        kbconf.setOption(EventProcessingOption.STREAM);		
		KieBase kbase = kieContainer.newKieBase(kbconf);
		
		KieSessionConfiguration ksconf1 = ks.newKieSessionConfiguration();
	    ksconf1.setOption(ClockTypeOption.get(ClockType.REALTIME_CLOCK.getId()));
	    KieSession kSession1 = kbase.newKieSession(ksconf1, null);
	        
        Thread t = new Thread() {
            @Override
            public void run() {
            	BookLoanMade e1 = new BookLoanMade(member.getId(), bookLoan.getBookId());
            	kSession1.insert(e1);
            	
                kSession1.fireUntilHalt();

                Collection<?> newEvents = kSession1.getObjects(new ClassObjectFilter(BookLoanExpiredEvent.class));
        	    for(Object o : newEvents) {
        	    	if(o instanceof BookLoanExpiredEvent) {
        	    		BookLoanExpiredEvent b = (BookLoanExpiredEvent) o;
        	    		BookLoan bl = bookLoanRepository.getOne(b.getBookLoanId());
        	    		bl.setExpired(true);
        	    		bookLoanRepository.save(bl);
        	    		Member m = memberRepo.getOne(b.getUserId());
        	    		m.setCanRent(false);
        	    		memberRepo.save(member);
        	    		
        	    	}
        	    		
        	    }
            }
        };
        t.setDaemon(true);
        t.start();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            //do nothing
        }
		
	}
	

	public ArrayList<BookLoan> getBookLoans(Long uId) {
		return bookLoanRepository.findByUserId(uId);
	}

	
	public void approveJustTag(String name) {
		Tag tag = tagRepo.findByTagName(name);
		tag.setApproved(true);
		this.tagRepo.save(tag);
		// bookTagRepository.setConfirmed(tag.getTagValue());
	}
	

	public void deleteTag(Long id) {
		BookTag bookTag = bookTagRepository.getOne(id);
		bookTag.setStatus(BookTagStatus.REFUSED);
		this.bookTagRepository.delete(bookTag);
	}


	public void deleteJustTag(String name) {
		Tag tag = tagRepo.findByTagName(name);
		tag.setApproved(false);
		this.tagRepo.delete(tag);
	}

	public List<BookTag> findAllTags() {
		return bookTagRepository.findAll();
	}

	public List<BookTag> findRequestedTags() {
		return bookTagRepository.findRequestedTags();
	}

	public List<Tag> findTags() {
		return tagRepo.findTags();
	}

	public boolean addToWishlist(Long uId, Long bookId) {
		try
		{
			Member member = this.memberRepo.getOne(uId);
			member.getWishlist().add(this.bookRepository.getOne(bookId));
			this.memberRepo.save(member);
		} catch (Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public ArrayList<BookDTO> getWishlist(Long uId) {
		Member m = this.memberRepo.getOne(uId);
		ArrayList<BookDTO> bookDTOs = new ArrayList<>();
		for(Book b : m.getWishlist()) {
			BookDTO bDTO = new BookDTO(b);
			ArrayList<BookTag> tags = this.bookTagRepository.findTagsByBookId(b.getId());
			bDTO.setTags(tags);
			bookDTOs.add(bDTO);
		}
		return bookDTOs;
	}
}
