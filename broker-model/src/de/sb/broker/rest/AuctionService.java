package de.sb.broker.rest;

import de.sb.broker.model.Auction;

import javax.persistence.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Path("auctions")
public class AuctionService {

    static private final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("broker");
    static private final String SQL_AUCTIONS = "select a.identity from Auction as a where " +
            "(:title is null or a.title = :title) and " +
            "(:description is null or a.description = :description) and " +
            "(:priceMin is null or a.askingPrice >= :priceMin) and " +
            "(:priceMax is null or a.askingPrice <= :priceMax) and " +
            "(:unitCountMin is null or a.unitCount >= :unitCountMin) and " +
            "(:unitCountMax is null or a.unitCount <= :unitCountMax) and " +
            "(:creationMin is null or a.creationTimestamp >= :creationMin) and " +
            "(:creationMax is null or a.creationTimestamp <= :creationMax) and " +
            "(:closureMin is null or a.closureTimestamp >= :closureMin) and " +
            "(:closureMax is null or a.closureTimestamp <= :closureMax)";
    private EntityManager entityManager;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    //Returns the auctions matching the given criteria, with null or missing parameters identifying omitted criteria.
    public Auction[] getAuctions(
            @QueryParam("title") String title,
            @QueryParam("description") String description,
            @QueryParam("priceMin") Long priceMin,
            @QueryParam("priceMax") Long priceMax,
            @QueryParam("unitCountMin") Long unitCountMin,
            @QueryParam("unitCountMax") Long unitCountMax,
            @QueryParam("creationMin") Long creationMin,
            @QueryParam("creationMax") Long creationMax,
            @QueryParam("closureMin") Long closureMin,
            @QueryParam("closureMax") Long closureMax
    ) {
        entityManager = entityManagerFactory.createEntityManager();

        try {
            Query q = entityManager.createQuery(SQL_AUCTIONS);
            q.setParameter("title", title);
            q.setParameter("description", description);
            q.setParameter("priceMin", priceMin);
            q.setParameter("priceMax", priceMax);
            q.setParameter("unitCountMin", unitCountMin);
            q.setParameter("unitCountMax", unitCountMax);
            q.setParameter("creationMin", creationMin);
            q.setParameter("creationMax", creationMax);
            q.setParameter("closureMin", closureMin);
            q.setParameter("closureMax", closureMax);

            List resultList = q.getResultList();
            Auction[] matchingAuctions = new Auction[resultList.size()];
            for (int i = 0; i < resultList.size(); i++) {
                matchingAuctions[i] = entityManager.find(Auction.class, resultList.get(i));
            }
            Arrays.sort(matchingAuctions, Comparator.comparing(Auction::getTitle));

            return matchingAuctions;
        } finally {
            entityManager.close();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}")
    //Returns the auction matching the given identity
    public Auction getAuction(@PathParam("identity") long auctionIdentity) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.find(Auction.class, auctionIdentity);
        } finally {
            em.close();
        }
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    //Creates or modifies an auction from the given template data. Note that an auction may only be modified as long as it is not sealed (i.e. is open and still without bids).
    public void setAuction(Auction auctionTemplate) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        try {
            entityManager.getTransaction().begin();
            Auction auction = entityManager.find(Auction.class, auctionTemplate.getIdentity());
            if (auction == null) {
                entityManager.persist(auctionTemplate);
                entityManager.getTransaction().commit();
            } else if (!auction.isSealed()) {
                auction.setTitle(auctionTemplate.getTitle());
                auction.setAskingPrice(auctionTemplate.getAskingPrice());
                auction.setClosureTimestamp(auctionTemplate.getClosureTimestamp());
                auction.setDescription(auctionTemplate.getDescription());
                auction.setUnitCount(auctionTemplate.getUnitCount());
                entityManager.flush();
            }
        } finally {
            Cache cache = entityManager.getEntityManagerFactory().getCache();
            cache.evict(auctionTemplate.getSeller().getClass(), auctionTemplate.getSeller().getIdentity());
            entityManager.close();
        }
    }
}


