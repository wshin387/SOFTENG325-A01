package se325.assignment01.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.dto.*;
import se325.assignment01.concert.common.types.BookingStatus;
import se325.assignment01.concert.service.domain.*;
import se325.assignment01.concert.service.jaxrs.LocalDateTimeParam;
import se325.assignment01.concert.service.mapper.BookingMapper;
import se325.assignment01.concert.service.mapper.ConcertMapper;
import se325.assignment01.concert.service.mapper.PerformerMapper;
import se325.assignment01.concert.service.mapper.SeatMapper;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Path("/concert-service")
public class ConcertResource {

    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    private ExecutorService executorService = Executors.newCachedThreadPool();

    private static final Map<Long, List<Subscription>> subscriptionMap = new ConcurrentHashMap<>();

    @GET
    @Path("/concerts/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConcertById(@PathParam("id") long id){
        LOGGER.info("Retrieving concert with id: " + id);

        Concert concert;

        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();

            concert = em.find(Concert.class, id);

            if (concert == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

        } finally {
            em.close();
        }
        return Response.ok(ConcertMapper.toDTO(concert)).build();
    }

    @GET
    @Path("/concerts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllConcerts(){

        GenericEntity<List<ConcertDTO>> entity;
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try{
            em.getTransaction().begin();

            TypedQuery<Concert> concertQuery = em.createQuery("select c from Concert c", Concert.class);
            List<Concert> concertList = concertQuery.getResultList();
            List<ConcertDTO> concertDTOList = ConcertMapper.listToDTO(concertList);
            entity = new GenericEntity<List<ConcertDTO>>(concertDTOList){};
        } finally {
            em.close();
        }
        return Response.ok(entity).build();
    }

    @GET
    @Path("/concerts/summaries")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllConcertSummaries(){

        GenericEntity<List<ConcertSummaryDTO>> entity;
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try{
            em.getTransaction().begin();

            TypedQuery<Concert> concertQuery = em.createQuery("select c from Concert c", Concert.class);
            List<Concert> concertList = concertQuery.getResultList();
            List<ConcertSummaryDTO> concertSummaryDTOList= concertList.stream().map(c -> new ConcertSummaryDTO(c.getId(),c.getTitle(),c.getImage()) ).collect(Collectors.toList());
            entity = new GenericEntity<List<ConcertSummaryDTO>>(concertSummaryDTOList){};

        } finally {
            em.close();
        }

        return Response.ok(entity).build();
    }

    @GET
    @Path("/performers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPerformerById(@PathParam("id") long id){

        LOGGER.info("Retrieving performer with id: " + id);
        Performer performer;
        PerformerDTO performerDTO;
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            em.getTransaction().begin();
            performer = em.find(Performer.class, id);

            if (performer == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            performerDTO = PerformerMapper.toDTO(performer);
        } finally {
            em.close();
        }
        return Response.ok(performerDTO).build();
    }


