package de.sb.broker.rest;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Bid;
import de.sb.broker.model.Person;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.util.*;

@Path("auctions")
public class AuctionService {
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

    private EntityManager getEM() {
        return LifeCycleProvider.brokerManager();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Auction.XmlSellerAsEntityFilter
    //Returns the auctions matching the given criteria, with null or missing parameters identifying omitted criteria.
    public Response getAuctions(
            @QueryParam("offset") @Min(0) int offset,
            @QueryParam("limit") @Min(0) int limit,
            @QueryParam("title") @Size(min = 1, max = 255) String title,
            @QueryParam("description") @Size(min = 1, max = 8189) String description,
            @QueryParam("minimum-price") @Min(1) Long priceMin,
            @QueryParam("maximum-price") @Min(1) Long priceMax,
            @QueryParam("minimum-count") @Min(1) Long unitCountMin,
            @QueryParam("maximum-count") @Min(1) Long unitCountMax,
            @QueryParam("minimum-creation") @Min(1) Long creationMin,
            @QueryParam("maximum-creation") @Min(1) Long creationMax,
            @QueryParam("minimum-closure") @Min(1) Long closureMin,
            @QueryParam("maximum-closure") @Min(1) Long closureMax,
            @QueryParam("closed") Boolean closed,
            @HeaderParam("Authorization") String authString
    ) {
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

        if (offset > 0)
            q.setFirstResult(offset);
        if (limit > 0)
            q.setMaxResults(limit);

        List<Long> auctionIdentities = q.getResultList();
        List<Auction> auctions = new ArrayList<>();

        for (Long identity : auctionIdentities) {
            auctions.add(getEM().find(Auction.class, identity));
        }

        List<Annotation> filterAnnotations = new ArrayList<>();

        if (closed == Boolean.TRUE) {
            for (Iterator<Auction> iterator = auctions.iterator(); iterator.hasNext(); ) {
                if (!iterator.next().isClosed()) {
                    iterator.remove();
                }
            }

            filterAnnotations.add(new Auction.XmlBidsAsEntityFilter.Literal());
            filterAnnotations.add(new Bid.XmlBidderAsEntityFilter.Literal());
            filterAnnotations.add(new Bid.XmlAuctionAsReferenceFilter.Literal());
        } else if (closed == Boolean.FALSE) {
            for (Iterator<Auction> iterator = auctions.iterator(); iterator.hasNext(); ) {
                if (iterator.next().isClosed()) {
                    iterator.remove();
                }
            }
        }

        Collections.sort(auctions, Comparator.comparing(Auction::getClosureTimestamp)
                .thenComparing(Auction::getCreationTimestamp)
                .thenComparing(Auction::getIdentity));
        GenericEntity<?> wrapper = new GenericEntity<Collection<Auction>>(auctions) {
        };

        return Response.ok().entity(wrapper, filterAnnotations.toArray(new Annotation[0])).build();
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    //Creates or modifies an auction from the given template data. Note that an auction may only be modified as long as it is not sealed (i.e. is open and still without bids).
    public long setAuction(@Valid @NotNull Auction auctionTemplate,
                           @HeaderParam("Authorization") String authString
    ) {
        try {
            Person requester = LifeCycleProvider.authenticate(authString);
            Auction auction = getEM().find(Auction.class, auctionTemplate.getIdentity());
            if (auction == null) {
                auction = new Auction(requester);
                auction = transferAuctionAttributes(auction, auctionTemplate);
                getEM().persist(auction);
                try {
                    getEM().getTransaction().commit();
                } finally {
                    getEM().getTransaction().begin();
                }
                Cache cache = getEM().getEntityManagerFactory().getCache();
                cache.evict(Person.class, auction.getSeller().getIdentity());
            } else if (!auction.isSealed()) {
                if (requester.getIdentity() != auction.getSellerReference() && !requester.getGroup().equals(Person.Group.ADMIN)) {
                    throw new ClientErrorException(403);
                }
                auction = transferAuctionAttributes(auction, auctionTemplate);
                getEM().flush();
                try {
                    getEM().getTransaction().commit();
                } finally {
                    getEM().getTransaction().begin();
                }
            } else {
                throw new ClientErrorException(403);
            }

            return auction.getIdentity();
        } catch (RollbackException exception) {
            throw new ClientErrorException(409);
        } catch (ConstraintViolationException e) {
            throw new ClientErrorException(400);
        }
    }

    private Auction transferAuctionAttributes(Auction auction, Auction auctionTemplate) {
        auction.setTitle(auctionTemplate.getTitle());
        auction.setAskingPrice(auctionTemplate.getAskingPrice());
        auction.setClosureTimestamp(auctionTemplate.getClosureTimestamp());
        auction.setDescription(auctionTemplate.getDescription());
        auction.setUnitCount(auctionTemplate.getUnitCount());
        return auction;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}")
    @Auction.XmlSellerAsReferenceFilter
    //Returns the auction matching the given identity
    public Auction getAuction(
            @PathParam("identity") long auctionIdentity,
            @HeaderParam("Authorization") String authString
    ) {
        LifeCycleProvider.authenticate(authString);
        Auction auction = getEM().find(Auction.class, auctionIdentity);
        if (auction == null) {
            throw new ClientErrorException(404);
        }

        return auction;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}/bid")
    @Bid.XmlBidderAsReferenceFilter
    @Bid.XmlAuctionAsReferenceFilter
    //Returns the requesters bid for the auction matching the given identity
    public Bid getBid(@PathParam("identity") long auctionIdentity,
                      @HeaderParam("Authorization") String authString
    ) {
        Person requester = LifeCycleProvider.authenticate(authString);
        Auction auction = getEM().find(Auction.class, auctionIdentity);

        if (auction == null) {
            throw new ClientErrorException(404);
        }

        return auction.getBid(requester);
    }

    @POST
    @Consumes({MediaType.TEXT_PLAIN})
    @Produces({MediaType.TEXT_PLAIN})
    @Path("{identity}/bid")
    //Creates or modifies the requesters bid for the given auction
    public long setBid(
            @HeaderParam("Authorization") String authString,
            @PathParam("identity") long auctionIdentity,
            @Min(0) long price) {
        try {
            Person requester = LifeCycleProvider.authenticate(authString);

            Auction auction = getEM().find(Auction.class, auctionIdentity);

            if (auction == null)
                throw new ClientErrorException(404);

            if (requester.getIdentity() == auction.getSellerReference())
                throw new ClientErrorException(403);

            Bid bid = auction.getBid(requester);
            if (bid == null) { //create new bid
                bid = new Bid(auction, requester);
                bid.setPrice(price);
                getEM().persist(bid);
            } else if (price == 0) { //remove requesters bid
                getEM().remove(bid);
            } else { //update requesters bid
                bid.setPrice(price);
            }
            try {
                getEM().getTransaction().commit();
            } finally {
                getEM().getTransaction().begin();
            }

            Cache cache = getEM().getEntityManagerFactory().getCache();
            cache.evict(Auction.class, auction.getIdentity());
            cache.evict(Person.class, requester.getIdentity());

            return bid.getIdentity();
        } catch (RollbackException exception) {
            throw new ClientErrorException(409);
        } catch (ConstraintViolationException ex) {
            throw new ClientErrorException(400);
        }
    }
}