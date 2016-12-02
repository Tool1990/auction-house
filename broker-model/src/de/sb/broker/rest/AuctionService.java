package de.sb.broker.rest;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Bid;
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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.util.*;

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

	@Auction.XmlSellerAsEntityFilter
	@GET
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	//Returns the auctions matching the given criteria, with null or missing parameters identifying omitted criteria.
	public Response getAuctions(
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
			@QueryParam("closed") Boolean closed,
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

			List<Long> auctionIdentities = q.getResultList();
			List<Auction> auctions = new ArrayList<>();

			for (Long identity : auctionIdentities) {
				auctions.add(getEM().find(Auction.class, identity));
			}

			List<Annotation> filterAnnotations = new ArrayList<>();

			if (closed == Boolean.TRUE) {
				for (Iterator<Auction> iterator = auctions.iterator(); iterator.hasNext();) {
					if (!iterator.next().isClosed()) {
						iterator.remove();
					}
				}

				filterAnnotations.add(new Auction.XmlBidsAsEntityFilter.Literal());
				filterAnnotations.add(new Bid.XmlBidderAsEntityFilter.Literal());
			} else if (closed == Boolean.FALSE) {
				for (Iterator<Auction> iterator = auctions.iterator(); iterator.hasNext();) {
					if (iterator.next().isClosed()){
						iterator.remove();
					}
				}
			}

			Collections.sort(auctions, Comparator.comparing(Auction::getTitle));
			GenericEntity<?> wrapper = new GenericEntity<Collection<Auction>>(auctions) {};

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

	@Auction.XmlSellerAsReferenceFilter
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

	@Bid.XmlBidderAsReferenceFilter
	@Bid.XmlAuctionAsReferenceFilter
	@GET
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	@Path("{identity}/bid")
	//Returns the requesters bid for the auction matching the given identity
	public Bid getBid(@PathParam("identity") long auctionIdentity, @HeaderParam("Authorization") String authString) {
		try {
			Person requester = LifeCycleProvider.authenticate(authString);
			Auction auction = getEM().find(Auction.class, auctionIdentity);
			if (auction == null) {
				throw new ClientErrorException(404);
			} else {
				return auction.getBid(requester);
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

	@POST
	@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	@Path("{identity}/bid")
	//Creates or modifies the requesters bid for the given auction
	public void setBid(
			@PathParam("identity") long auctionIdentity,
			@Min(0) long price,
			@HeaderParam("Authorization") String authString
	) {
		Auction auction = null;
		Person requester = null;
		try {
			requester = LifeCycleProvider.authenticate(authString);
			auction = getEM().find(Auction.class, auctionIdentity);
			if (auction == null) {
				throw new ClientErrorException(404);
			}
			Bid bid = auction.getBid(requester);

			if (bid == null) { //create new bid
				bid = new Bid(auction, requester);
				bid.setPrice(price);
				getEM().persist(bid);
				getEM().getTransaction().commit();
				getEM().getTransaction().begin();
			} else if (price == 0) { //remove requesters bid
				bid = getEM().getReference(Bid.class, bid.getIdentity());
				getEM().remove(bid);
				getEM().getTransaction().commit();
			} else { //update requesters bid
				bid = getEM().find(Bid.class, bid.getIdentity());
				bid.setPrice(price);
				getEM().flush();
			}
		} catch (IllegalArgumentException exception) {
			throw new ClientErrorException(400);
		} catch (NotAuthorizedException exception) {
			throw new ClientErrorException(401);
		} catch (RollbackException exception) {
			throw new ClientErrorException(409);
		} finally {
			if (!getEM().getTransaction().isActive())
				getEM().getTransaction().begin();

			if (auction != null) {
				Cache cache = getEM().getEntityManagerFactory().getCache();
				cache.evict(auction.getClass(), auction.getIdentity());
				cache.evict(requester.getClass(), requester.getIdentity());
			}
		}
	}
}