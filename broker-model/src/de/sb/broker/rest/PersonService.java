package de.sb.broker.rest;

import de.sb.broker.model.*;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("people")
public class PersonService {
    static private final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("broker");
    static private final String SQL_PEOPLE = "select p.identity from Person as p where  " +
            "(:alias is null or p.alias = :alias) and " +
            "(:familyName is null or p.name.family = :familyName) and " +
            "(:givenName is null or p.name.given = :givenName) and " +
            "(:group is null or p.group = :group) and " +
            "(:city is null or p.address.city = :city) and " +
            "(:postCode is null or p.address.postCode = :postCode) and " +
            "(:street is null or p.address.street = :street) and " +
            "(:email is null or p.contact.email = :email) and " +
            "(:phone is null or p.contact.phone = :phone) and " +
            "(:creationMin is null or p.creationTimestamp >= :creationMin) and " +
            "(:creationMax is null or p.creationTimestamp <= :creationMax)";
    private static final String SQL_AVATAR = "select d.identity from Document as d where d.hash = :docHash";

    private EntityManager getEM() {
        return LifeCycleProvider.brokerManager();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    //Returns the people matching the given criteria, with null or missing parameters identifying omitted criteria.
    public Person[] getPeople(
            @Min(0) @QueryParam("firstResult") int firstResult,
            @Min(0) @QueryParam("maxResults") int maxResults,
            @QueryParam("alias") String alias,
            @Size(min = 1, max = 31) @QueryParam("familyName") String familyName,
            @Size(min = 1, max = 31) @QueryParam("givenName") String givenName,
            @QueryParam("group") Person.Group group,
            @Size(min = 1, max = 63) @QueryParam("city") String city,
            @Size(min = 1, max = 15) @QueryParam("postCode") String postCode,
            @Size(min = 1, max = 63) @QueryParam("street") String street,
            @Size(min = 1, max = 63) @Pattern(regexp = Contact.EMAIL_PATTERN) @QueryParam("email") String email,
            @Size(min = 1, max = 63) @QueryParam("phone") String phone,
            @Min(1) @QueryParam("creationMin") Long creationMin,
            @Min(1) @QueryParam("creationMax") Long creationMax,
            @HeaderParam("Authorization") String authString
    ) {
        try {
            LifeCycleProvider.authenticate(authString);
            TypedQuery<Long> q = getEM().createQuery(SQL_PEOPLE, Long.class);
            q.setParameter("familyName", familyName);
            q.setParameter("givenName", givenName);
            q.setParameter("alias", alias);
            q.setParameter("group", group);
            q.setParameter("city", city);
            q.setParameter("postCode", postCode);
            q.setParameter("street", street);
            q.setParameter("email", email);
            q.setParameter("phone", phone);
            q.setParameter("creationMin", creationMin);
            q.setParameter("creationMax", creationMax);

            if (firstResult > 0)
                q.setFirstResult(firstResult);
            if (maxResults > 0)
                q.setMaxResults(maxResults);

            List<Long> resultList = q.getResultList();
            Person[] matchingPeople = new Person[resultList.size()];

            for (int i = 0; i < resultList.size(); i++) {
                matchingPeople[i] = getEM().find(Person.class, resultList.get(i));
            }
            Arrays.sort(matchingPeople, Comparator.comparing(Person::getAlias)); //(Person person) -> person.getAlias()

            return matchingPeople;
        } catch (IllegalArgumentException exception) {
            throw new ClientErrorException(400);
        } catch (NotAuthorizedException exception) {
            throw new ClientErrorException(401);
        } catch (RollbackException exception) {
            throw new ClientErrorException(409);
        } finally {
            if (!getEM().getTransaction().isActive())
                getEM().getTransaction().begin();
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}")
    //Returns  the person matching the given identity.
    public Person getPerson(@PathParam("identity") long personIdentity, @HeaderParam("Authorization") String authString) {
        try {
            LifeCycleProvider.authenticate(authString);
            Person person = getEM().find(Person.class, personIdentity);
            if (person == null) {
                throw new ClientErrorException(404);
            } else {
                return person;
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

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}/auctions")
    //Returns all auctions associated with the person matching the given identity (as seller or bidder).
    public Auction[] getAuctions(@PathParam("identity") long personIdentity, @HeaderParam("Authorization") String authString) {
        try {
            LifeCycleProvider.authenticate(authString);
            Person person = getEM().find(Person.class, personIdentity);
            if (person == null) {
                throw new ClientErrorException(404);
            }
            List<Auction> resultList = new ArrayList(person.getAuctions());
            List<Bid> bidsList = new ArrayList(person.getBids());

            for (Bid bid : bidsList) {
                if (bid.getBidderReference() == personIdentity) {
                    resultList.add(bid.getAuction());
                }
            }
            Auction[] auctions = resultList.toArray(new Auction[0]);
            Arrays.sort(auctions, Comparator.comparing(Auction::getTitle));

            return auctions;
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
    @Produces({MediaType.WILDCARD})
    @Path("{identity}/avatar")
    public Response getAvatar(@PathParam("identity") long personIdentity, @HeaderParam("Authorization") String authString) {
        try {
            LifeCycleProvider.authenticate(authString);
            Person person = getEM().find(Person.class, personIdentity);
            if (person == null) {
                throw new ClientErrorException(404);
            }
            Document avatar = person.getAvatar();
            //respone with no content if avatar null else response with avatar content
            Response res = avatar == null ? Response.noContent().build() : Response.ok(avatar.getContent(), avatar.getType()).build();

            return res;
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
    @Consumes({MediaType.WILDCARD})
    @Path("{identity}/avatar")
    public void setAvatar(
            @NotNull byte[] documentContent,
            @NotNull @HeaderParam("Content-type") String contentType,
            @PathParam("identity") long personIdentity,
            @HeaderParam("Authorization") String authString
    ) {
        Document doc = null;
        try {
            Person requester = LifeCycleProvider.authenticate(authString);
            Person person = getEM().find(Person.class, personIdentity);
            if (person == null) {
                throw new ClientErrorException(404);
            }
            if (requester.getIdentity() != personIdentity && !requester.getGroup().equals(Person.Group.ADMIN)) {
                throw new ClientErrorException(403);
            }
            byte[] docHash = Document.getHash(documentContent);
            TypedQuery query = getEM().createQuery(SQL_AVATAR, Document.class);
            query.setParameter("docHash", docHash);
            List<Long> result = query.getResultList();
            if (result.size() == 1) {
                doc = getEM().find(Document.class, result.get(0));
                doc.setType(contentType);
                getEM().flush();
            } else {
                doc = new Document(contentType, documentContent);
                getEM().persist(doc);
            }
            getEM().getTransaction().commit();
            getEM().getTransaction().begin();
            person.setAvatar(doc);
            getEM().flush();
        } catch (IllegalArgumentException exception) {
            throw new ClientErrorException(400);
        } catch (NotAuthorizedException exception) {
            throw new ClientErrorException(401);
        } catch (RollbackException exception) {
            throw new ClientErrorException(409);
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
    public Bid[] getBids(@PathParam("identity") long personIdentity, @HeaderParam("Authorization") String authString) {
        try {
            Person requester = LifeCycleProvider.authenticate(authString);
            Person person = getEM().find(Person.class, personIdentity);
            if (person == null) {
                throw new ClientErrorException(404);
            }
            Collection<Bid> resultList = person.getBids();
            List<Bid> matchingBidsList = new ArrayList<>();

            for (Bid bid : resultList) {
                Auction auction = bid.getAuction();
                if (auction.isClosed() || requester.getIdentity() == personIdentity) {
                    matchingBidsList.add(bid);
                }
            }
            Bid[] bids = matchingBidsList.toArray(new Bid[0]);
            Arrays.sort(bids, Comparator.comparing(Bid::getPrice));
            return bids;
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
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.WILDCARD})
    //Creates or modifies an auction from the given template data. Note that an auction may only be modified as long as it is not sealed (i.e. is open and still without bids).
    public long setPerson(
            @Valid @NotNull Person personTemplate,
            @HeaderParam("Set-password") String newPassword,
            @HeaderParam("Authorization") String authString
    ) {

        try {
            Person requester = LifeCycleProvider.authenticate(authString);
            if ((requester.getIdentity() != personTemplate.getIdentity() || personTemplate.getGroup().equals(Person.Group.ADMIN))
                    && !requester.getGroup().equals(Person.Group.ADMIN)) {
                throw new ClientErrorException(403);
            }
            long identity;
            Person person = getEM().find(Person.class, personTemplate.getIdentity());

            if (person == null) {
                getEM().persist(personTemplate);
                getEM().getTransaction().commit();
                identity = personTemplate.getIdentity();
            } else {
                person.setAlias(personTemplate.getAlias());
                person.setGroup(personTemplate.getGroup());
                person.getAddress().setCity(personTemplate.getAddress().getCity());
                person.getAddress().setPostCode(personTemplate.getAddress().getPostCode());
                person.getAddress().setStreet(personTemplate.getAddress().getStreet());
                person.getName().setFamily(personTemplate.getName().getFamily());
                person.getName().setGiven(personTemplate.getName().getGiven());
                if (newPassword != "" && newPassword != null) {
                    person.setPasswordHash(Person.getHash(newPassword.getBytes()));
                }
                identity = person.getIdentity();
                getEM().flush();
            }

            return identity;
        } catch (IllegalArgumentException exception) {
            throw new ClientErrorException(400);
        } catch (NotAuthorizedException exception) {
            throw new ClientErrorException(401);
        } catch (RollbackException exception) {
            throw new ClientErrorException(409);
        } finally {
            if (!getEM().getTransaction().isActive())
                getEM().getTransaction().begin();
        }
    }
}