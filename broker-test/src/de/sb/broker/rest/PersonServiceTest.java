package de.sb.broker.rest;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Person;
import org.junit.Test;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

public class PersonServiceTest extends ServiceTest {
	
    static private final int RESPONSE_CODE_200 = 200; // no exception & result
    static private final int RESPONSE_CODE_204 = 204; // no exception & void
    static private final int RESPONSE_CODE_400 = 400; // illegal argument
    static private final int RESPONSE_CODE_401 = 401; // not authenticated
    static private final int RESPONSE_CODE_403 = 403; // not authorized 
    static private final int RESPONSE_CODE_404 = 404; // not found
    static private final int RESPONSE_CODE_409 = 409; // resource collision
	
    static private final String USER_SASCHA = "sascha";
    static private final String PASSWORD_SASCHA = "sascha";    
	
	@Test
	public void testCriteriaQueries(){
        // getPeople
		
		// authentication
        WebTarget webTarget = newWebTarget(USER_SASCHA, "").path("people");
        assertEquals(RESPONSE_CODE_401, webTarget.request().get().getStatus());
        
        webTarget = newWebTarget(USER_SASCHA, PASSWORD_SASCHA).path("people");       
        // illegal arguments
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("offset", -1).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("limit", -1).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("alias", "").request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("family-name", "").request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("family-name", new String(new char[0])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("family-name", new String(new char[32])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("given-name", "").request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("given-name", 0).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("given-name", 32).request().get().getStatus());
        //assertEquals(RESPONSE_CODE_400, webTarget.queryParam("group", null).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("city", "").request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("city", new String(new char[0])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("city", new String(new char[64])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("post-code", "").request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("post-code", new String(new char[0])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("post-code", new String(new char[16])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("street", "").request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("street", new String(new char[0])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("street", new String(new char[64])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("email", "").request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("email", new String(new char[0])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("email", new String(new char[64])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("phone", "").request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("phone", new String(new char[0])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("phone", new String(new char[64])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("minimum-creation", 0).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("maximum-creation", 0).request().get().getStatus());
        
        assertEquals(RESPONSE_CODE_200, webTarget.request(MediaType.APPLICATION_JSON).get().getStatus());
        assertEquals(RESPONSE_CODE_200, webTarget.request(MediaType.APPLICATION_XML).get().getStatus());
	}
	
	@Test
	public void testGetPerson(){
        //WebTarget webTarget = newWebTarget(USER_SASCHA, "").path("people/2"); // sascha == 1 or 2 ?
        //assertEquals(RESPONSE_CODE_401, webTarget.request().get().getStatus());
	}
	
	@Test
	public void testIdentityQueries(){
		// getPerson
		// setPerson
		// getRequester
	}
	
	@Test
	public void testAuctionRelationQueries(){
		//getAuctions
	}
	
	@Test
	public void testBidRelationQueries(){
		//getBids
	}
	
	@Test
	public void testLifeCycle(){
		// create, update, delete...
	}

}
