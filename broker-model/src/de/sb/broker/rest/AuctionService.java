package de.sb.broker.rest;

import de.sb.broker.model.Auction;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("auctions")
public class AuctionService {

    static private final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("broker");

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    //Returns the auctions matching the given criteria, with null or missing parameters identifying omitted criteria.
    public Auction[] getAuctions(@QueryParam("title") String title) {

        //TODO: weitere QueryParams einfügen

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Query q = entityManager.createQuery("select a.identity from Auction as a where :title is null or a.title = :title").setParameter("title", title);
        List resultList = q.getResultList();
        Auction[] matchingAuctions = new Auction[resultList.size()];
        for (int i = 0; i < resultList.size(); i++) {
            matchingAuctions[i] = entityManager.find(Auction.class, (long) resultList.get(i));
        }
        return matchingAuctions;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}")
    //Returns the auction matching the given identity
    public Auction getAuction(@PathParam("identity") long auctionIdentity) {
        return entityManagerFactory.createEntityManager().find(Auction.class, auctionIdentity);
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("auctions")
    //Creates or modifies an auction from the given template data. Note that an auction may only be modified as long as it is not sealed (i.e. is open and still without bids).
    public void setAuction(Auction auctionTemplate) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Auction auction = entityManager.find(Auction.class, auctionTemplate.getIdentity());
        if (auction == null) {
            try {
                entityManager.getTransaction().begin();
                entityManager.persist(auctionTemplate);
                entityManager.getTransaction().commit();
            } catch (Exception e) {
            } finally {
                entityManager.close();
            }
        } else if (!auction.isSealed()) {
            try {
                entityManager.getTransaction().begin();
                auction.setTitle(auctionTemplate.getTitle());
                auction.setAskingPrice(auctionTemplate.getAskingPrice());
                auction.setClosureTimestamp(auctionTemplate.getClosureTimestamp());
                auction.setDescription(auctionTemplate.getDescription());
                auction.setUnitCount(auctionTemplate.getUnitCount());
                entityManager.flush();
            } catch (Exception e) {
            } finally {
                entityManager.close();
            }
        }
    }
}
