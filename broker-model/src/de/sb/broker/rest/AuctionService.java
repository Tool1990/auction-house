package de.sb.broker.rest;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Person;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Path("auctions")
public class AuctionService {

    private EntityManager getEM() {
        return LifeCycleProvider.brokerManager();
    }

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

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    //Returns the auctions matching the given criteria, with null or missing parameters identifying omitted criteria.
    public Auction[] getAuctions(
            @Min(0) @QueryParam("firstResult") int firstResult,
            @Min(0) @QueryParam("maxResults") int maxResults,
            @Size(min = 1, max = 255) @QueryParam("title") String title,
            @Size(min = 1, max = 8189) @QueryParam("description") String description,
            @Min(1) @QueryParam("priceMin") Long priceMin,
            @Min(1) @QueryParam("priceMax") Long priceMax,
            @Min(1) @QueryParam("unitCountMin") Long unitCountMin,
            @Min(1) @QueryParam("unitCountMax") Long unitCountMax,
            @Min(1) @QueryParam("creationMin") Long creationMin,
            @Min(1) @QueryParam("creationMax") Long creationMax,
            @Min(1) @QueryParam("closureMin") Long closureMin,
            @Min(1) @QueryParam("closureMax") Long closureMax,
            @HeaderParam("Authorization") String authString
    ) {
        try {
            LifeCycleProvider.authenticate(authString);
            Query q = getEM().createQuery(SQL_AUCTIONS);
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

            if (firstResult > 0)
                q.setFirstResult(firstResult);
            if (maxResults > 0)
                q.setMaxResults(maxResults);

            List resultList = q.getResultList();
            Auction[] matchingAuctions = new Auction[resultList.size()];
            for (int i = 0; i < resultList.size(); i++) {
                matchingAuctions[i] = getEM().find(Auction.class, resultList.get(i));
            }
            Arrays.sort(matchingAuctions, Comparator.comparing(Auction::getTitle));

            return matchingAuctions;
        } catch (IllegalArgumentException exception) {
            throw new ClientErrorException(400);
        } catch (NotAuthorizedException exception) {
            throw new ClientErrorException(401);
        } finally {
            if (!getEM().getTransaction().isActive())
                getEM().getTransaction().begin();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}")
    //Returns the auction matching the given identity
    public Auction getAuction(@PathParam("identity") long auctionIdentity, @HeaderParam("Authorization") String authString) {
        try {
            LifeCycleProvider.authenticate(authString);
            Auction auction = getEM().find(Auction.class, auctionIdentity);
            if (auction == null) {
                throw new ClientErrorException(404);
            } else {
                return auction;
            }
        } catch (IllegalArgumentException exception) {
            throw new ClientErrorException(400);
        } catch (NotAuthorizedException exception) {
            throw new ClientErrorException(401);
        } finally {
            if (!getEM().getTransaction().isActive())
                getEM().getTransaction().begin();
        }
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    //Creates or modifies an auction from the given template data. Note that an auction may only be modified as long as it is not sealed (i.e. is open and still without bids).
    public void setAuction(@Valid @NotNull Auction auctionTemplate, @HeaderParam("Authorization") String authString) {
        try {
            Person requester = LifeCycleProvider.authenticate(authString);
            if (requester.getIdentity() != auctionTemplate.getSellerReference() && !requester.getGroup().equals(Person.Group.ADMIN)) {
                throw new ClientErrorException(403);
            }
            Auction auction = getEM().find(Auction.class, auctionTemplate.getIdentity());
            if (auction == null) {
                getEM().persist(auctionTemplate);
                getEM().getTransaction().commit();
                getEM().getTransaction().begin();
            } else if (!auction.isSealed()) {
                auction.setTitle(auctionTemplate.getTitle());
                auction.setAskingPrice(auctionTemplate.getAskingPrice());
                auction.setClosureTimestamp(auctionTemplate.getClosureTimestamp());
                auction.setDescription(auctionTemplate.getDescription());
                auction.setUnitCount(auctionTemplate.getUnitCount());
                getEM().flush();
            } else {
                throw new ClientErrorException(403);
            }
        } catch (IllegalArgumentException exception) {
            throw new ClientErrorException(400);
        } catch (NotAuthorizedException exception) {
            throw new ClientErrorException(401);
        } catch (RollbackException exception) {
            throw new ClientErrorException(409);
        } finally {
            Cache cache = getEM().getEntityManagerFactory().getCache();
            cache.evict(auctionTemplate.getSeller().getClass(), auctionTemplate.getSeller().getIdentity());
            if (!getEM().getTransaction().isActive())
                getEM().getTransaction().begin();
        }
    }
}