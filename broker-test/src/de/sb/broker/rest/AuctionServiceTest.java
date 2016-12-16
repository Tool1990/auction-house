package de.sb.broker.rest;

import de.sb.broker.model.Auction;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by Wayne on 06.12.2016.
 */
public class AuctionServiceTest extends ServiceTest {

    static private final String USER_INES = "ines";
    static private final String PASSWORD_INES = "ines";
    static private final String USER_SASCHA = "sascha";
    static private final String PASSWORD_SASCHA = "sascha";


    @Test
    public void testCriteriaQueries() throws JAXBException {
        WebTarget webTarget = newWebTarget(USER_INES, "").path("auctions");
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), webTarget.request().get().getStatus());

        webTarget = newWebTarget(USER_INES, PASSWORD_INES).path("auctions");
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("offset", -1).request().get().getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("limit", -1).request().get().getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("title", "").request().get().getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("title", new String(new char[256])).request().get().getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("description", "").request().get().getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("description", new String(new char[8190])).request().get().getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("minimum-price", 0).request().get().getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("maximum-price", 0).request().get().getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("minimum-count", 0).request().get().getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("maximum-count", 0).request().get().getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("minimum-creation", 0).request().get().getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("maximum-creation", 0).request().get().getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("minimum-closure", 0).request().get().getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("maximum-closure", 0).request().get().getStatus());

        assertEquals(Response.Status.OK.getStatusCode(), webTarget.request(MediaType.APPLICATION_JSON).get().getStatus());
//        assertEquals(Response.Status.OK.getStatusCode(), webTarget.request(MediaType.APPLICATION_XML).get().getStatus());

        Auction[] auctions = webTarget.request(MediaType.APPLICATION_JSON).get(Auction[].class);
        assertFalse(auctions.length == 0);
    }

    @Test
    public void testGetAuction() {
        WebTarget webTarget = newWebTarget(USER_INES, "");
        Response response = webTarget.path("auctions/1").request().get();
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        webTarget = newWebTarget(USER_INES, PASSWORD_INES);
        response = webTarget.path("auctions/0").request().get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        response = webTarget.path("auctions/3").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Auction auction = response.readEntity(Auction.class);
        assertEquals(3, auction.getIdentity());

    }

    @Test
    public void testSetAuction() throws JAXBException {

        WebTarget webTarget = newWebTarget(USER_INES, PASSWORD_INES);
        Auction auction = webTarget.path("auctions/7").request().get().readEntity(Auction.class);
        Response response = webTarget.path("auctions").request().put(Entity.json(auction)); // isSealed
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        auction = createTestAuction();
        response = webTarget.path("auctions").request().put(Entity.json(auction)); //new auction
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        long identity = response.readEntity(Long.class);
        this.getWasteBasket().add(identity);

        auction = webTarget.path("auctions/" + identity).request().get().readEntity(Auction.class);
        response = webTarget.path("auctions").request().put(Entity.json(auction)); // new auction !isSealed
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        webTarget = newWebTarget(USER_INES, "");
        response = webTarget.path("auctions").request().put(Entity.json(auction));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    private Auction createTestAuction() {
        Auction auction = new Auction();
        auction.setAskingPrice(200);
        Calendar cal = Calendar.getInstance();
        cal.set(2017, 11, 03);
        cal.add(Calendar.DATE, 1);
        auction.setClosureTimestamp(cal.getTime().getTime());
        auction.setDescription("This is a Test Auction");
        auction.setTitle("testAuction");
        auction.setUnitCount((short) 1);

        return auction;
    }

    @Test
    public void testSetBid() {
        WebTarget webTarget = newWebTarget(USER_INES, "");
        Response response = webTarget.path("auctions/6/bid").request().post(Entity.entity(-1L, MediaType.TEXT_PLAIN));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        Entity entity = Entity.entity(1000L, MediaType.TEXT_PLAIN);
        response = webTarget.path("auctions/6/bid").request().post(entity);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        webTarget = newWebTarget(USER_INES, PASSWORD_INES);
        response = webTarget.path("auctions/-1/bid").request().post(entity);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        //bid on own auction
        Auction auction = createTestAuction();
        response = webTarget.path("auctions").request().put(Entity.json(auction));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        long auctionIdentity = response.readEntity(Long.class);
        this.getWasteBasket().add(auctionIdentity);
        response = webTarget.path("auctions/" + auctionIdentity + "/bid").request().post(entity);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        //new bid
        webTarget = newWebTarget(USER_SASCHA, PASSWORD_SASCHA);
        response = webTarget.path("auctions/" + auctionIdentity + "/bid").request().post(entity);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        long bidIdentity = response.readEntity(Long.class);
        this.getWasteBasket().add(bidIdentity);

        //update bid
        response = webTarget.path("auctions/" + auctionIdentity + "/bid").request().post(entity);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        //remove bid
        response = webTarget.path("auctions/" + auctionIdentity + "/bid").request().post(Entity.entity(0L, MediaType.TEXT_PLAIN));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        response = webTarget.path("auctions/" + auctionIdentity + "/bid").request().get();
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

    }

    @Test
    public void testGetBid() {
        WebTarget webTarget = newWebTarget(USER_INES, "");
        Response response = webTarget.path("auctions/6/bid").request().get();
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        webTarget = newWebTarget(USER_INES, PASSWORD_INES);
        response = webTarget.path("auctions/0/bid").request().get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        response = webTarget.path("auctions/5/bid").request().get();
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        response = webTarget.path("auctions/6/bid").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    }

}
