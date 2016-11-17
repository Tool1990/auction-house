package de.sb.broker.rest;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Bid;
import de.sb.broker.model.Document;
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
    public Person[] getPeople(@QueryParam("alias") String alias, @QueryParam("familyName") String familyName, @QueryParam("givenName") String givenName, @QueryParam("group") Person.Group group, @QueryParam("city") String city) {

        //TODO: weitere QueryParams einf�gen

        EntityManager entityManager = entityManagerFactory.createEntityManager();


        Query q = entityManager.createQuery("select p.identity from Person as p where  " +
                "(:alias is null or p.alias = :alias ) " +
                "and (:familyName is null or p.name.family = :familyName) and " +
                "(:givenName is null or p.name.given = :givenName and " +
                "(:group is null or p.group = :group) and " +
                "(:city is null or p.address.city = :city)" +
                ")").setParameter("alias", alias).setParameter("familyName", familyName).setParameter("givenName", givenName).setParameter("group", group).setParameter("city", city);

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
    @Path("{identity}/avatar")
    public Document getAvatar(@PathParam("identity") long personIdentity) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Person person = entityManager.find(Person.class, personIdentity);
        return person.getAvatar();
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}/avatar")
    public void setAvatar(Document documentTemplate, @PathParam("identity") long personIdentity){
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Person person = entityManager.find(Person.class, personIdentity);
        if (person.getAvatar() == null) {
            try {
                entityManager.getTransaction().begin();
                Document doc = entityManager.find(Document.class, documentTemplate.getIdentity());
                if(doc==null){
                    documentTemplate.setHash(Document.documentHash(documentTemplate.getContent()));
                    entityManager.persist(documentTemplate);
                    entityManager.getTransaction().commit();

                    entityManager.getTransaction().begin();
                    doc = entityManager.find(Document.class, documentTemplate.getIdentity());
                    person.setAvatar(doc);
                }else{
                    person.setAvatar(doc);
                }
                entityManager.flush();

            } catch (Exception e) {
            } finally {
                entityManager.close();
            }
        } else {
            try {
                entityManager.getTransaction().begin();
                person.getAvatar().setContent(documentTemplate.getContent());
                person.getAvatar().setHash(Document.documentHash(documentTemplate.getContent()));
                person.getAvatar().setType(documentTemplate.getType());
                entityManager.flush();
            } catch (Exception e) {
            } finally {
                entityManager.close();
            }
        }
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


    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("people")
    //Creates or modifies an auction from the given template data. Note that an auction may only be modified as long as it is not sealed (i.e. is open and still without bids).
    public long setPerson(Person personTemplate, @HeaderParam("set-password") String newPassword) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Person person = entityManager.find(Person.class, personTemplate.getIdentity());
        long identity = 0;
        if (person == null) {
            try {
                entityManager.getTransaction().begin();
                entityManager.persist(personTemplate);
                entityManager.getTransaction().commit();
                identity = person.getIdentity();
            } catch (Exception e) {
            } finally {
                entityManager.close();
            }
        } else {
            try {
                entityManager.getTransaction().begin();
                person.setAlias(personTemplate.getAlias());
                person.setGroup(personTemplate.getGroup());
                person.getAddress().setCity(personTemplate.getAddress().getCity());
                person.getAddress().setPostCode(personTemplate.getAddress().getPostCode());
                person.getAddress().setStreet(personTemplate.getAddress().getStreet());
                person.getName().setFamily(personTemplate.getName().getFamily());
                person.getName().setGiven(personTemplate.getName().getGiven());
                if (newPassword != "" && newPassword != null) {
                    person.setPasswordHash(personTemplate.getPasswordHash());
                }
                identity = person.getIdentity();
                entityManager.flush();
            } catch (Exception e) {
            } finally {
                entityManager.close();
            }
        }
        return identity;
    }
}

