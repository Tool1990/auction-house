package de.sb.broker.rest;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Person;
import de.sb.java.net.HttpAuthenticationCodec;
import org.glassfish.jersey.server.ParamException;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.IllegalFormatException;
import java.util.List;

@Path("auctions")
public class AuctionService {

    public EntityManager getEM(){
        return LifeCycleProvider.brokerManager();
    }

    public Person authenticate(String authHeaderString){
        Person person = LifeCycleProvider.authenticate(authHeaderString);
        if (person == null)
            throw new ClientErrorException(401);
        return person;
    }



    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    //Returns the auctions matching the given criteria, with null or missing parameters identifying omitted criteria.
    public Auction[] getAuctions(@QueryParam("title") String title, @QueryParam("priceMin") long priceMin, @QueryParam("priceMax") @Min(1) long priceMax, @HeaderParam("Authorization") String authString) {
        Person person = authenticate(authString);
        if (!person.getGroup().equals(Person.Group.ADMIN)){
            throw new ClientErrorException(404);
        }
        Query q;
        Auction[] matchingAuctions = null;
        try {
            if (priceMax == 0) {
                q = getEM().createQuery("select a.identity from Auction as a where (:title is null or a.title = :title) and (:priceMin is null or a.askingPrice >= :priceMin)").setParameter("title", title).setParameter("priceMin", priceMin);

            } else {
                q = getEM().createQuery("select a.identity from Auction as a where (:title is null or a.title = :title) and (:priceMin is null or a.askingPrice >= :priceMin) and (:priceMax is null or a.askingPrice <= :priceMax)").setParameter("title", title).setParameter("priceMin", priceMin).setParameter("priceMax", priceMax);
            }
            List resultList = q.getResultList();
            matchingAuctions = new Auction[resultList.size()];
            for (int i = 0; i < resultList.size(); i++) {
                matchingAuctions[i] = getEM().find(Auction.class, resultList.get(i));
            }
        } catch (Exception e) {
        } finally {
            if (!getEM().getTransaction().isActive())
                getEM().getTransaction().begin();
        }
        return matchingAuctions;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("{identity}")
    //Returns the auction matching the given identity
    public Auction getAuction(@PathParam("identity") long auctionIdentity) {
        Auction auction = null;
        try {
            auction = getEM().find(Auction.class, auctionIdentity);


        }catch(RollbackException exception){
            throw new ClientErrorException(409);
        }catch(IllegalArgumentException exception){
            throw new ClientErrorException(400);
        } finally {
            if (!getEM().getTransaction().isActive())
                getEM().getTransaction().begin();
        }

        if (auction == null){
            throw new ClientErrorException(404);
        }
        return auction;
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    //Creates or modifies an auction from the given template data. Note that an auction may only be modified as long as it is not sealed (i.e. is open and still without bids).
    public void setAuction(Auction auctionTemplate) {

        Auction auction = getEM().find(Auction.class, auctionTemplate.getIdentity());
        if (auction == null) {
            try {
                getEM().getTransaction().begin();
                getEM().persist(auctionTemplate);
                getEM().getTransaction().commit();
                getEM().getTransaction().begin();
            } catch (Exception e) {
            } finally {
                if (!getEM().getTransaction().isActive())
                    getEM().getTransaction().begin();
            }
        } else if (!auction.isSealed()) {
            try {
                getEM().getTransaction().begin();
                auction.setTitle(auctionTemplate.getTitle());
                auction.setAskingPrice(auctionTemplate.getAskingPrice());
                auction.setClosureTimestamp(auctionTemplate.getClosureTimestamp());
                auction.setDescription(auctionTemplate.getDescription());
                auction.setUnitCount(auctionTemplate.getUnitCount());
                getEM().flush();
            } catch (Exception e) {
            } finally {
                Cache cache = getEM().getEntityManagerFactory().getCache();
                cache.evict(auctionTemplate.getSeller().getClass(), auctionTemplate.getSeller().getIdentity());

                if (!getEM().getTransaction().isActive())
                    getEM().getTransaction().begin();
            }
        }
    }
}

