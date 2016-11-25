package de.sb.broker.rest;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Bid;
import de.sb.broker.model.Document;
import de.sb.broker.model.Person;

import javax.persistence.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("people")
public class PersonService {
    static private final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("broker");
    static private final String SQLPEOPLE = "select p.identity from Person as p where  " +
            "(:alias is null or p.alias = :alias ) " +
            "and (:familyName is null or p.name.family = :familyName) and " +
            "(:givenName is null or p.name.given = :givenName and " +
            "(:group is null or p.group = :group) and " +
            "(:city is null or p.address.city = :city)" +
            ")";
    private static final String SQLAVATAR = "select d.identity from Document as d where d.hash = :docHash";

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    //Returns the people matching the given criteria, with null or missing parameters identifying omitted criteria.
    public Person[] getPeople(
            int firstResult,
            int maxResults,
            @QueryParam("alias") String alias,
            @QueryParam("familyName") String familyName,
            @QueryParam("givenName") String givenName,
            @QueryParam("group") Person.Group group,
            @QueryParam("city") String city
    ) {
        //TODO: alle query parameter, sortierung, exceptions nicht fangen wenn leerer catchblock, sämtlicher Funktionscore in try block packen
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Person[] matchingPeople = null;

        try {

            TypedQuery<Long> q = entityManager.createQuery(SQLPEOPLE, Long.class);
            q.setParameter("familyName", familyName);
            q.setParameter("givenName", givenName);
            q.setParameter("alias", alias);
            q.setParameter("group", group);
            q.setParameter("city", city);
            if (firstResult > 0)
                q.setFirstResult(firstResult);
            if (maxResults > 0)
                q.setMaxResults(maxResults);
            List<Long> resultList = q.getResultList();
            matchingPeople = new Person[resultList.size()];
            for (int i = 0; i < resultList.size(); i++) {
                matchingPeople[i] = entityManager.find(Person.class, resultList.get(i));
            }
        } finally {
            entityManager.close();
        }

        Arrays.sort(matchingPeople, Comparator.comparing(Person::getAlias)); //(Person person) -> person.getAlias()
        return matchingPeople;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}")
    //Returns  the person matching the given identity.
    public Person getPerson(@PathParam("identity") long personIdentity) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            Person person = em.find(Person.class, personIdentity);
            return person;
        } finally {
            em.close();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}/auctions")
    //Returns all auctions associated with the person matching the given identity (as seller or bidder).
    public Auction[] getAuctions(@PathParam("identity") long personIdentity) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        List<Auction> resultList = null;
        try {
            Person person = entityManager.find(Person.class, personIdentity);
            resultList = new ArrayList(person.getAuctions());

            List<Bid> bidsList = new ArrayList(person.getBids());

            for (Bid bid : bidsList) {
                if (bid.getBidderReference() == personIdentity) {
                    resultList.add(bid.getAuction());
                }
            }
        } finally {
            entityManager.close();
        }

        return resultList.toArray(new Auction[0]);
    }

    @GET
    @Produces({MediaType.WILDCARD})
    @Path("{identity}/avatar")
    public Response getAvatar(@PathParam("identity") long personIdentity) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Response res = null;
        try {
            Person person = entityManager.find(Person.class, personIdentity);
            Document avatar = person.getAvatar();


            //respone with no content if avatar null else response with avatar content
            res = avatar == null ? Response.noContent().build() : Response.ok(avatar.getContent(), avatar.getType()).build();
        }  finally {
            entityManager.close();
        }

        return res;
    }

    @PUT
    @Consumes({MediaType.WILDCARD})
    @Path("{identity}/avatar")
    public void setAvatar(byte[] documentContent, @HeaderParam("Content-type") String contentType, @PathParam("identity") long personIdentity) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        Document doc = null;

        try {
            Person person = entityManager.find(Person.class, personIdentity);
            //TODO: byte[] statt int
            int docHash = Document.documentHash(documentContent);
            TypedQuery query = entityManager.createQuery(SQLAVATAR, Document.class);
            query.setParameter("docHash", docHash);
            List<Long> result = query.getResultList();
            if (result.size() == 1) {
                doc = entityManager.find(Document.class, result.get(0));
                person.setAvatar(doc);
                entityManager.flush();
            } else {
                doc = new Document(contentType, documentContent);
                entityManager.persist(doc);
            }
            doc.setType(contentType);
            entityManager.getTransaction().commit();

            entityManager.getTransaction().begin();
            person.setAvatar(doc);
            entityManager.getTransaction().commit();

        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            Cache cache = entityManager.getEntityManagerFactory().getCache();
            cache.evict(doc.getClass(), doc.getIdentity());
            entityManager.close();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}/bids")
    //Returns all bids for closed auctions associated with the bidder matching the given identity.
    public Bid[] getBids(@PathParam("identity") long personIdentity) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Bid[] matchingBids = null;
        try {

            Person person = entityManager.find(Person.class, personIdentity);
            Collection<Bid> resultList = person.getBids();
            List<Bid> matchingBidsList = new ArrayList<>();

            for (Bid bid : resultList) {
                Auction auction = bid.getAuction();
                if (auction.isClosed()) {
                    matchingBidsList.add(bid);
                }
            }
            matchingBids = matchingBidsList.toArray(new Bid[matchingBidsList.size()]);
        } finally {
            entityManager.close();
        }
        return matchingBids;
    }


    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.WILDCARD})
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
                identity = personTemplate.getIdentity();
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
                    person.setPasswordHash(Person.passwordHash(newPassword));
                }
                identity = person.getIdentity();
                entityManager.flush();
            } finally {
                entityManager.close();
            }
        }
        return identity;
    }
}

