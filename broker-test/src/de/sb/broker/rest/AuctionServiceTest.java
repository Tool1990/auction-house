package de.sb.broker.rest;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Person;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
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

    static private final int RESPONSE_CODE_200 = 200;
    static private final int RESPONSE_CODE_204 = 204;
    static private final int RESPONSE_CODE_400 = 400;
    static private final int RESPONSE_CODE_401 = 401;
    static private final int RESPONSE_CODE_403 = 403;
    static private final int RESPONSE_CODE_404 = 404;
    static private final int RESPONSE_CODE_409 = 409;
    static private final String USER_INES = "ines";
    static private final String PASSWORD_INES = "ines";


    @Test
    public void testCriteriaQueries() throws JAXBException {
        WebTarget webTarget = newWebTarget(USER_INES, "").path("auctions");
        assertEquals(webTarget.request().get().getStatus(), RESPONSE_CODE_401);

        webTarget = newWebTarget(USER_INES, PASSWORD_INES).path("auctions");
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("offset", -1).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("limit", -1).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("title", "").request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("title", new String(new char[256])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("description", "").request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("description", new String(new char[8190])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("minimum-price", 0).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("maximum-price", 0).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("minimum-count", 0).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("maximum-count", 0).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("minimum-creation", 0).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("maximum-creation", 0).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("minimum-closure", 0).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("maximum-closure", 0).request().get().getStatus());

        assertEquals(RESPONSE_CODE_200, webTarget.request(MediaType.APPLICATION_JSON).get().getStatus());
//        assertEquals(RESPONSE_CODE_200, webTarget.request(MediaType.APPLICATION_XML).get().getStatus());

        Auction[] auctions = webTarget.request(MediaType.APPLICATION_JSON).get(Auction[].class);
        assertFalse(auctions.length == 0);
    }

    @Test
    public void testGetAuction() {
        WebTarget webTarget = newWebTarget(USER_INES, "");
        Response response = webTarget.path("auctions/1").request().get();
        assertEquals(RESPONSE_CODE_401, response.getStatus());

        webTarget = newWebTarget(USER_INES, PASSWORD_INES);
        response = webTarget.path("auctions/0").request().get();
        assertEquals(RESPONSE_CODE_404, response.getStatus());

        response = webTarget.path("auctions/3").request().get();
        assertEquals(RESPONSE_CODE_200, response.getStatus());
        Auction auction = response.readEntity(Auction.class);
        assertEquals(3, auction.getIdentity());

    }

    @Test
    public void testSetAuction() throws JAXBException {

        WebTarget webTarget = newWebTarget(USER_INES, PASSWORD_INES);
        Auction auction = webTarget.path("auctions/7").request().get().readEntity(Auction.class);
        Response response = webTarget.path("auctions").request().put(Entity.json(auction));
        assertEquals(RESPONSE_CODE_403, response.getStatus());

        auction = createTestAuction();
        response = webTarget.path("auctions").request().put(Entity.json(auction));
        assertEquals(RESPONSE_CODE_200, response.getStatus());

        long identity = response.readEntity(Long.class);
        this.getWasteBasket().add(identity);

        webTarget = newWebTarget(USER_INES, "");
        response = webTarget.path("auctions").request().put(Entity.json(auction));
        assertEquals(RESPONSE_CODE_401, response.getStatus());
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
    public void testGetBid() {
        WebTarget webTarget = newWebTarget(USER_INES, "");
        Response response = webTarget.path("auctions/6/bid").request().get();
        assertEquals(RESPONSE_CODE_401, response.getStatus());

        webTarget = newWebTarget(USER_INES, PASSWORD_INES);
        response = webTarget.path("auctions/0/bid").request().get();
        assertEquals(RESPONSE_CODE_404, response.getStatus());

        response = webTarget.path("auctions/5/bid").request().get();
        assertEquals(RESPONSE_CODE_204, response.getStatus());

        response = webTarget.path("auctions/6/bid").request().get();
        assertEquals(RESPONSE_CODE_200, response.getStatus());

    }

}
