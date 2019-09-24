package se325.assignment01.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.dto.*;
import se325.assignment01.concert.common.types.BookingStatus;
import se325.assignment01.concert.service.config.Config;
import se325.assignment01.concert.service.domain.*;
import se325.assignment01.concert.service.jaxrs.LocalDateTimeParam;
import se325.assignment01.concert.service.mapper.ConcertMapper;
import se325.assignment01.concert.service.mapper.SeatMapper;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/concert-service")
public class ConcertResource {

    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);

    @GET
    @Path("/concerts/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConcertById(@PathParam("id") long id) {
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
    public Response getAllConcerts() {

        GenericEntity<List<ConcertDTO>> entity;
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            em.getTransaction().begin();

            TypedQuery<Concert> concertQuery = em.createQuery("select c from Concert c", Concert.class);
            List<Concert> concertList = concertQuery.getResultList();
            List<ConcertDTO> concertDTOList = ConcertMapper.listToDTO(concertList);
            entity = new GenericEntity<List<ConcertDTO>>(concertDTOList) {
            };
        } finally {
            em.close();
        }
        return Response.ok(entity).build();
    }

    @GET
    @Path("/concerts/summaries")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllConcertSummaries() {

        GenericEntity<List<ConcertSummaryDTO>> entity;
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            em.getTransaction().begin();

            TypedQuery<Concert> concertQuery = em.createQuery("select c from Concert c", Concert.class);
            List<Concert> concertList = concertQuery.getResultList();
            List<ConcertSummaryDTO> concertSummaryDTOList = concertList.stream().map(c -> new ConcertSummaryDTO(c.getId(), c.getTitle(), c.getImage())).collect(Collectors.toList());
            entity = new GenericEntity<List<ConcertSummaryDTO>>(concertSummaryDTOList) {
            };

        } finally {
            em.close();
        }

        return Response.ok(entity).build();
    }

    @POST
    @Path("/login")
    public Response login(UserDTO userDTO) {
        NewCookie newCookie;
        String username = userDTO.getUsername();
        String password = userDTO.getPassword();
        EntityManager em = PersistenceManager.instance().createEntityManager();
        Response response;

        try {
            LOGGER.info("Attempting Login");
            em.getTransaction().begin();

            TypedQuery<User> userQuery = em.createQuery("select u from User u where u.username = :inputUserName AND u.password = :inputPassword", User.class)
                    .setParameter("inputUserName", username)
                    .setParameter("inputPassword", password)
                    /*.setLockMode(LockModeType.OPTIMISTIC)*/;

            User user = userQuery.getResultList().stream().findFirst().orElse(null);
            if (user == null) {
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

    @GET
    @Path("/seats/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSeats(@PathParam("date") LocalDateTimeParam dateTimeParam, @QueryParam("status") BookingStatus bookingStatus) {

        LocalDateTime date = dateTimeParam.getLocalDateTime();
        GenericEntity<List<SeatDTO>> entity;
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            em.getTransaction().begin();
            TypedQuery<Seat> seatQuery; //query depends on booking status

            if (bookingStatus == BookingStatus.Any) {
                seatQuery = em.createQuery("select s from Seat s Where s.date = :date", Seat.class)
                        .setParameter("date", date);
            } else {

                boolean isBooked = (bookingStatus == BookingStatus.Booked);
                seatQuery = em.createQuery("select s from Seat s Where s.date=:date and s.isBooked = :isBooked", Seat.class)
                        .setParameter("isBooked", isBooked)
                        .setParameter("date", date);
            }

            LOGGER.info("retrieved " + seatQuery.getResultList());

            List<Seat> bookedSeatList = seatQuery.getResultList();
            List<SeatDTO> bookedDTOSeatList = bookedSeatList.stream().map(s -> SeatMapper.toDTO(s)).collect(Collectors.toList());
            entity = new GenericEntity<List<SeatDTO>>(bookedDTOSeatList) {
            };

        } finally {
            em.close();
        }
        return Response.ok(entity).build();
    }
}
