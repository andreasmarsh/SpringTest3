package com.andreasmarsh.SpringTest;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import java.text.DecimalFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class AppController {

    @Autowired
    private UserRepository repo;

    @Autowired
    private MovieRepository movieRepo;

    @Autowired
    private MovieShowingRepository movieShowingRepo;

    @Autowired
    private PromotionRepository promoRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private BookedShowingRepository bookedShowingRepo;

    @Autowired
    private UserServices service;

    @Autowired
    private MovieServices movieService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SeatRepository seatRepo;

    @Autowired
    private PriceRepository priceRepo;

    @Autowired
    private CreditCardRepository cardRepo;




    @PostMapping("/process_register")
    public String processRegister(User user, Address address, HttpServletRequest request)
            throws UnsupportedEncodingException, MessagingException {

        String[] cardIDs = request.getParameterValues("cardID");
        String[] cardTypes = request.getParameterValues("cardType");
        String[] cardNumbers = request.getParameterValues("cardNumber");
        String[] cardMonths = request.getParameterValues("expMonth");
        String[] cardYears = request.getParameterValues("expYear");
        String[] cardCvvs = request.getParameterValues("cvv");

        System.out.println(cardNumbers.length + " cards");
        for (int i = 0; i < 3; i++) {
            System.out.println(cardNumbers[i]);
            if (!cardNumbers[i].equals("")) {
                user.addCreditCard(cardTypes[i], Long.parseLong(cardNumbers[i]), Long.parseLong(cardMonths[i]), Long.parseLong(cardYears[i]), cardCvvs[i]);
            } else {
                user.addCreditCard(null, null, null, null, null);
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            if(repo.doesEmailExist(user.getEmail()) == BigInteger.valueOf(0)) {
                service.register(user, address, getSiteURL(request));
            } else {
                return "user_exists";
            }

            return "register_success";
        } else {
            System.out.println(user.getId());
            service.update(user, address, getSiteURL(request));
        }
        return "redirect:/";
    }

    @PostMapping("/process_reset")
    public String processReset(HttpServletRequest request)
            throws UnsupportedEncodingException, MessagingException {

        String[] emails = request.getParameterValues("email");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {

            if(repo.doesEmailExist(emails[0]) == BigInteger.valueOf(1)) {
                service.reset(emails[0], getSiteURL(request));

                return "reset_success";
            } else {
                return "reset_form_failure";
            }
        }

        return "redirect:/";
    }

    @PostMapping("/process_edit")
    public String processEdit(User user, Address address, HttpServletRequest request, Model model)
            throws UnsupportedEncodingException, MessagingException {

        String[] cardIDs = request.getParameterValues("cardID");
        String[] cardTypes = request.getParameterValues("cardType");
        String[] cardNumbers = request.getParameterValues("cardNumber");
        String[] cardMonths = request.getParameterValues("expMonth");
        String[] cardYears = request.getParameterValues("expYear");
        String[] cardCvvs = request.getParameterValues("cvv");

        String[] currentPasswords = request.getParameterValues("currentPwd");
        String[] newPasswords = request.getParameterValues("newPwd");


        //System.out.println(cardNumbers.length + " cards");
        for (int i = 0; i < 3; i++) {
            System.out.println(cardNumbers[i]);
            if (!cardNumbers[i].equals("")) {
                user.setCreditCard(Long.parseLong(cardIDs[i]), cardTypes[i], Long.parseLong(cardNumbers[i]), Long.parseLong(cardMonths[i]), Long.parseLong(cardYears[i]), cardCvvs[i]);
            } else {
                user.setCreditCard(Long.parseLong(cardIDs[i]), null, null, null, null, null);
            }
        }

        System.out.println(currentPasswords[0]);
        System.out.println(newPasswords[0]);
        System.out.println(passwordEncoder.encode(currentPasswords[0]));
        System.out.println(user.getPassword());

        BCryptPasswordEncoder b = new BCryptPasswordEncoder();
        if(b.matches(currentPasswords[0], user.getPassword()) && !(newPasswords[0].equals(""))) {
            String encodedPassword = passwordEncoder.encode(newPasswords[0]);
            user.setPassword(encodedPassword);
            service.update(user, address, getSiteURL(request));
            return "edit_success";
        } else if(currentPasswords[0].equals("") && newPasswords[0].equals("")){
            System.out.println(user.getId());
            service.update(user, address, getSiteURL(request));
            return "edit_success";
        } else {
            user.setAddress(address);
            return showEditProfileFormFailure(user, model);
        }
    }

    private String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }

    @Autowired
    private UserRepository userRepo;

    @GetMapping("/no-movies")
    public String viewNoMovies(Model model) {
        List<Movie> listMoviesNowShowing = movieRepo.findByNowShowing(true);
        List<Movie> listMoviesComingSoon = movieRepo.findByNowShowing(false);
        String search = "";

        model.addAttribute("listMoviesNowShowing", listMoviesNowShowing);
        model.addAttribute("listMoviesComingSoon", listMoviesComingSoon);
        model.addAttribute("search", search);
        return "no-movies";
    }

    @GetMapping("")
    public String viewHomePage(Model model, @AuthenticationPrincipal UserDetails currentUser, HttpServletRequest request) {

        try {

            User theUser = repo.findByEmail(currentUser.getUsername());
            model.addAttribute("theUser", theUser);
        } catch (NullPointerException npe){
            //if no one is logged in, they will be tempUser
            User theUser = repo.findByEmail("temp@gmail.com");
            model.addAttribute("theUser", theUser);
            //nothing :p
        }


        List<Movie> listMoviesNowShowing = movieRepo.findByNowShowing(true);
        List<Movie> listMoviesComingSoon = movieRepo.findByNowShowing(false);
        String search = "";

        model.addAttribute("listMoviesNowShowing", listMoviesNowShowing);
        model.addAttribute("listMoviesComingSoon", listMoviesComingSoon);
        model.addAttribute("search", search);
        return "homepage";
    }

    @PostMapping("/process_search")
    public String viewSearch(Model model, String search) {
        String modifiedSearch = "%" + search + "%";
        List<Movie> listSearchResults = movieRepo.findBySearch(modifiedSearch);

        List<Movie> allMovies = movieRepo.findAll();

        allMovies.forEach(movie -> {
            if (movie.getCategories().contains(categoryRepo.findByCategory(search))) {
                listSearchResults.add(movie);
            }});

        model.addAttribute("listSearchResults", listSearchResults);
        String search2 = "";
        model.addAttribute("search", search2);
        if (listSearchResults.size() > 0) {
            return "search-results";
        } else {
            return "redirect:/no-movies";
        }
    }

    @GetMapping("/error")
    public String showErrorPage(Model model) {
        String search = "";
        model.addAttribute("search", search);
        return "error";
    }

    @GetMapping("/profile")
    public String showProfilePage(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        User user = repo.findByEmail(currentUser.getUsername());
        model.addAttribute("currentUser", user);
        String search = "";
        model.addAttribute("search", search);

        return "profile";
    }

    @GetMapping("/admin-portal")
    public String showAdminPortal(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        User user = (User) repo.findByEmail(currentUser.getUsername());
        model.addAttribute("currentUser", user);
        String search = "";
        model.addAttribute("search", search);

        return "admin-portal";
    }

    @GetMapping("/manage-movies")
    public String listMovies(Model model) {
        List<Movie> listMovies = movieRepo.findAll();
        model.addAttribute("listMovies", listMovies);
        String search = "";
        model.addAttribute("search", search);

        return "manage-movies";
    }

    @GetMapping("/manage-promotions")
    public String listPromotions(Model model) {
        List<Promotion> listPromotions = promoRepo.findAll();
        model.addAttribute("listPromotions", listPromotions);
        String search = "";
        model.addAttribute("search", search);

        return "manage-promotions";
    }

    @GetMapping("/promo-sent")
    public String listPromotions2(Model model) {
        List<Promotion> listPromotions = promoRepo.findAll();
        model.addAttribute("listPromotions", listPromotions);
        String search = "";
        model.addAttribute("search", search);

        return "promo-sent";
    }

    @GetMapping("/promotion-form")
    public String promotionForm(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
        model.addAttribute("promotion", new Promotion());
        String search = "";
        model.addAttribute("search", search);

        return "promotion-form";
        //}
    }

    @PostMapping("/process_addPromotion")
    public String processAddPromotion(Promotion promo, HttpServletRequest request)
            throws UnsupportedEncodingException, MessagingException, ParseException {


        Date date=new SimpleDateFormat("yyyy-MM-dd").parse(promo.getStartStringDate());
        Date date2=new SimpleDateFormat("HH:mm").parse(promo.getStartStringTime());
        //promo.setStartTime(date);
        promo.setStartDate(date);
        promo.setStartTime(date2);

        Date date3=new SimpleDateFormat("yyyy-MM-dd").parse(promo.getEndStringDate());
        Date date4=new SimpleDateFormat("HH:mm").parse(promo.getEndStringTime());

        promo.setEndDate(date3);
        promo.setEndTime(date4);

        Promotion savedPromo = promoRepo.save(promo);

        return "redirect:/manage-promotions";
    }

    @GetMapping("/edit-promotion/{id}")
    public String showEditPromotionForm(@PathVariable("id") Integer id, Model model) {
        System.out.println(id);
        Promotion promo = promoRepo.findById(Long.valueOf(id)).get();

        model.addAttribute("promotion", promo);

        System.out.println("here");

        return "edit-promotion-page";
    }

    @PostMapping("/edit-promotion-process")
    public String processEditPromotion(Promotion promo, HttpServletRequest request, Model model)
            throws UnsupportedEncodingException, MessagingException, ParseException {

        Date date=new SimpleDateFormat("yyyy-MM-dd").parse(promo.getStartStringDate());
        Date date2=new SimpleDateFormat("HH:mm").parse(promo.getStartStringTime());
        //promo.setStartTime(date);
        promo.setStartDate(date);
        promo.setStartTime(date2);

        Date date3=new SimpleDateFormat("yyyy-MM-dd").parse(promo.getEndStringDate());
        Date date4=new SimpleDateFormat("HH:mm").parse(promo.getEndStringTime());

        promo.setEndDate(date3);
        promo.setEndTime(date4);

        promoRepo.save(promo);

        return "redirect:/manage-promotions";
    }

    @GetMapping("/movie-form")
    public String movieForm(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
        model.addAttribute("movie", new Movie());

        String search = "";
        model.addAttribute("search", search);

        return "movie-form";
        //}
    }

    @GetMapping("/edit-movie/{id}")
    public String showEditMovieForm(@PathVariable("id") Integer id, Model model) {
        System.out.println(id);
        Movie movie = movieRepo.findById(Long.valueOf(id)).get();
        model.addAttribute("movie", movie);
        List<MovieShowing> showings = movie.getMovieShowings();
        model.addAttribute("showings", showings);
        List<Review> reviews = movie.getReviews();
        model.addAttribute("reviews", reviews);
        String search = "";
        model.addAttribute("search", search);
        model.addAttribute("movieID", id);

        return "edit-movie-form";
    }

    @PostMapping("/process_addMovie")
    public String processRegisterMovie(Movie movie, HttpServletRequest request)
            throws UnsupportedEncodingException, MessagingException, ParseException {

        String[] rating = request.getParameterValues("parentalguidancerating");
        movie.setRating(rating[0]);

        // set showings
        String[] theaterIDS = request.getParameterValues("theaterID");
        System.out.println("here");
        String[] dates = request.getParameterValues("date");
        String[] times = request.getParameterValues("time");

        List<MovieShowing> showings = new ArrayList<>();

        System.out.println(dates[1]);
        System.out.println(times[1]);

        for (int i = 0; i < 3; i++) {
            if (!theaterIDS[i].equals("") && !dates[i].equals("") && !times[i].equals("")) {
                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(dates[i]);
                Date date2 = new SimpleDateFormat("HH:mm").parse(times[i]);

                MovieShowing movieShowing = new MovieShowing();

                movieShowing.setTheaterID(Long.parseLong(theaterIDS[i]));
                movieShowing.setDate(date);
                movieShowing.setTime(date2);
                movieShowing.setMovie(movie);


                showings.add(movieShowing);
            }
        }

        movie.setMovieShowings(showings);

        // set reviews

        String[] reviewers = request.getParameterValues("reviewer");
        String[] ratings = request.getParameterValues("rating");
        String[] reviews = request.getParameterValues("review");

        List<Review> reviewList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (!reviews[i].equals("") && !ratings[i].equals("") && !reviewers[i].equals("")) {
                Review review = new Review();
                review.setReviewer(reviewers[i]);
                review.setRating(Long.parseLong(ratings[i]));
                review.setReview(reviews[i]);
                review.setMovie(movie);
                reviewList.add(review);
            }
        }

        if (reviewList.size() != 0){
            movie.setReviews(reviewList);
        }

        // set categories
        String[] category = request.getParameterValues("category");
        String[] category2 = request.getParameterValues("category2");

        Set<Category> categories = new HashSet<Category>();

        if (!category[0].equals("")) {
            if (categoryRepo.findByCategory(category[0]) != null) {
                Category categoryPicked = categoryRepo.findByCategory(category[0]);
                categories.add(categoryPicked);
            } else {
                if (categoryRepo.findAll().size() == 0){
                    Category categoryPicked = new Category((long)(1), category[0]);
                    categoryRepo.save(categoryPicked);
                    categoryPicked = categoryRepo.findByCategory(category[0]);
                    categories.add(categoryPicked);
                } else {
                    Category categoryPicked = new Category((long) (categoryRepo.findAll().get(categoryRepo.findAll().size() - 1).getCategoryID() + 1), category[0]);
                    categoryRepo.save(categoryPicked);
                    categoryPicked = categoryRepo.findByCategory(category[0]);
                    categories.add(categoryPicked);
                }
            }
        }

        if (!category2[0].equals("")) {
            if (categoryRepo.findByCategory(category2[0]) != null) {
                Category categoryPicked = categoryRepo.findByCategory(category2[0]);
                categories.add(categoryPicked);
            } else {
                if (categoryRepo.findAll().size() == 0){
                    Category categoryPicked = new Category((long)(1), category2[0]);
                    categoryRepo.save(categoryPicked);
                    categoryPicked = categoryRepo.findByCategory(category2[0]);
                    categories.add(categoryPicked);
                } else {
                    Category categoryPicked = new Category((long) (categoryRepo.findAll().get(categoryRepo.findAll().size() - 1).getCategoryID() + 1), category2[0]);
                    categoryRepo.save(categoryPicked);
                    categoryPicked = categoryRepo.findByCategory(category2[0]);
                    categories.add(categoryPicked);
                }
            }
        }

        movie.setCategories(categories);

        System.out.println(categoryRepo.findByCategory(category2[0]));
        System.out.println(movie.getCategories().toString());
        // save movie
        Movie saved = movieRepo.save(movie);

        //fill the db with showings
        List<MovieShowing> allShowings = saved.getMovieShowings();

        for (int i = 0; i < allShowings.size(); i++) {

            //for each showing, the database will have 40 seats avaliable
            movieShowingRepo.fillTheater(allShowings.get(i).getShowID());

        }

        return "redirect:/manage-movies";
    }

    @GetMapping("/update-info")
    public String showEditForm(Model model) {
        String search = "";
        model.addAttribute("search", search);

        return "update-info";
    }


    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            model.addAttribute("user", new User());
            model.addAttribute("address", new Address());
            model.addAttribute("creditcard", new CreditCard());
            String search = "";
            model.addAttribute("search", search);
            return "signup_form";
        }

        return "redirect:/";
    }

    @GetMapping("/reset")
    public String showResetForm(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String search = "";
        model.addAttribute("search", search);
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "reset_form";
        }

        return "redirect:/";
    }

    @GetMapping("/view-movie/{id}")
    public String showMovieDetails(@PathVariable("id") Integer id, Model model) {
        Movie movie = movieRepo.findById(Long.valueOf(id)).get();
        model.addAttribute("movie", movie);
        String search = "";
        model.addAttribute("search", search);
        List<MovieShowing> showingList = movie.getMovieShowings();
        model.addAttribute("showingList", showingList);

        return "view-movie";
    }

    @GetMapping("/edit-profile/{id}")
    public String showEditProfileForm(@PathVariable("id") Integer id, Model model) {
        System.out.println(id);
        User user = repo.findById(Long.valueOf(id)).get();
        model.addAttribute("user", user);

        model.addAttribute("address", user.getAddress());
        String search = "";
        model.addAttribute("search", search);

        return "edit_form";
    }

    @GetMapping("/delete-promotion/{id}")
    public String showDeletePromotionForm(@PathVariable("id") Integer id, Model model) {
        //System.out.println(id);
        Promotion promo = promoRepo.findById(Long.valueOf(id)).get();
        model.addAttribute("promo", promo);
        String search = "";
        model.addAttribute("search", search);

        return "delete-promotion";
    }

    @PostMapping("/process_delete_promotion")
    public String processDeletePromotion(Promotion promotion) {
        //System.out.println(promotion.getPromotionID());
        service.deletePromotion(promotion);
        return "redirect:/manage-promotions";
    }

    @GetMapping("/send-promotion/{id}")
    public String showSendPromotionForm(@PathVariable("id") Integer id, Model model) {
        //System.out.println(id);
        Promotion promo = promoRepo.findById(Long.valueOf(id)).get();
        model.addAttribute("promo", promo);
        String search = "";
        model.addAttribute("search", search);

        return "send-promo";
    }

    @PostMapping("/process_send_promotion")
    public String processSendPromotion(Promotion promotion,  HttpServletRequest request) throws MessagingException, UnsupportedEncodingException {
        //System.out.println(promotion.getPromotionID());
        promotion = promoRepo.getById(promotion.getPromotionID());
        service.sendPromotion(promotion, getSiteURL(request));
        return "redirect:/promo-sent";
    }

    @GetMapping("/delete-movie/{id}")
    public String showDeleteMovieForm(@PathVariable("id") Integer id, Model model) {
        //System.out.println(id);
        Movie movie = movieRepo.findById(Long.valueOf(id)).get();
        model.addAttribute("movie", movie);
        String search = "";
        model.addAttribute("search", search);

        return "delete-movie";
    }

    @PostMapping("/process_delete_movie")
    public String processDeleteMovie(Movie movie) {
        //System.out.println(promotion.getPromotionID());
        service.deleteMovie(movie);
        return "redirect:/manage-movies";
    }

    @GetMapping("/book-movie/{id}")
    public String bookMovie(@PathVariable("id") Integer id, Model model) {
        MovieShowing showing = movieShowingRepo.findById(Long.valueOf(id)).get(); //get the specific showing
        Movie movie = showing.getMovie();
        model.addAttribute("movie", movie);
        model.addAttribute("showing", showing); //the movie showing
        String search = "";
        model.addAttribute("search", search);
        List<MovieShowing> showingList = movie.getMovieShowings();
        model.addAttribute("showingList", showingList);

        return "book-movie";
    }

    @GetMapping("/booking-process/{id}")
    public String bookingProcess(@PathVariable("id") Integer id, @AuthenticationPrincipal UserDetails currentUser)
            throws UnsupportedEncodingException, MessagingException, ParseException {
        MovieShowing showing = movieShowingRepo.findById(Long.valueOf(id)).get(); //get the specific showing
        Movie movie = showing.getMovie();


        // now, we just need to save it to the user's booking.
        // If no user is signed in, it's saved to the session.
        User user = repo.findByEmail(currentUser.getUsername());

        Booking booking = new Booking(); //user.getBooking();

        //Use the showing info
        bookedShowing bookedShow = new bookedShowing(booking, movie, showing.getDate(), showing.getTime());


        List<bookedShowing> showings = new ArrayList<>(); //make an empty list to be filled
        System.out.println(showings.size());
        if (booking.getBookedShowings() != null) {
            //if it's not null, take the other showings into account.
            showings = booking.getBookedShowings();

        }



        showings.add(bookedShow); //save it to the showings

        booking.setBookedShowings(showings);
        //user.setBooking(booking);
        booking.setUser(user);

        System.out.println(showings.size());

        //create the booked showing
        bookedShowingRepo.save(bookedShow);

        bookingRepo.save(booking);
        repo.save(user);


        System.out.println(booking.getBookingID() +" " +movie.getTitle() +" "+ showing.getDate() +" "+ showing.getTime());
        System.out.println(bookedShow.getBookedShowingID() +" " +bookedShow.getDate() +" ");

        return "redirect:/cart";
        //return "homepage";
        //return "redirect:/homepage";
    }

    @PostMapping("/cart")
    public String cart(Model model, @AuthenticationPrincipal UserDetails currentUser, @RequestParam Integer showID, HttpServletRequest request) {

        User user = new User();

        try {
            user = repo.findByEmail(currentUser.getUsername());
            model.addAttribute("user", user);
        } catch (NullPointerException npe) {
            user = repo.findByEmail("temp@gmail.com");
            model.addAttribute("user", user);

        }

        String type = "";
        model.addAttribute("type", type);
        Long num = 0L;
        model.addAttribute("num", num);



        String[] ticketTypes = request.getParameterValues("ticketTypes");
        String[] movieTitle = request.getParameterValues("movieTitle");
        int size = 0;
        int adultCount = 0;
        int childCount = 0;
        int seniorCount = 0;
        for(int i = 0; i < ticketTypes[0].length(); i++){
            if(ticketTypes[0].charAt(i) == 'a'){
                adultCount++;
            } else if(ticketTypes[0].charAt(i) == 'c'){
                childCount++;
            } else if(ticketTypes[0].charAt(i) == 's'){
                seniorCount++;
            }
        }
        if(childCount > 0){
            size++;
        } else if (adultCount > 0) {
            size++;
        } else if(seniorCount > 0) {
            size++;
        }

        model.addAttribute("adultCount",adultCount);
        model.addAttribute("childCount",childCount);
        model.addAttribute("seniorCount",seniorCount);
        model.addAttribute("movieTitle",movieTitle[0]);

        MovieShowing showing = movieShowingRepo.findById(Long.valueOf(showID)).get();
        System.out.println("The showing is" + showing.getShowID());


        //get the specific showing
        model.addAttribute("showing", showing); //the movie showing
        Price adultPrice = priceRepo.findByTicketType("ADULT");
        model.addAttribute("adultPrice", adultPrice);
        Price childPrice = priceRepo.findByTicketType("CHILD");
        model.addAttribute("childPrice", childPrice);
        Price seniorPrice = priceRepo.findByTicketType("SENIOR");
        model.addAttribute("seniorPrice", seniorPrice);
        Price bookingFee = priceRepo.findByTicketType("ADULT");
        model.addAttribute("bookingFee", bookingFee);
        double adultCartPrice = adultPrice.getTicketPrice() * adultCount;
        double childCartPrice = childPrice.getTicketPrice() * childCount;
        double seniorCartPrice =seniorPrice.getTicketPrice() * seniorCount;

        int ticketCount = adultCount + childCount + seniorCount;
        model.addAttribute("ticketCount", ticketCount);


        DecimalFormat df = new DecimalFormat("###.##");
        double total = adultCartPrice + childCartPrice + seniorCartPrice;
        double tax = total *0.04;
        total += tax;
        total+= adultPrice.getBookingFee();
        if(request.getParameterValues("promoCode") != null){
            String[] promotion = request.getParameterValues("promoCode");
            if(promoRepo.findByCode(promotion[0]) != null){
                total = total - (total * ((promoRepo.findByCode(promotion[0]).getPercentage()).doubleValue()/100));
                System.out.println(promotion[0]);
                System.out.println(promoRepo.findByCode(promotion[0]).getPercentage());
            }

        }
        String format1 = "";
        String format2 = "";
        String format3 = "";
        String format4 = "";
        String format5 = "";

        format1 = df.format(adultCartPrice);
        format2 = df.format(childCartPrice);
        format3 = df.format(seniorCartPrice);
        format4 = df.format(tax);
        format5 = df.format(total);


        model.addAttribute("adultCartPrice",format1);
        model.addAttribute("childCartPrice",format2);
        model.addAttribute("seniorCartPrice",format3);
        model.addAttribute("tax",format4);
        model.addAttribute("total", format5);


        //check if they have a card
        List<CreditCard> cards = new ArrayList<>();
        cards = user.getCreditCards();

        int count = 0;
        for (int i = 0; i < 3; i++) {
            if (cards.get(i).getCardType() == null) {
                count++; //card i is null
            }
        }

        if (count == 3) {
            return "cart_NoCard";
        }



        return "cart";
    }

    @GetMapping("/edit-profile-failure")
    public String showEditProfileFormFailure(User user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("address", user.getAddress());
        String search = "";
        model.addAttribute("search", search);

        return "edit_form_failure";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> listUsers = userRepo.findAll();
        model.addAttribute("listUsers", listUsers);
        String search = "";
        model.addAttribute("search", search);

        return "users";
    }

    @GetMapping("/verify")
    public String verifyUser(@Param("code") String code, Model model) {
        String search = "";
        model.addAttribute("search", search);
        if (service.verify(code)) {
            return "verify_success";
        } else {
            return "verify_fail";
        }
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String search = "";
        model.addAttribute("search", search);
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            if(priceRepo.findByTicketType("ADULT") == null) {
                Price adult = new Price();
                Price child = new Price();
                Price senior = new Price();

                adult.setTicketType("ADULT");
                child.setTicketType("CHILD");
                senior.setTicketType("SENIOR");
                adult.setTicketPrice(13.69);
                child.setTicketPrice(10.69);
                senior.setTicketPrice(12.69);
                adult.setBookingFee(1.42);
                child.setBookingFee(1.42);
                senior.setBookingFee(1.42);

                priceRepo.save(adult);
                priceRepo.save(child);
                priceRepo.save(senior);
            }
            return "login";

        }

        return "redirect:/";
    }

    @GetMapping("/login_error")
    public String loginFailurePage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String search = "";
        model.addAttribute("search", search);
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "login_error";
        }

        return "redirect:/";
    }

    @GetMapping("/seat-select/{id}")
    public String showSeatPage(@PathVariable("id") Integer id,Model model) {
        MovieShowing showing = movieShowingRepo.findById(Long.valueOf(id)).get(); //get the specific showing
        model.addAttribute("showing", showing); //the movie showing
        String search = "";
        model.addAttribute("seat", new Seat());
        model.addAttribute("user", new User());
        model.addAttribute("seatList", new SeatList());
        Boolean[] check = new Boolean[41];
        check[0] = false;
        Long checker2 = new Long(id);
        for(int i = 0; i < 41; i ++){
            String holder= Integer.toString(i);
            holder+=Long.toString(id);
            Long checker = new Long(Long.parseLong(holder));
            if( i != 0 && !(seatRepo.findBySeatID(checker).getReserved() == 0)){
                check[i] = true;
            } else { check[i] = false;}
        }
        model.addAttribute("check",check);
        return "seat-selection";
    }

    @PostMapping("/process_checkout")
    public String checkout(HttpServletRequest request, @AuthenticationPrincipal UserDetails currentUser, Model model) throws UnsupportedEncodingException, MessagingException{
        String[] holder =request.getParameterValues("seatID");

        User theUser = repo.findByEmail(currentUser.getUsername());
        Long cardNum = 0L;
        String cardType = "";
        String cardCVV = "";

        //first try for cart-noCard
        try {
            String[] ctype = request.getParameterValues("cardType");
            cardType = ctype[0];
            String[] cNum = request.getParameterValues("cardNumber");
            cardNum = Long.parseLong(cNum[0]);

            String[] cvv = request.getParameterValues("cvv");
            cardCVV = cvv[0];
        } catch (NullPointerException npe) {

            //if here, the user used a card that they already had
            String[] type = request.getParameterValues("creditCards");
            List<CreditCard> cards = cardRepo.findAll();
            CreditCard card = new CreditCard();
            card = cardRepo.findByType(type[0]);
            cardNum = card.getCardNumber();
            cardType = card.getCardType();
            cardCVV = card.getCvv();


        } //try-catch

        System.out.println(cardNum);
        System.out.println(cardType);
        System.out.println(cardCVV);
// cardNum   cardType   cardCVV


        String[] promo =  request.getParameterValues("promoCode");
        String[] price = request.getParameterValues("price");
        String[] ticketCount = request.getParameterValues("ticketCount");
        String[] showIDs= request.getParameterValues("showID2");
        String[] movieTitle = request.getParameterValues("movieTitle");
        request.getParameterValues("seatID");
        //System.out.print(holder[0]);

        int size = 0;
        for(int i = 0; i < holder[0].length(); i++){
            if(holder[0].charAt(i) == ','){
                size++;
            }
        }
        String[] reserved = new String[size];

        int previousIndex = 0;
        int count = 0;
        for(int i = 0; i < holder[0].length(); i++){

            if(holder[0].charAt(i) == ',' && count <=size-1){
                String holder2 = holder[0].substring(previousIndex,i);
                reserved[count] = holder2;
                previousIndex = i+1;
                count = count+1;

            }
        }

        //Make a booking
        double dPrice = Double.parseDouble(price[0]);
        Booking book = new Booking();
        Seat seat = seatRepo.findBySeatID(Long.parseLong(reserved[0])); //find a specific seat

        //fill booking
        // cardNum   cardType   cardCVV
        book.setCardNumber(cardNum);
        book.setCardType(cardType);

        CreditCardServices cardServices = new CreditCardServices();
        cardCVV = cardServices.secretCardEncoder(cardCVV); //encrpyt it before storing
        book.setCardCVV(cardCVV);
        Long showID = seat.getShowId();
        MovieShowing showing = movieShowingRepo.findByShowingID(Long.parseLong(showIDs[0]));
        //MovieShowing showing = movieShowingRepo.findByShowingID(showID);
        book.setDate(showing.getDate());
        book.setTime(showing.getTime());
        book.setPromoCode(promo[0]);
        book.setPrice(dPrice);
        book.setMovieTitle(showing.getMovie().getTitle());

        //attach a user
        book.setUser(theUser);
        service.sendCheckoutEmail(theUser,price,movieTitle,reserved,showIDs[0], getSiteURL(request));
        Booking booking = bookingRepo.save(book);

        Long id = booking.getBookingID();
        System.out.println(id);

        for(int i = 0; i < reserved.length; i++){
            Seat s = new Seat();
            s = seatRepo.findBySeatID(Long.parseLong(reserved[i]));
            s.setReserved(1L);

            s.setBookingID(id); //make sure the seat can be associated with the order

            seatRepo.save(s);
        }
        model.addAttribute("movieTitle", movieTitle[0]);
        model.addAttribute("total", price[0]);
        model.addAttribute("ticketCount",ticketCount[0]);
        model.addAttribute("showRepo", movieShowingRepo);
        return "checkout_verification";
    }

    @PostMapping("/process_editMovie")
    public String editMovie(HttpServletRequest request){
        String[] title = request.getParameterValues("title");
        String[] cast = request.getParameterValues("cast");
        String[] director = request.getParameterValues("director");
        String[] producer = request.getParameterValues("producer");
        String[] synopsis = request.getParameterValues("synopsis");
        String[] trailer = request.getParameterValues("trailer");
        String[] rating = request.getParameterValues("parentalguidancerating");
        String[] movieID = request.getParameterValues("movieID");
        Movie movie = movieRepo.findByMovieId(Long.parseLong(movieID[0]));
        movie.setTitle(title[0]);
        movie.setCast(cast[0]);
        movie.setDirector(director[0]);
        movie.setProducer(producer[0]);
        movie.setSynopsis(synopsis[0]);
        movie.setTrailer(trailer[0]);
        movie.setRating(rating[0]);
        movieRepo.save(movie);

        return "redirect:/";
    }

    @PostMapping("/process_seat_select")
    public String bookSeat(SeatList seats, Long showID, Model model, HttpServletRequest request) throws MessagingException, UnsupportedEncodingException {
        return "cart";
    }

    @GetMapping("/manage-prices")
    public String editPrices(Model model) {
        Price adult = priceRepo.findByTicketType("ADULT");
        Price child = priceRepo.findByTicketType("CHILD");
        Price senior = priceRepo.findByTicketType("SENIOR");

        model.addAttribute("adult",adult);
        model.addAttribute("child",child);
        model.addAttribute("senior",senior);

        return "edit_prices";
    }
    @PostMapping("/process_editPrices")
    public String processPriceEdit(HttpServletRequest request){
        String[] adultPrice = request.getParameterValues("adultPrice");
        String[] childPrice = request.getParameterValues("childPrice");
        String[] seniorPrice = request.getParameterValues("seniorPrice");
        String[] bookingFee = request.getParameterValues("bookingFee");

        Price adult = priceRepo.findByTicketType("ADULT");
        Price child = priceRepo.findByTicketType("CHILD");
        Price senior = priceRepo.findByTicketType("SENIOR");

        adult.setTicketPrice(Double.parseDouble(adultPrice[0]));
        child.setTicketPrice(Double.parseDouble(childPrice[0]));
        senior.setTicketPrice(Double.parseDouble(seniorPrice[0]));
        adult.setBookingFee(Double.parseDouble(bookingFee[0]));
        child.setBookingFee(Double.parseDouble(bookingFee[0]));
        senior.setBookingFee(Double.parseDouble(bookingFee[0]));

        priceRepo.save(adult);
        priceRepo.save(child);
        priceRepo.save(senior);



        return "admin-portal";
    }

    @GetMapping("/purchase-history")
    public String purchaseHistory(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        User theUser = repo.findByEmail(currentUser.getUsername());

        List<Booking> allOrders = bookingRepo.findByUser(theUser);
        model.addAttribute("allOrders", allOrders);
        String search = "";
        model.addAttribute("search", search);

        return "purchase-history";
    }

}