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
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/concert-service")
public class ConcertResource {

    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    //private EntityManager em = PersistenceManager.instance().createEntityManager();


    @GET
    @Path("/concerts/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConcert(@PathParam("id") long id){
        LOGGER.info("Retrieving concert with id: " + id);
        Concert concert; //domain model
        ConcertDTO concertDTO;
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();
            concert = em.find(Concert.class, id);
            if (concert == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            concertDTO = ConcertMapper.toDTO(concert);
        } finally {
            em.close();
        }
        return Response.ok(concertDTO).build();
    }

    @GET
    @Path("/concerts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllConcerts(){
        GenericEntity<List<ConcertDTO>> entity;
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try{
            em.getTransaction().begin();
            TypedQuery<Concert> query = em.createQuery("select c from Concert c", Concert.class);
            List<Concert> allConcerts = query.getResultList();
            List<ConcertDTO> allDTOConcerts= allConcerts.stream().map(c -> ConcertMapper.toDTO(c)).collect(Collectors.toList());
            entity = new GenericEntity<List<ConcertDTO>>(allDTOConcerts){};
        } finally {
            em.close();
        }
        return Response.ok(entity).build();
    }

    @GET
    @Path("/concerts/summaries")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllConcertSummaries(){
        GenericEntity<List<ConcertSummaryDTO>> entity; //id, title, imagename
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try{
            em.getTransaction().begin();
            TypedQuery<Concert> query = em.createQuery("select c from Concert c", Concert.class);
            List<Concert> allConcerts = query.getResultList();
            List<ConcertSummaryDTO> allSummaries= allConcerts.stream().map(c -> new ConcertSummaryDTO(c.getId(),c.getTitle(),c.getImage()) ).collect(Collectors.toList());
            entity = new GenericEntity<List<ConcertSummaryDTO>>(allSummaries){};
        } finally {
            em.close();
        }        return Response.ok(entity).build();
    }

    @GET
    @Path("/performers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPerformer(@PathParam("id") long id){
        LOGGER.info("Retrieving performer with id: " + id);
        Performer performer; //domain model
        PerformerDTO entity;
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();
            performer = em.find(Performer.class, id);
            if (performer == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            entity = PerformerMapper.toDTO(performer);
        } finally {
            em.close();
        }
        return Response.ok(entity).build();
    }


