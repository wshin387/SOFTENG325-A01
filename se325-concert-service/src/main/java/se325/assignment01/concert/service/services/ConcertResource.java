package se325.assignment01.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.dto.*;
import se325.assignment01.concert.service.domain.*;
import se325.assignment01.concert.service.mapper.BookingMapper;
import se325.assignment01.concert.service.mapper.ConcertMapper;
import se325.assignment01.concert.service.mapper.PerformerMapper;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/concert-service")
public class ConcertResource {

    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    //private EntityManager em = PersistenceManager.instance().createEntityManager();
    @GET
    @Path("/concerts/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveConcert(@PathParam("id") long id) {
        LOGGER.info("Retrieving concert with identifier: " + id);

        Concert concert;
        ConcertDTO dto;
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try{
            em.getTransaction().begin();
            concert = em.find(Concert.class, id);
            em.getTransaction().commit();
            if (concert==null){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            dto = ConcertMapper.toDTO(concert);
        } finally {
            // When you're done using the EntityManager, close it to free up resources.
            em.close();
        }
        return Response.ok(dto).build();
    }

    @GET
    @Path("/concerts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveAllConcerts() {
        LOGGER.info("Retrieving all Concerts");
        List<Concert> concerts = new ArrayList<>();
        List<ConcertDTO> dto;
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try{
            em.getTransaction().begin();
            TypedQuery<Concert> concertQuery = em.createQuery("select c from Concert c",Concert.class);
            concerts = concertQuery.getResultList();
            if (concerts==null){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            em.getTransaction().commit();
            dto = ConcertMapper.listToDTO(concerts);
        } finally {
            // When you're done using the EntityManager, close it to free up resources.
            em.close();
        }
        return Response.ok(dto).build();
    }

    @GET
    @Path("/concerts/summaries")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveTitles() {
        LOGGER.info("Getting all summaries");
        List<Concert> concerts = new ArrayList<>();
        List<ConcertSummaryDTO> dto;
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try{
            em.getTransaction().begin();
            TypedQuery<Concert> concertQuery = em.createQuery("select c from Concert c",Concert.class);
            concerts = concertQuery.getResultList();
            if (concerts==null){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            em.getTransaction().commit();
            dto = ConcertMapper.listToSummaryDTO(concerts);
        } finally {
            // When you're done using the EntityManager, close it to free up resources.
            em.close();
        }
        return Response.ok(dto).build();
    }


    @GET
    @Path("/performers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrievePerformer(@PathParam("id") long id) {
        LOGGER.info("Retrieving performer with identifier: " + id);

        Performer performer;
        PerformerDTO dto;
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try{
            em.getTransaction().begin();
            performer = em.find(Performer.class, id);
            if (performer==null){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            em.getTransaction().commit();
            dto = PerformerMapper.toDTO(performer);
        } finally {
            // When you're done using the EntityManager, close it to free up resources.
            em.close();
        }
        return Response.ok(dto).build();
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

    private NewCookie makeCookie(String clientId) {
        NewCookie newCookie = new NewCookie(clientId, UUID.randomUUID().toString());
        LOGGER.info("Generated cookie: " + newCookie.getValue());
        return newCookie;
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
            TypedQuery<User> query = em.createQuery("select u from User u where u.token=:token", User.class);
            query.setParameter("token",cookie.getValue());
            User user = query.getSingleResult();
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


    private NewCookie appendCookie(Cookie clientCookie){
        return new NewCookie(Config.AUTH_COOKIE, clientCookie.getValue());
    }
}