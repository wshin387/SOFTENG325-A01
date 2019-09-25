package se325.assignment01.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.dto.BookingDTO;
import se325.assignment01.concert.common.dto.BookingRequestDTO;
import se325.assignment01.concert.common.dto.ConcertInfoNotificationDTO;
import se325.assignment01.concert.common.dto.ConcertInfoSubscriptionDTO;
import se325.assignment01.concert.service.config.Config;
import se325.assignment01.concert.service.domain.Booking;
import se325.assignment01.concert.service.domain.Concert;
import se325.assignment01.concert.service.domain.Seat;
import se325.assignment01.concert.service.domain.User;
import se325.assignment01.concert.service.mapper.BookingMapper;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Path("/concert-service")
public class BookingResource {

    private static Logger LOGGER = LoggerFactory.getLogger(BookingResource.class);
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private static final Map<Long, List<Subscription>> subscribersMap = new ConcurrentHashMap<>(); //ConcertId to List of Subscriptions

    /*
     * Returns a User from the database whose cookie matches the cookie given as argument.
     * If no such user can be found, null is returned.
     */
    private User getAuthenticatedUser(EntityManager em, Cookie cookie) {
        TypedQuery<User> userQuery = em.createQuery("select u from User u where u.cookie = :cookie", User.class)
                .setParameter("cookie", cookie.getValue());
        User user = userQuery.getResultList().stream().findFirst().orElse(null);

        return user;
    }

    private NewCookie appendCookie(Cookie clientCookie) {
        return new NewCookie(Config.AUTH_COOKIE, clientCookie.getValue());
    }

