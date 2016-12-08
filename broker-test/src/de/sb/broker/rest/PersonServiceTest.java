package de.sb.broker.rest;

import org.junit.Test;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class PersonServiceTest extends ServiceTest {
	@Test
	public void testCriteriaQueries() {
		// getPeople

		// authentication
		WebTarget webTarget = newWebTarget(USER_SASCHA, "").path("people");
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), webTarget.request().get().getStatus());

		webTarget = newWebTarget(USER_SASCHA, PASSWORD_SASCHA).path("people");
		// illegal arguments
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("offset", -1).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("limit", -1).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("alias", "").request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("family-name", "").request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("family-name", new String(new char[0])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("family-name", new String(new char[32])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("given-name", "").request().get().getStatus());
		//assertEquals(Response.Status.BAD_REQUEST, webTarget.queryParam("group", null).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("city", "").request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("city", new String(new char[0])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("city", new String(new char[64])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("post-code", "").request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("post-code", new String(new char[0])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("post-code", new String(new char[16])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("street", "").request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("street", new String(new char[0])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("street", new String(new char[64])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("email", "").request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("email", new String(new char[0])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("email", new String(new char[64])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("phone", "").request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("phone", new String(new char[0])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("phone", new String(new char[64])).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("minimum-creation", 0).request().get().getStatus());
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webTarget.queryParam("maximum-creation", 0).request().get().getStatus());

		assertEquals(Response.Status.OK.getStatusCode(), webTarget.request(MediaType.APPLICATION_JSON).get().getStatus());
		assertEquals(Response.Status.OK.getStatusCode(), webTarget.request(MediaType.APPLICATION_XML).get().getStatus());
	}

	@Test
	public void testGetPerson() {
		//WebTarget webTarget = newWebTarget(USER_SASCHA, "").path("people/2"); // sascha == 1 or 2 ?
		//assertEquals(RESPONSE_CODE_401, webTarget.request().get().getStatus());
	}

	@Test
	public void testIdentityQueries() {
		// getPerson
		// setPerson
		// getRequester
	}

	@Test
	public void testAuctionRelationQueries() {
		//getAuctions
	}

	@Test
	public void testBidRelationQueries() {
		//getBids
	}

	@Test
	public void testLifeCycle() {
		// create, update, delete...
	}

}
