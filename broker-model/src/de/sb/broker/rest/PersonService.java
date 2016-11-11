package de.sb.broker.rest;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Bid;
import de.sb.broker.model.Person;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("people")
public class PersonService {
    static private final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("broker");

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    //Returns the people matching the given criteria, with null or missing parameters identifying omitted criteria.
    public Person[] getPeople(@QueryParam("alias") String alias) {

        //TODO: weitere QueryParams einfügen

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Query q = entityManager.createQuery("select p.identity from Person as p where :alias is null or p.alias = :alias").setParameter("alias", alias);
        List resultList = q.getResultList();
        Person[] matchingPeople = new Person[resultList.size()];
        for (int i = 0; i < resultList.size(); i++) {
            matchingPeople[i] = entityManager.find(Person.class, resultList.get(i));
        }
        return matchingPeople;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}")
    //Returns  the person matching the given identity.
    public Person getPerson(@PathParam("identity") long personIdentity) {
        return entityManagerFactory.createEntityManager().find(Person.class, personIdentity);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}/auctions")
    //Returns all auctions associated with the person matching the given identity (as seller or bidder).
    public Auction[] getAuctions(@PathParam("identity") long personIdentity) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Person person = entityManager.find(Person.class, personIdentity);
        List resultList = entityManager.createQuery("select a.identity from Auction as a").getResultList();
        List<Auction> matchingAuctionsList = new ArrayList<>();
        for (Object auctionID : resultList) {
            Auction auction = entityManager.find(Auction.class, auctionID);
            if (auction.getSellerReference() == (personIdentity) || auction.getBid(person) != null) {
                matchingAuctionsList.add(auction);
            }
        }
        Auction[] matchingAuctions = new Auction[matchingAuctionsList.size()];
        matchingAuctions = matchingAuctionsList.toArray(matchingAuctions);
        return matchingAuctions;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}/bids")
    //Returns all bids for closed auctions associated with the bidder matching the given identity.
    public Bid[] getBids(@PathParam("identity") long personIdentity) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Person person = entityManager.find(Person.class, personIdentity);
        List resultList = entityManager.createQuery("select a.identity from Auction as a").getResultList();
        List<Bid> matchingBidsList = new ArrayList<>();
        for (Object auctionID : resultList) {
            Auction auction = entityManager.find(Auction.class, auctionID);
            if (auction.isClosed() && auction.getBid(person) != null) {
                matchingBidsList.add(auction.getBid(person));
            }
        }
        Bid[] matchingBids = new Bid[matchingBidsList.size()];
        matchingBids = matchingBidsList.toArray(matchingBids);
        return matchingBids;
    }

}
