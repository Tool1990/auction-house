package de.sb.broker.rest;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Person;
import org.junit.Test;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

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
     public void testGetAuction() {
        WebTarget webTarget = newWebTarget(USER_INES, "");
        Response response = webTarget.path("auctions/1").request().get();
        assertEquals(RESPONSE_CODE_401, response.getStatus());

        webTarget = newWebTarget(USER_INES, PASSWORD_INES);
        response = webTarget.path("auctions/0").request().get();
        assertEquals(RESPONSE_CODE_404, response.getStatus());

        response = webTarget.path("auctions/3").request().get();
        assertEquals(RESPONSE_CODE_200, response.getStatus());

    }

    public Person getRequester(WebTarget webTarget) {
        Person requester = (Person) webTarget.path("people/requester").request().get().getEntity();
        return requester;
    }

    @Test
    public void testSetAuction() {
//        WebTarget webTarget = newWebTarget(USER_INES, "");
//        Response response = webTarget.path("auctions").request().put(null);
//        assertEquals(RESPONSE_CODE_401, response.getStatus());
//
//        webTarget = newWebTarget(USER_INES, PASSWORD_INES);
//        Person requester = getRequester(webTarget);
//        Auction auction = new Auction(requester);
//        auction.setTitle("");

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
