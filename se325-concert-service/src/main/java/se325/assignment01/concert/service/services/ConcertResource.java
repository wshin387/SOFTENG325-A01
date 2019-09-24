package se325.assignment01.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.dto.*;
import se325.assignment01.concert.service.domain.Concert;
import se325.assignment01.concert.service.domain.Performer;
import se325.assignment01.concert.service.domain.User;
import se325.assignment01.concert.service.mapper.ConcertMapper;
import se325.assignment01.concert.service.mapper.PerformerMapper;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/concert-service")
public class ConcertResource {

    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    private EntityManager em = PersistenceManager.instance().createEntityManager();
    @GET
    @Path("/concerts/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveConcert(@PathParam("id") long id) {
        LOGGER.info("Retrieving concert with identifier: " + id);

        Concert concert;
        ConcertDTO dto;
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


            String cookie = user.getCookie();
            if (cookie == null) {
                newCookie = makeCookie(Config.AUTH_COOKIE);
                user.setCookie(newCookie.getValue());
            }else{
                newCookie = new NewCookie(Config.AUTH_COOKIE, cookie);
            }
            em.merge(user);

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

        //
        try {
            LOGGER.info("Attempting Login");
            em.getTransaction().begin();
            TypedQuery<User> concertQuery = em.createQuery("select u from User u where :cookie = u.cookie", User.class).setParameter("cookie", cookie);
            List<User> users = concertQuery.getResultList();

            if (users.size() == 0) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        }finally {
            em.close();
        }

        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

}