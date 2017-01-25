package de.sb.broker.rest;

import de.sb.broker.model.*;

import javax.imageio.ImageIO;
import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.List;

@Path("people")
public class PersonService {
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
            @QueryParam("offset") @Min(0) int offset,
            @QueryParam("limit") @Min(0) int limit,
            @QueryParam("alias") @Size(min = 1, max = 63) String alias,
            @QueryParam("family-name") @Size(min = 1, max = 31) String familyName,
            @QueryParam("given-name") @Size(min = 1, max = 31) String givenName,
            @QueryParam("group") Person.Group group,
            @QueryParam("city") @Size(min = 1, max = 63) String city,
            @QueryParam("post-code") @Size(min = 1, max = 15) String postCode,
            @QueryParam("street") @Size(min = 1, max = 63) String street,
            @QueryParam("email") @Size(min = 1, max = 63) @Pattern(regexp = Contact.EMAIL_PATTERN) String email,
            @QueryParam("phone") @Size(min = 1, max = 63) String phone,
            @QueryParam("minimum-creation") @Min(1) Long creationMin,
            @QueryParam("maximum-creation") @Min(1) Long creationMax,
            @HeaderParam("Authorization") String authString
    ) {
        LifeCycleProvider.authenticate(authString);

        TypedQuery<Long> query = getEM().createQuery(SQL_PEOPLE, Long.class);
        query.setParameter("familyName", familyName);
        query.setParameter("givenName", givenName);
        query.setParameter("alias", alias);
        query.setParameter("group", group);
        query.setParameter("city", city);
        query.setParameter("postCode", postCode);
        query.setParameter("street", street);
        query.setParameter("email", email);
        query.setParameter("phone", phone);
        query.setParameter("creationMin", creationMin);
        query.setParameter("creationMax", creationMax);

        if (offset > 0)
            query.setFirstResult(offset);
        if (limit > 0)
            query.setMaxResults(limit);

        List<Long> resultList = query.getResultList();
        Person[] matchingPeople = new Person[resultList.size()];

        for (int i = 0; i < resultList.size(); i++) {
            matchingPeople[i] = getEM().find(Person.class, resultList.get(i));
        }

        Arrays.sort(matchingPeople, Comparator.comparing(Person::getAlias)); //(Person person) -> person.getAlias()
        return matchingPeople;
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.WILDCARD})
    public long setPerson(
            @Valid @NotNull Person personTemplate,
            @HeaderParam("Set-password") String newPassword,
            @HeaderParam("Authorization") String authString
    ) {
        try {
            Person requester = LifeCycleProvider.authenticate(authString);
            if ((requester.getIdentity() != personTemplate.getIdentity() && !requester.getGroup().equals(Person.Group.ADMIN))) {
                throw new ClientErrorException(403);
            }

            if (!requester.getGroup().equals(Person.Group.ADMIN) && personTemplate.getGroup().equals(Person.Group.ADMIN)) {
                throw new ClientErrorException(403);
            }

            long identity;
            Person person = getEM().find(Person.class, personTemplate.getIdentity());

            if (person == null) {
                personTemplate.setPasswordHash(Person.getHash(newPassword.getBytes()));
                getEM().persist(personTemplate); //was, wenn die person felder hat die man nicht befüllen sollte?
                try {
                    getEM().getTransaction().commit();
                } finally {
                    getEM().getTransaction().begin();
                }
                identity = personTemplate.getIdentity();
            } else {
                person.setAlias(personTemplate.getAlias());
                person.setGroup(personTemplate.getGroup());
                person.getAddress().setCity(personTemplate.getAddress().getCity());
                person.getAddress().setPostCode(personTemplate.getAddress().getPostCode());
                person.getAddress().setStreet(personTemplate.getAddress().getStreet());
                person.getName().setFamily(personTemplate.getName().getFamily());
                person.getName().setGiven(personTemplate.getName().getGiven());
                if (newPassword != null) {
                    person.setPasswordHash(Person.getHash(newPassword.getBytes()));
                }
                identity = person.getIdentity();
                try {
                    getEM().getTransaction().commit();
                } finally {
                    getEM().getTransaction().begin();
                }
            }

            return identity;
        } catch (RollbackException exception) {
            throw new ClientErrorException(409);
        } catch (ConstraintViolationException ex) {
            throw new ClientErrorException(400);
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}")
    //Returns  the person matching the given identity.
    public Person getPerson(@PathParam("identity") long personIdentity,
                            @HeaderParam("Authorization") String authString
    ) {
        LifeCycleProvider.authenticate(authString);
        Person person = getEM().find(Person.class, personIdentity);

        if (person == null) {
            throw new ClientErrorException(404);
        }

        return person;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("requester")
    //Returns the requester
    public Person getRequester(@HeaderParam("Authorization") String authString) {
        return LifeCycleProvider.authenticate(authString);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}/auctions")
    //Returns all auctions associated with the person matching the given identity (as seller or bidder).
    public Response getAuctions(
            @PathParam("identity") long personIdentity,
            @QueryParam("seller") Boolean seller,
            @QueryParam("closed") Boolean closed,
            @HeaderParam("Authorization") String authString
    ) {
        LifeCycleProvider.authenticate(authString);
        Person person = getEM().find(Person.class, personIdentity);

        if (person == null) {
            throw new ClientErrorException(404);
        }

        List<Auction> auctions = new ArrayList<>();
        List<Annotation> filterAnnotations = new ArrayList<>();

        if (seller == Boolean.TRUE) {
            auctions.addAll(person.getAuctions());
            filterAnnotations.add(new Auction.XmlSellerAsReferenceFilter.Literal());
        } else if (seller == Boolean.FALSE) {
            for (Bid bid : person.getBids()) {
                if (bid.getBidderReference() == personIdentity) {
                    auctions.add(bid.getAuction());
                }
            }

            filterAnnotations.add(new Auction.XmlSellerAsEntityFilter.Literal());
        } else {
            auctions.addAll(person.getAuctions());

            for (Bid bid : person.getBids()) {
                if (bid.getBidderReference() == personIdentity) {
                    auctions.add(bid.getAuction());
                }
            }

            filterAnnotations.add(new Auction.XmlSellerAsEntityFilter.Literal());
        }

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

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}/bids")
    @Bid.XmlBidderAsReferenceFilter
    @Bid.XmlBidderAsEntityFilter
    //Returns all bids for closed auctions
    public Bid[] getBids(@PathParam("identity") long personIdentity,
                         @HeaderParam("Authorization") String authString
    ) {
        Person requester = LifeCycleProvider.authenticate(authString);
        Person person = getEM().find(Person.class, personIdentity);

        if (person == null) {
            throw new ClientErrorException(404);
        }

        Set<Bid> matchingBids = new HashSet<>();

        for (Bid bid : person.getBids()) {
            Auction auction = bid.getAuction();
            if (auction.isClosed() || requester.getIdentity() == personIdentity) {
                matchingBids.add(bid);
            }
        }

        Bid[] bids = matchingBids.toArray(new Bid[0]);
        Arrays.sort(bids, Comparator.comparing(Bid::getCreationTimestamp).thenComparing(Bid::getPrice).thenComparing(Bid::getIdentity));
        return bids;
    }

    @GET
    @Produces({MediaType.WILDCARD})
    @Path("{identity}/avatar")
    public Response getAvatar(
            @PathParam("identity") long personIdentity,
            @HeaderParam("Authorization") String authString,
            @QueryParam("width") @Min(0) Integer width,
            @QueryParam("height") @Min(0) Integer height
            ) {
        LifeCycleProvider.authenticate(authString);
        Person person = getEM().find(Person.class, personIdentity);

        if (person == null) {
            throw new ClientErrorException(404);
        }

        Document avatar = person.getAvatar();

        if (avatar == null) {
            return Response.noContent().build();
        }
        byte[] avatarAsByteArray = avatar.getContent();

        if ((width != null) || (height != null)) {
            int imageWidth, imageHeight;
            try {
                InputStream in = new ByteArrayInputStream(avatar.getContent());
                BufferedImage originalImage = ImageIO.read(in);
                int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();

                imageHeight = height != null ? height : originalImage.getHeight();
                imageWidth = width != null ? width : originalImage.getWidth();

                BufferedImage resizedImage = new BufferedImage(imageWidth, imageHeight, type);
                Graphics2D g = resizedImage.createGraphics();
                g.drawImage(originalImage, 0, 0, imageWidth, imageHeight, null);
                g.dispose();

                String imageType = avatar.getType().substring(avatar.getType().lastIndexOf("/") + 1);
                ByteArrayOutputStream baos=new ByteArrayOutputStream();
                ImageIO.write(resizedImage, imageType, baos);
                avatarAsByteArray = baos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Response.ok(avatarAsByteArray, avatar.getType()).build();
    }

    @PUT
    @Consumes({MediaType.WILDCARD})
    @Path("{identity}/avatar")
    public Response setAvatar(
            @NotNull byte[] documentContent,
            @NotNull @HeaderParam("Content-type") String contentType,
            @PathParam("identity") long personIdentity,
            @HeaderParam("Authorization") String authString
    ) {
        try {
            Person requester = LifeCycleProvider.authenticate(authString);
            Person person = getEM().find(Person.class, personIdentity);

            if (person == null) {
                throw new ClientErrorException(404);
            }
            if (requester.getIdentity() != personIdentity && !requester.getGroup().equals(Person.Group.ADMIN)) {
                throw new ClientErrorException(403);
            }
            byte[] documentHash = Document.getHash(documentContent);
            TypedQuery query = getEM().createQuery(SQL_AVATAR, Document.class);
            query.setParameter("docHash", documentHash);
            List<Long> result = query.getResultList();
            Document document;

            if (result.size() == 1) {
                document = getEM().find(Document.class, result.get(0));
                document.setType(contentType);
                getEM().flush();
            } else {
                document = new Document(contentType, documentContent);
                getEM().persist(document);
            }
            try {
                getEM().getTransaction().commit();
            } finally {
                getEM().getTransaction().begin();
            }
            person.setAvatar(document);
            try {
                getEM().getTransaction().commit();
            } finally {
                getEM().getTransaction().begin();
            }

            Cache cache = getEM().getEntityManagerFactory().getCache();
            cache.evict(person.getClass(), person.getIdentity());

            return Response.ok().build();
        } catch (ConstraintViolationException exception) {
            throw new ClientErrorException(400);
        } catch (RollbackException exception) {
            throw new ClientErrorException(409);
        }
    }
}