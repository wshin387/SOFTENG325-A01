package se325.assignment01.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.dto.PerformerDTO;
import se325.assignment01.concert.service.domain.Performer;
import se325.assignment01.concert.service.mapper.PerformerMapper;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/concert-service")
public class PerformerResource {

    private static Logger LOGGER = LoggerFactory.getLogger(PerformerResource.class);


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
}
