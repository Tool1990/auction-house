package de.sb.broker.rest;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Bid;
import de.sb.broker.model.Document;
import de.sb.broker.model.Person;

import javax.persistence.*;
import javax.print.Doc;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Path("people")
public class PersonService {
    static private final String SQL = "select p.identity from Person as p where  " +
            "(:alias is null or p.alias = :alias ) " +
            "and (:familyName is null or p.name.family = :familyName) and " +
            "(:givenName is null or p.name.given = :givenName and " +
            "(:group is null or p.group = :group) and " +
            "(:city is null or p.address.city = :city)" +
            ")";
    private static final String SQLAVATAR = "select d.identity from Document as d where d.hash = :docHash";

    /**
     *
     * @return EntityManager provided from LifeCycleProvider
     */
    public EntityManager getEM(){
        return LifeCycleProvider.brokerManager();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    //Returns the people matching the given criteria, with null or missing parameters identifying omitted criteria.
    public Person[] getPeople(@QueryParam("alias") String alias, @QueryParam("familyName") String familyName, @QueryParam("givenName") String givenName, @QueryParam("group") Person.Group group, @QueryParam("city") String city) {


        Person[] matchingPeople = null;

        try {

            TypedQuery<Long> q = getEM().createQuery(SQL, Long.class);
            q.setParameter("familyName", familyName);
            q.setParameter("givenName", givenName);
            q.setParameter("alias", alias);
            q.setParameter("group", group);
            q.setParameter("city", city);
            List<Long> resultList = q.getResultList();
            matchingPeople = new Person[resultList.size()];
            for (int i = 0; i < resultList.size(); i++) {
                matchingPeople[i] = getEM().find(Person.class, resultList.get(i));
            }
        } catch (Exception e) {
        } finally {
            if (!getEM().getTransaction().isActive())
                getEM().getTransaction().begin();
        }


        return matchingPeople;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}")
    //Returns  the person matching the given identity.
    public Person getPerson(@PathParam("identity") long personIdentity) {
        Person person = null;
        try {
            person = getEM().find(Person.class, personIdentity);
        } catch (Exception e) {
        } finally {
            if (!getEM().getTransaction().isActive())
                getEM().getTransaction().begin();
        }
        System.out.println(person);
        return person;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}/auctions")
    //Returns all auctions associated with the person matching the given identity (as seller or bidder).
    public Auction[] getAuctions(@PathParam("identity") long personIdentity) {
        List<Auction> resultList = null;
        try {
            Person person = getEM().find(Person.class, personIdentity);
            resultList = new ArrayList(person.getAuctions());

            List<Bid> bidsList = new ArrayList(person.getBids());

            for (Bid bid : bidsList) {
                if (bid.getBidderReference() == personIdentity) {
                    resultList.add(bid.getAuction());
                }
            }
        } catch (Exception e) {
        } finally {
            if (!getEM().getTransaction().isActive())
                getEM().getTransaction().begin();
        }
        Auction[] results = new Auction[resultList.size()];

        return resultList.toArray(results);
    }

    @GET
    @Produces({MediaType.WILDCARD})
    @Path("{identity}/avatar")
    public Response getAvatar(@PathParam("identity") long personIdentity) {
        Response res = null;
        try {
            Person person = getEM().find(Person.class, personIdentity);
            Document avatar = person.getAvatar();


            //respone with no content if avatar null else response with avatar content
            res = avatar == null ? Response.noContent().build() : Response.ok().entity(avatar.getContent()).header("mimetype", avatar.getType()).build();
        } catch (Exception e) {
        } finally {
            if (!getEM().getTransaction().isActive())
                getEM().getTransaction().begin();
        }

        return res;
    }

    @PUT
    @Consumes({MediaType.WILDCARD})
    @Path("{identity}/avatar")
    public void setAvatar(byte[] documentContent, @HeaderParam("Accept") String contentType, @PathParam("identity") long personIdentity) {
        Document doc = null;

        try {
            Person person = getEM().find(Person.class, personIdentity);

            byte[] docHash = Document.documentHash(documentContent);
            TypedQuery query = getEM().createQuery(SQLAVATAR, Document.class);
            query.setParameter("docHash", docHash);
            List<Long> result = query.getResultList();
            if (result.size() != 0) {
                getEM().getTransaction().begin();
                doc = getEM().find(Document.class, result.get(0));
                person.setAvatar(doc);
            } else {
                doc = new Document(contentType, documentContent);
                getEM().getTransaction().begin();
                getEM().persist(doc);
                getEM().getTransaction().commit();

                getEM().getTransaction().begin();
                person.setAvatar(doc);
            }
            getEM().merge(person);
            getEM().getTransaction().commit();
            getEM().getTransaction().begin();


        } catch (Exception e) {
        } finally {
            Cache cache = getEM().getEntityManagerFactory().getCache();
            cache.evict(doc.getClass(), doc.getIdentity());

            if (!getEM().getTransaction().isActive())
                getEM().getTransaction().begin();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}/bids")
    //Returns all bids for closed auctions associated with the bidder matching the given identity.
    public Bid[] getBids(@PathParam("identity") long personIdentity) {
        Bid[] matchingBids = null;
        try {

            Person person = getEM().find(Person.class, personIdentity);
            Collection<Bid> resultList = person.getBids();
            List<Bid> matchingBidsList = new ArrayList<>();

            for (Bid bid : resultList) {
                Auction auction = bid.getAuction();
                if (auction.isClosed()) {
                    matchingBidsList.add(bid);
                }
            }
            matchingBids = matchingBidsList.toArray(new Bid[matchingBidsList.size()]);
        } catch (Exception e) {
        } finally {
            if (!getEM().getTransaction().isActive())
                getEM().getTransaction().begin();
        }
        return matchingBids;
    }


    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.WILDCARD})
    //Creates or modifies an auction from the given template data. Note that an auction may only be modified as long as it is not sealed (i.e. is open and still without bids).
    public long setPerson(Person personTemplate, @HeaderParam("set-password") String newPassword) {
        Person person = getEM().find(Person.class, personTemplate.getIdentity());
        long identity = 0;
        if (person == null) {
            try {
                getEM().getTransaction().begin();
                getEM().persist(personTemplate);
                getEM().getTransaction().commit();
                identity = personTemplate.getIdentity();
            } catch (Exception e) {
            } finally {
                if (!getEM().getTransaction().isActive())
                    getEM().getTransaction().begin();
            }
        } else {
            try {
                getEM().getTransaction().begin();
                person.setAlias(personTemplate.getAlias());
                person.setGroup(personTemplate.getGroup());
                person.getAddress().setCity(personTemplate.getAddress().getCity());
                person.getAddress().setPostCode(personTemplate.getAddress().getPostCode());
                person.getAddress().setStreet(personTemplate.getAddress().getStreet());
                person.getName().setFamily(personTemplate.getName().getFamily());
                person.getName().setGiven(personTemplate.getName().getGiven());
                if (newPassword != "" && newPassword != null) {
                    person.setPasswordHash(Person.passwordHash(newPassword));
                }
                identity = person.getIdentity();
                getEM().flush();
            } catch (Exception e) {
            } finally {
                if (!getEM().getTransaction().isActive())
                    getEM().getTransaction().begin();
            }
        }
        return identity;
    }
}

