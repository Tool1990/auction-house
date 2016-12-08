package de.sb.broker.rest;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Person;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by Wayne on 06.12.2016.
 */
public class AuctionServiceTest extends ServiceTest {

	@Test
	public void testCriteriaQueries() throws JAXBException {
		WebTarget webTarget = newWebTarget(USER_INES, "").path("auctions");
		assertEquals(webTarget.request().get().getStatus(), Response.Status.UNAUTHORIZED);

		webTarget = newWebTarget(USER_INES, PASSWORD_INES).path("auctions");
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("offset", -1).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("limit", -1).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("title", "").request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("title", new String(new char[256])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("description", "").request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("description", new String(new char[8190])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("minimum-price", 0).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("maximum-price", 0).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("minimum-count", 0).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("maximum-count", 0).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("minimum-creation", 0).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("maximum-creation", 0).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("minimum-closure", 0).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("maximum-closure", 0).request().get().getStatus());

		assertEquals(Response.Status.BAD_REQUEST, webTarget.request(MediaType.APPLICATION_JSON).get().getStatus());
		//assertEquals(RESPONSE_CODE_200, webTarget.request(MediaType.APPLICATION_XML).get().getStatus());

		String auctions = webTarget.request(MediaType.APPLICATION_JSON).get().readEntity(String.class);
		assertFalse(auctions.length() != 0);
	}

	@Test
	public void testGetAuction() {
		WebTarget webTarget = newWebTarget(USER_INES, "");
		Response response = webTarget.path("auctions/1").request().get();
		assertEquals(Response.Status.UNAUTHORIZED, response.getStatus());

		webTarget = newWebTarget(USER_INES, PASSWORD_INES);
		response = webTarget.path("auctions/0").request().get();
		assertEquals(Response.Status.NOT_FOUND, response.getStatus());

		response = webTarget.path("auctions/3").request().get();
		assertEquals(Response.Status.OK, response.getStatus());
		Auction auction = response.readEntity(Auction.class);
		assertEquals(3, auction.getIdentity());

	}

	public Person getRequester(WebTarget webTarget) {
		Person requester = webTarget.path("people/requester").request().get().readEntity(Person.class);
		return requester;
	}

	@Test
	public void testSetAuction() throws JAXBException {

		WebTarget webTarget = newWebTarget(USER_INES, PASSWORD_INES);
		Person requester = getRequester(webTarget);
		Auction auction = webTarget.path("auctions/7").request().get().readEntity(Auction.class);
		Response response = webTarget.path("auctions").request().put(Entity.json(auction));
		assertEquals(Response.Status.FORBIDDEN, response.getStatus());

		auction = new Auction(requester);
		auction.setTitle("TestAuction");
		auction.setDescription("Description");
		auction.setAskingPrice(10);
		auction.setUnitCount((short) 1);
		response = webTarget.path("auctions").request().put(Entity.json(auction));
		assertEquals(Response.Status.OK, response.getStatus());

		long identity = response.readEntity(Long.class);
		this.getWasteBasket().add(identity);

		webTarget = newWebTarget(USER_INES, "");
		response = webTarget.path("auctions").request().put(Entity.json(auction));
		assertEquals(Response.Status.UNAUTHORIZED, response.getStatus());
	}

	@Test
	public void testGetBid() {
		WebTarget webTarget = newWebTarget(USER_INES, "");
		Response response = webTarget.path("auctions/6/bid").request().get();
		assertEquals(Response.Status.UNAUTHORIZED, response.getStatus());

		webTarget = newWebTarget(USER_INES, PASSWORD_INES);
		response = webTarget.path("auctions/0/bid").request().get();
		assertEquals(Response.Status.NOT_FOUND, response.getStatus());

		response = webTarget.path("auctions/5/bid").request().get();
		assertEquals(Response.Status.NO_CONTENT, response.getStatus());

		response = webTarget.path("auctions/6/bid").request().get();
		assertEquals(Response.Status.OK, response.getStatus());

	}

}
