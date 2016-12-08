package de.sb.broker.rest;

import de.sb.broker.model.*;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.util.*;

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
			@HeaderParam("Authorization") String authString) {
		try {
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

	@PUT
	@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.WILDCARD})
	public long setPerson(
			@Valid @NotNull Person personTemplate,
			@HeaderParam("Set-password") String newPassword,
			@HeaderParam("Authorization") String authString) {
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
				person = new Person(); //person neu befüllen?
				personTemplate.setPasswordHash(Person.getHash(newPassword.getBytes()));
				getEM().persist(personTemplate); //was, wenn die person felder hat die man nicht befüllen sollte?
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
				if (newPassword != null) {
					person.setPasswordHash(Person.getHash(newPassword.getBytes()));
				}
				identity = person.getIdentity();
				getEM().getTransaction().commit();
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

	@GET
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	@Path("{identity}")
	//Returns  the person matching the given identity.
	public Person getPerson(@PathParam("identity") long personIdentity,
							@HeaderParam("Authorization") String authString) {
		try {
			LifeCycleProvider.authenticate(authString);
			Person person = getEM().find(Person.class, personIdentity);

			if (person == null) {
				throw new ClientErrorException(404);
			}

			return person;
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
	@Path("requester")
	//Returns the requester
	public Person getRequester(@HeaderParam("Authorization") String authString) {
		try {
			return LifeCycleProvider.authenticate(authString);
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
	public Response getAuctions(
			@PathParam("identity") long personIdentity,
			@QueryParam("seller") Boolean seller,
			@HeaderParam("Authorization") String authString) {
		try {
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

			Collections.sort(auctions, Comparator.comparing(Auction::getClosureTimestamp)
					.thenComparing(Auction::getCreationTimestamp)
					.thenComparing(Auction::getIdentity));
			GenericEntity<?> wrapper = new GenericEntity<Collection<Auction>>(auctions) {
			};

			return Response.ok().entity(wrapper, filterAnnotations.toArray(new Annotation[0])).build();
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
	@Path("{identity}/bids")
	@Bid.XmlBidderAsReferenceFilter
	@Bid.XmlBidderAsEntityFilter
	//Returns all bids for closed auctions
	public Bid[] getBids(@PathParam("identity") long personIdentity,
						 @HeaderParam("Authorization") String authString) {
		try {
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

			if (avatar == null) {
				return Response.noContent().build();
			}

			return Response.ok(avatar.getContent(), avatar.getType()).build();
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
	public Response setAvatar(
			@NotNull byte[] documentContent,
			@NotNull @HeaderParam("Content-type") String contentType,
			@PathParam("identity") long personIdentity,
			@HeaderParam("Authorization") String authString) {
		Person person = null;
		try {
			Person requester = LifeCycleProvider.authenticate(authString);
			person = getEM().find(Person.class, personIdentity);

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
			getEM().getTransaction().commit();
			getEM().getTransaction().begin();
			person.setAvatar(document);
			getEM().getTransaction().commit();

			return Response.ok().build();
		} catch (IllegalArgumentException exception) {
			throw new ClientErrorException(400);
		} catch (NotAuthorizedException exception) {
			throw new ClientErrorException(401);
		} catch (RollbackException exception) {
			throw new ClientErrorException(409);
		} finally {
			if (!getEM().getTransaction().isActive())
				getEM().getTransaction().begin();
			if (person != null) {
				Cache cache = getEM().getEntityManagerFactory().getCache();
				cache.evict(person.getClass(), person.getIdentity());
			}
		}
	}
}