    @GET
    @Path("/performers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveAllPerformers() {
        LOGGER.info("Retrieving all Performers");
        List<Performer> performers = new ArrayList<>();
        List<PerformerDTO> dto;
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try{
            em.getTransaction().begin();
            TypedQuery<Performer> performerQuery = em.createQuery("select c from Performer c",Performer.class);
            performers = performerQuery.getResultList();
            if (performers==null){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            em.getTransaction().commit();
            dto = PerformerMapper.listToDTO(performers);
        } finally {
            // When you're done using the EntityManager, close it to free up resources.
            em.close();
        }
        return Response.ok(dto).build();
    }

    @POST
    @Path("/login")
    public Response login(UserDTO userDTO){
        NewCookie newCookie;
        String username = userDTO.getUsername();
        String password = userDTO.getPassword();
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            LOGGER.info("Attempting Login");
            em.getTransaction().begin();
            TypedQuery<User> concertQuery = em.createQuery("select c from User c ",User.class);
            List<User> users = concertQuery.getResultList();

            boolean verified = false;
            User user = null;
            for (User u: users){
                String currentUsername = u.getUsername();
                String currentPassword = u.getPassword();

                if ((currentPassword.equals(password)) && (currentUsername.equals(username))) {
                    verified = true;
                    user = u;
                    break;
                }
            }

            if (!verified){
                LOGGER.info("Login Failed: incorrect credentials");
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            LOGGER.info("Login successful");


            newCookie = new NewCookie(Config.AUTH_COOKIE, UUID.randomUUID().toString());
            user.setCookie(newCookie.getValue());
            em.merge(user);
            em.getTransaction().commit();
        } finally {
            // When you're done using the EntityManager, close it to free up resources.
            em.close();
        }

        return Response.ok().cookie(newCookie).build();
    }


    @POST
    @Path("/bookings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response makeBooking(BookingRequestDTO bookingRequestDTO, @CookieParam(Config.AUTH_COOKIE) Cookie cookie){
        if (cookie==null){
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Booking booking;
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            LOGGER.info("Attempting Login");
            em.getTransaction().begin();
            TypedQuery<User> userQuery = em.createQuery("select u from User u where u.cookie = :cookie", User.class).setParameter("cookie", cookie.getValue());
            User user = userQuery.getResultList().stream().findFirst().orElse(null);
            //User user = userQuery.getSingleResult();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            LOGGER.info("Login successful");

            Concert concert =  em.find(Concert.class, bookingRequestDTO.getConcertId());
            if (concert == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            if (!concert.getDates().contains(bookingRequestDTO.getDate())){
                return Response.status(Response.Status.BAD_REQUEST).build();
            }


            // one query to get all requested seats
            TypedQuery<Seat> query = em.createQuery("select s from Seat s where s.date = :requestDate and s.isBooked = false and s.label in :seats", Seat.class);
            query.setParameter("seats",bookingRequestDTO.getSeatLabels());
            query.setParameter("requestDate",bookingRequestDTO.getDate());
            List<Seat> seats = query.getResultList();


            //at least one seat has been booked
            if (seats.size() != bookingRequestDTO.getSeatLabels().size()){
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            //all seats are free to book
            for (Seat seat : seats){
                seat.setBooked(true);
            }
            //get number of free seats left
            TypedQuery<Seat> freeSeatsQuery = em.createQuery("select s from Seat s where s.date = :requestDate and s.isBooked=false ", Seat.class).setParameter("requestDate",bookingRequestDTO.getDate());
            int numSeatsRemaining = freeSeatsQuery.getResultList().size();
            TypedQuery<Seat> totalSeatsQuery = em.createQuery("select s from Seat s where s.date = :requestDate",Seat.class).setParameter("requestDate",bookingRequestDTO.getDate());
            int totalSeats = totalSeatsQuery.getResultList().size();
            booking = new Booking(bookingRequestDTO.getConcertId(),bookingRequestDTO.getDate(),seats,user);
            em.persist(booking);
            em.getTransaction().commit();


        }finally {
            em.close();
        }

        //return Response.status(Response.Status.CREATED).build();
        return Response.created(URI.create("concert-service/bookings/"+booking.getId())).cookie(appendCookie(cookie)).build();
    }


    @GET
    @Path("/bookings/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBookingsForClient(@PathParam("id") long id, @CookieParam(Config.AUTH_COOKIE) Cookie cookie){
        if (cookie == null){
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        BookingDTO bookingDTO;
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try{
            em.getTransaction().begin();
            //authenticate
            TypedQuery<User> userQuery = em.createQuery("select u from User u where u.cookie=:cookie", User.class);
            userQuery.setParameter("cookie",cookie.getValue());
            User user = userQuery.getResultList().stream().findFirst().orElse(null);
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Booking booking = em.find(Booking.class, id);
            if (booking == null){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            //check if user matches who made the booking
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
        if (cookie == null){
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        EntityManager em = PersistenceManager.instance().createEntityManager();
        GenericEntity<List<BookingDTO>> entity;
        try{
            em.getTransaction().begin();
            TypedQuery<User> authQuery = em.createQuery("select u from User u where u.cookie=:cookie", User.class);
            authQuery.setParameter("cookie",cookie.getValue());
            User user = authQuery.getSingleResult();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }


            // get all bookings for user

            TypedQuery<Booking> bookingQuery = em.createQuery("select b from Booking b where b.user = :user", Booking.class);
            bookingQuery.setParameter("user", user);
            List<Booking> results = bookingQuery.getResultList();
            List<BookingDTO> resultsDTO = results.stream().map(b -> BookingMapper.toDTO(b)).collect(Collectors.toList());
            entity = new GenericEntity<List<BookingDTO>>(resultsDTO){};
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
        //get all seats for the date filtered by status
        try{
            em.getTransaction().begin();
            TypedQuery<Seat> query;
            if (bookingStatus == BookingStatus.Any){
                query = em.createQuery("select s from Seat s Where s.date=:date",Seat.class);
            } else {
                boolean isBooked = (bookingStatus == BookingStatus.Booked) ? true : false;
                query = em.createQuery("select s from Seat s Where s.date=:date and s.isBooked=:isBooked",Seat.class);
                query.setParameter("isBooked", isBooked);
            }
            query.setParameter("date", date);
            LOGGER.info("retrieved " + query.getResultList());
            List<Seat> bookedSeats = query.getResultList();
            List<SeatDTO> bookedDTOSeats= bookedSeats.stream().map(s -> SeatMapper.toDTO(s)).collect(Collectors.toList());
            entity = new GenericEntity<List<SeatDTO>>(bookedDTOSeats){};
        } finally {
            em.close();
        }
        return Response.ok(entity).build();
    }


    private NewCookie appendCookie(Cookie clientCookie){
        return new NewCookie(Config.AUTH_COOKIE, clientCookie.getValue());
    }
}