    @GET
    @Path("/performers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllPerformers() {
        LOGGER.info("Retrieving all Performers");
        List<Performer> performerList = new ArrayList<>();
        List<PerformerDTO> performerDTOList;
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try{
            em.getTransaction().begin();
            TypedQuery<Performer> performerQuery = em.createQuery("select p from Performer p",Performer.class);
            performerList = performerQuery.getResultList();
            if (performerList == null){
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            em.getTransaction().commit();
            performerDTOList = PerformerMapper.listToDTO(performerList);
        } finally {
            em.close();
        }
        return Response.ok(performerDTOList).build();
    }

    @POST
    @Path("/login")
    public Response login(UserDTO userDTO){
        NewCookie newCookie;
        String username = userDTO.getUsername();
        String password = userDTO.getPassword();
        EntityManager em = PersistenceManager.instance().createEntityManager();
        Response response;

        try {
            LOGGER.info("Attempting Login");
            em.getTransaction().begin();

            TypedQuery<User> query = em.createQuery("select u from User u where u.username = :inputUserName AND u.password = :inputPassword", User.class)
                .setParameter("inputUserName", username)
                .setParameter("inputPassword", password)
                /*.setLockMode(LockModeType.OPTIMISTIC)*/;

            User user = query.getResultList().stream().findFirst().orElse(null);
            if (user == null ){
                response = Response.status(Response.Status.UNAUTHORIZED).build();
            } else {

                NewCookie cookie = new NewCookie(Config.AUTH_COOKIE, UUID.randomUUID().toString());
                user.setCookie(cookie.getValue());
                response = Response.ok().cookie(cookie).build();
                em.merge(user);
                em.getTransaction().commit();
            }


        } finally {
            em.close();
        }

        return response;
    }

    @POST
    @Path("/bookings")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response attemptBooking(BookingRequestDTO bookingRequestDTO, @CookieParam(Config.AUTH_COOKIE) Cookie cookie ) {
        Booking booking;
        int numSeatsRemaining;
        int totalSeats;

        if (cookie == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();

            TypedQuery<User> userQuery = em.createQuery("select u from User u where u.cookie = :cookie", User.class).setParameter("cookie", cookie.getValue());
            User user = userQuery.getResultList().stream().findFirst().orElse(null); //gets a single user
            //User user = userQuery.getSingleResult();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            //check that concert exists for given request date
            Concert concert = em.find(Concert.class, bookingRequestDTO.getConcertId());
            if (concert == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (!concert.getDates().contains(bookingRequestDTO.getDate())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            //try book the seats and persist the booking
            booking = this.bookSeats(bookingRequestDTO, user);
            //booking failed and returned null because at least one requested seat as been booked
            if (booking == null){
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            //get free seats and total seats for publish calculations
            TypedQuery<Seat> freeSeatsQuery = em.createQuery("select s from Seat s where s.date = :requestDate and s.isBooked=false ", Seat.class).setParameter("requestDate", bookingRequestDTO.getDate());
            numSeatsRemaining = freeSeatsQuery.getResultList().size();
            TypedQuery<Seat> totalSeatsQuery = em.createQuery("select s from Seat s where s.date = :requestDate", Seat.class).setParameter("requestDate", bookingRequestDTO.getDate());
            totalSeats = totalSeatsQuery.getResultList().size();
        }finally {
            em.close();
        }
        this.notifySubcribers(numSeatsRemaining, totalSeats, bookingRequestDTO.getConcertId(), bookingRequestDTO.getDate());
        return Response.created(URI.create("concert-service/bookings/"+booking.getId())).cookie(appendCookie(cookie)).build();
    }

    private Booking bookSeats(BookingRequestDTO bookingRequestDTO, User user){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        List<Seat> seats = new ArrayList<>();
        Booking booking;
        try {
            em.getTransaction().begin();
            // one query to get all requested seats
            TypedQuery<Seat> query = em.createQuery("select s from Seat s where s.date = :requestDate and s.isBooked = false and s.label in :seats", Seat.class);
            query.setParameter("seats", bookingRequestDTO.getSeatLabels());
            query.setParameter("requestDate", bookingRequestDTO.getDate());
            query.setLockMode(LockModeType.OPTIMISTIC);

            seats = query.getResultList();

            //at least one seat has been booked
            if (seats.size() != bookingRequestDTO.getSeatLabels().size()) {
                return null;
            }
            //all seats are free to book
            for (Seat seat : seats) {
                seat.setBooked(true);
            }
            booking = new Booking(bookingRequestDTO.getConcertId(), bookingRequestDTO.getDate(), seats, user);
            em.persist(booking);
            em.getTransaction().commit();
        } catch (OptimisticLockException e){
            //retry booking
            em.close();
            booking = this.bookSeats(bookingRequestDTO, user);
        } finally{
            em.close();
        }
        return booking;
    }


//    @POST
//    @Path("/bookings")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response makeBooking(BookingRequestDTO bookingRequestDTO, @CookieParam(Config.AUTH_COOKIE) Cookie cookie){
//        if (cookie == null){
//            return Response.status(Response.Status.UNAUTHORIZED).build();
//        }
//
//        Booking booking;
//        int numberOfSeats, numberOfAvailableSeats;
//        EntityManager em = PersistenceManager.instance().createEntityManager();
//
//        try {
//            LOGGER.info("Attempting to log in");
//            em.getTransaction().begin();
//            TypedQuery<User> userQuery = em.createQuery("select u from User u where u.cookie = :cookie", User.class).setParameter("cookie", cookie.getValue());
//            User user = userQuery.getResultList().stream().findFirst().orElse(null); //gets a single user
//            //User user = userQuery.getSingleResult();
//            if (user == null) {
//                return Response.status(Response.Status.UNAUTHORIZED).build();
//            }
//
//            LOGGER.info("Login successful");
//
//            Concert concert =  em.find(Concert.class, bookingRequestDTO.getConcertId());
//            if (concert == null) {
//                return Response.status(Response.Status.BAD_REQUEST).build();
//            }
//
//            if (!concert.getDates().contains(bookingRequestDTO.getDate())){
//                return Response.status(Response.Status.BAD_REQUEST).build();
//            }
//
//            // one query to get all requested seats
//            TypedQuery<Seat> seatQuery = em.createQuery("select s from Seat s where s.date = :requestDate and s.isBooked = false and s.label in :seats", Seat.class)
//                .setParameter("seats",bookingRequestDTO.getSeatLabels())
//                .setParameter("requestDate",bookingRequestDTO.getDate());
//
//            List<Seat> seatList = seatQuery.getResultList();
//
//
//            //check that all seats are available
//            if (seatList.size() != bookingRequestDTO.getSeatLabels().size()){
//                return Response.status(Response.Status.FORBIDDEN).build();
//            }
//
//            //set all seats to booked
//            for (Seat seat : seatList){
//                seat.setBooked(true);
//            }
//
//            //get number of remaining available seats
//            TypedQuery<Seat> availableSeatsQuery = em.createQuery("select s from Seat s where s.date = :requestDate and s.isBooked = false ", Seat.class)
//                    .setParameter("requestDate",bookingRequestDTO.getDate());
//            numberOfAvailableSeats = availableSeatsQuery.getResultList().size();
//
//            TypedQuery<Seat> allSeatsQuery = em.createQuery("select s from Seat s where s.date = :requestDate",Seat.class).setParameter("requestDate",bookingRequestDTO.getDate());
//            numberOfSeats = allSeatsQuery.getResultList().size();
//
//            booking = new Booking(bookingRequestDTO.getConcertId(),bookingRequestDTO.getDate(),seatList,user);
//            em.persist(booking);
//            em.getTransaction().commit();
//
//
//        }finally {
//            em.close();
//        }
//
//        //return Response.status(Response.Status.CREATED).build();
//        this.notifySubcribers(numberOfAvailableSeats, numberOfSeats, bookingRequestDTO.getConcertId(), bookingRequestDTO.getDate());
//        return Response.created(URI.create("concert-service/bookings/"+booking.getId())).cookie(appendCookie(cookie)).build();
//    }


    @GET
    @Path("/bookings/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBookingsByClientId(@PathParam("id") long id, @CookieParam(Config.AUTH_COOKIE) Cookie cookie){
        if (cookie == null){
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        BookingDTO bookingDTO;
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try{
            em.getTransaction().begin();

            //check if user is authenticated
            TypedQuery<User> userQuery = em.createQuery("select u from User u where u.cookie=:cookie", User.class);
            userQuery.setParameter("cookie",cookie.getValue());

            User user = userQuery.getResultList().stream().findFirst().orElse(null); //gets 1 user
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Booking booking = em.find(Booking.class, id);
            if (booking == null){
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            //check for forbidden request
            if (booking.getUser().getId() != user.getId()){
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            bookingDTO = BookingMapper.toDTO(booking);
        } finally {
            em.close();
        }

        return Response.ok(bookingDTO).cookie(appendCookie(cookie)).build();
    }

    @GET
    @Path("/bookings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllBookingsForUser(@CookieParam(Config.AUTH_COOKIE)Cookie cookie){
        //check cookie
        if (cookie == null){
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        EntityManager em = PersistenceManager.instance().createEntityManager();
        GenericEntity<List<BookingDTO>> entity;

        try{
            em.getTransaction().begin();

            TypedQuery<User> userQuery = em.createQuery("select u from User u where u.cookie=:cookie", User.class)
                .setParameter("cookie",cookie.getValue());

            User user = userQuery.getResultList().stream().findFirst().orElse(null); //gets 1 user
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            //get all bookings
            TypedQuery<Booking> bookingQuery = em.createQuery("select b from Booking b where b.user = :user", Booking.class);
            bookingQuery.setParameter("user", user);
            List<Booking> bookingList = bookingQuery.getResultList();
            //convert to BookingDTO
            List<BookingDTO> bookingDTOList = bookingList.stream().map(b -> BookingMapper.toDTO(b)).collect(Collectors.toList());
            entity = new GenericEntity<List<BookingDTO>>(bookingDTOList){};
        } finally {
            em.close();
        }
        return Response.ok(entity).cookie(appendCookie(cookie)).build();
    }

    @GET
    @Path("/seats/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSeats(@PathParam("date")LocalDateTimeParam dateTimeParam, @QueryParam("status") BookingStatus bookingStatus){

        LocalDateTime date = dateTimeParam.getLocalDateTime();
        GenericEntity<List<SeatDTO>> entity;
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            em.getTransaction().begin();
            TypedQuery<Seat> seatQuery; //query depends on booking status

            if (bookingStatus == BookingStatus.Any){
                seatQuery = em.createQuery("select s from Seat s Where s.date = :date", Seat.class)
                        .setParameter("date", date);
            } else {

                boolean isBooked = (bookingStatus == BookingStatus.Booked);
                seatQuery = em.createQuery("select s from Seat s Where s.date=:date and s.isBooked=:isBooked",Seat.class)
                    .setParameter("isBooked", isBooked)
                    .setParameter("date", date);
            }

            LOGGER.info("retrieved " + seatQuery.getResultList());

            List<Seat> bookedSeatList = seatQuery.getResultList();
            List<SeatDTO> bookedDTOSeatList = bookedSeatList.stream().map(s -> SeatMapper.toDTO(s)).collect(Collectors.toList());
            entity = new GenericEntity<List<SeatDTO>>(bookedDTOSeatList){};

        } finally {
            em.close();
        }
        return Response.ok(entity).build();
    }


    private NewCookie appendCookie(Cookie clientCookie){
        return new NewCookie(Config.AUTH_COOKIE, clientCookie.getValue());
    }

    @POST
    @Path("/subscribe/concertInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void concertInfoSubscribe(@Suspended AsyncResponse response, @CookieParam(Config.AUTH_COOKIE)Cookie cookie, ConcertInfoSubscriptionDTO concertInfoSubscriptionDTO){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        LOGGER.info("subscribed");
        if (cookie == null){
            LOGGER.info("cookie = null");
            executorService.submit(()->{
                response.resume(Response.status(Response.Status.UNAUTHORIZED).build());
            });
            return;
        }
        try{
            em.getTransaction().begin();

            TypedQuery<User> userQuery = em.createQuery("select u from User u where u.cookie = :cookie", User.class)
                    .setParameter("cookie", cookie.getValue());
            User user = userQuery.getResultList().stream().findFirst().orElse(null); //gets a single user

            if (user == null){
                executorService.submit(()-> {
                    response.resume(Response.status(Response.Status.UNAUTHORIZED).build());
                });
                return;
            }

            long id = concertInfoSubscriptionDTO.getConcertId();
            Concert concert = em.find(Concert.class,id);
            if (concert == null){

                executorService.submit(()-> {
                    response.resume(Response.status(Response.Status.BAD_REQUEST).build());
                });
                return;
            }
            if (!concert.getDates().contains(concertInfoSubscriptionDTO.getDate())){
                executorService.submit(()-> {
                    response.resume(Response.status(Response.Status.BAD_REQUEST).build());
                });
                return;
            }
            //valid subscription info add sub to user
            List<Subscription> subs = subscriptionMap.getOrDefault(concert.getId(), new ArrayList<>());
            subs.add(new Subscription(concertInfoSubscriptionDTO, response));
            subscriptionMap.put(concert.getId(),subs);
        }finally {
            em.close();
        }
    }

    public void notifySubcribers(int numRemainingSeats, int totalSeats, long concertId, LocalDateTime date){
        List<Subscription> subs = subscriptionMap.get(concertId);
        if (subs == null){
            return;
        }
        List<Subscription> newSubs = new ArrayList<>();
        for (Subscription sub : subs){
            ConcertInfoSubscriptionDTO info = sub.getConcertInfoSubscriptionDTO();
            if (info.getDate().isEqual(date)){
                if (info.getPercentageBooked() < 100 - (numRemainingSeats*100)/totalSeats){
                    AsyncResponse response = sub.getResponse();
                    synchronized (response){
                        ConcertInfoNotificationDTO notification = new ConcertInfoNotificationDTO(numRemainingSeats);
                        response.resume(Response.ok(notification).build());
                    }
                } else {
                    newSubs.add(sub);
                }
            } else {
                newSubs.add(sub);
            }
        }
        subscriptionMap.put(concertId, newSubs);
    }
}

/** Encapsulates a concertInfoSubcriptionDTO and its associated async response
 *  Stored in subcriber map where it is used in notifysubscribers()
 */
class Subscription{

    private ConcertInfoSubscriptionDTO concertInfoSubscriptionDTO;
    private AsyncResponse response;

    public Subscription(ConcertInfoSubscriptionDTO concertInfoSubscriptionDTO, AsyncResponse response){
        this.concertInfoSubscriptionDTO = concertInfoSubscriptionDTO;
        this.response = response;
    }

    public AsyncResponse getResponse(){ return this.response; }
    public ConcertInfoSubscriptionDTO getConcertInfoSubscriptionDTO(){ return this.concertInfoSubscriptionDTO; }

}