    /**
     * Attempts to make a booking. Also notifies all subscribers of the concert that is being booked.
     */
    @POST
    @Path("/bookings")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response tryMakeBooking(BookingRequestDTO bookingRequestDTO, @CookieParam(Config.AUTH_COOKIE) Cookie cookie ) {

        if (cookie == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Booking booking;
        //int variables used as inputs for notifySubscribers()
        int availableNumberOfSeats;
        int totalNumberOfSeats;
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            em.getTransaction().begin();

            User user = this.getAuthenticatedUser(em, cookie);

            if (user == null ) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Concert concert = em.find(Concert.class, bookingRequestDTO.getConcertId());
            //check concert exists with the supplied concert id
            if (concert == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            //check that date exists within concert
            if (!concert.getDates().contains(bookingRequestDTO.getDate())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            //try to persist booking
            booking = this.makeBooking(bookingRequestDTO, user);

            //failed to make booking due to unavailability of seats
            if (booking == null){
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            //get number of seats for concert
            //needed to calculate availability and notify subscribers
            TypedQuery<Seat> availableSeatsQuery = em.createQuery("select s from Seat s where s.date = :requestDate and s.isBooked = false ", Seat.class)
                    .setParameter("requestDate", bookingRequestDTO.getDate());
            availableNumberOfSeats = availableSeatsQuery.getResultList().size();

            TypedQuery<Seat> allSeatsQuery = em.createQuery("select s from Seat s where s.date = :requestDate", Seat.class)
                    .setParameter("requestDate", bookingRequestDTO.getDate());
            totalNumberOfSeats = allSeatsQuery.getResultList().size();

        } finally {
            em.close();
        }

        this.notifySubscribers(availableNumberOfSeats, totalNumberOfSeats, bookingRequestDTO.getConcertId(), bookingRequestDTO.getDate());
        return Response.created(URI.create("concert-service/bookings/"+booking.getId())).cookie(appendCookie(cookie)).build();
    }

    /*
    Persist modification of relevant seats to the database.
    Skips the user authentication query
    Called within tryMakeBooking()
     */
    private Booking makeBooking(BookingRequestDTO bookingRequestDTO, User user){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        Booking booking;

        try {
            em.getTransaction().begin();

            TypedQuery<Seat> seatQuery = em.createQuery("select s from Seat s where s.date = :requestDate and s.isBooked = false and s.label in :seats", Seat.class)
                    .setParameter("seats", bookingRequestDTO.getSeatLabels())
                    .setParameter("requestDate", bookingRequestDTO.getDate())
                    .setLockMode(LockModeType.OPTIMISTIC);
            List<Seat> seatList = seatQuery.getResultList();

            //check if all seats are available
            if (seatList.size() != bookingRequestDTO.getSeatLabels().size()) {
                return null;
            }

            //set all seats too booked
            for (Seat seat : seatList) {
                seat.setBooked(true);
            }
            booking = new Booking(bookingRequestDTO.getConcertId(), bookingRequestDTO.getDate(), seatList, user);
            em.persist(booking);

            em.getTransaction().commit();

        } catch (OptimisticLockException e) {
            em.close();
            booking = this.makeBooking(bookingRequestDTO, user); //retry, skip authentication of user
        } finally {
            em.close();
        }
        return booking;
    }

    /*
    Returns bookings made by a client filtered by Booking id
     */
    @GET
    @Path("/bookings/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBookingsById(@PathParam("id") long id, @CookieParam(Config.AUTH_COOKIE) Cookie cookie){
        if (cookie == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        BookingDTO bookingDTO;
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try{
            em.getTransaction().begin();

            User user = this.getAuthenticatedUser(em, cookie);

            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Booking booking = em.find(Booking.class, id);
            if (booking == null) {
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
    public Response getAllBookingsForUser(@CookieParam(Config.AUTH_COOKIE) Cookie cookie){
        //check cookie
        if (cookie == null){
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        EntityManager em = PersistenceManager.instance().createEntityManager();
        GenericEntity<List<BookingDTO>> entity;

        try{
            em.getTransaction().begin();

            User user = this.getAuthenticatedUser(em, cookie);

            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            //get all bookings
            TypedQuery<Booking> bookingQuery = em.createQuery("select b from Booking b where b.user = :user", Booking.class)
                .setParameter("user", user);
            List<Booking> bookingList = bookingQuery.getResultList();

            //convert to List of BookingDTOs
            List<BookingDTO> bookingDTOList = BookingMapper.listToDTO(bookingList);
            entity = new GenericEntity<List<BookingDTO>>(bookingDTOList){};
        } finally {
            em.close();
        }
        return Response.ok(entity).cookie(appendCookie(cookie)).build();
    }


    @POST
    @Path("/subscribe/concertInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void concertInfoSubscribe(@Suspended AsyncResponse response, @CookieParam(Config.AUTH_COOKIE) Cookie cookie, ConcertInfoSubscriptionDTO concertInfoSubscriptionDTO){
        EntityManager em = PersistenceManager.instance().createEntityManager();

        if (cookie == null) {
            LOGGER.info("Unauthorized to subscribe");
            executorService.submit(()->{
                response.resume(Response.status(Response.Status.UNAUTHORIZED).build());
            });
            return;
        }
        try {
            em.getTransaction().begin();

            User user = this.getAuthenticatedUser(em, cookie);

            if (user == null){
                executorService.submit(()-> {
                    response.resume(Response.status(Response.Status.UNAUTHORIZED).build());
                });
                return;
            }

            long id = concertInfoSubscriptionDTO.getConcertId();
            Concert concert = em.find(Concert.class,id);

            //if no such concert exists
            if (concert == null){
                executorService.submit(()-> {
                    response.resume(Response.status(Response.Status.BAD_REQUEST).build());
                });
                return;
            }

            //if no such date exist
            if (!concert.getDates().contains(concertInfoSubscriptionDTO.getDate())) {
                executorService.submit(()-> {
                    response.resume(Response.status(Response.Status.BAD_REQUEST).build());
                });
                return;
            }

            //add Subscription to list of subscribers to concert
            List<Subscription> subscribersList = subscribersMap.getOrDefault(concert.getId(), new ArrayList<>());
            subscribersList.add(new Subscription(concertInfoSubscriptionDTO, response));
            subscribersMap.put(concert.getId(), subscribersList);

        } finally {
            em.close();
        }
    }

    public void notifySubscribers(int availableNumberOfSeats, int totalNumberOfSeats, long concertId, LocalDateTime date){
        List<Subscription> subscriberList = subscribersMap.get(concertId);
        if (subscriberList == null) {
            return;
        }

        List<Subscription> newSubscriptions = new ArrayList<>();

        for (Subscription subscriber : subscriberList) {
            ConcertInfoSubscriptionDTO concertInfoSubscriptionDTO = subscriber.getConcertInfoSubscriptionDTO();
            if (concertInfoSubscriptionDTO.getDate().isEqual(date)){
                if (concertInfoSubscriptionDTO.getPercentageBooked() < 100 - availableNumberOfSeats * 100 / totalNumberOfSeats) {
                    AsyncResponse response = subscriber.getResponse();

                    synchronized (response) {
                        ConcertInfoNotificationDTO notification = new ConcertInfoNotificationDTO(availableNumberOfSeats);
                        response.resume(Response.ok(notification).build());
                    }
                } else {
                    newSubscriptions.add(subscriber);
                }
            } else {
                newSubscriptions.add(subscriber);
            }
        }
        subscribersMap.put(concertId, newSubscriptions);
    }
}

/**
 * The Subscription class encapsulates a tuple containing a ConcertInfoSubscriptionDTO and an AsyncResponse
 */
class Subscription {

    private ConcertInfoSubscriptionDTO concertInfoSubscriptionDTO;
    private AsyncResponse response;

    public Subscription(ConcertInfoSubscriptionDTO concertInfoSubscriptionDTO, AsyncResponse response){
        this.concertInfoSubscriptionDTO = concertInfoSubscriptionDTO;
        this.response = response;
    }

    public AsyncResponse getResponse(){ return this.response; }
    public ConcertInfoSubscriptionDTO getConcertInfoSubscriptionDTO(){ return this.concertInfoSubscriptionDTO; }

}

