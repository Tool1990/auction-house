package de.sb.broker.rest;

import de.sb.broker.model.Person;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PersonServiceTest extends ServiceTest {

    static private final int RESPONSE_CODE_200 = 200; // no exception & result
    static private final int RESPONSE_CODE_204 = 204; // no exception & void
    static private final int RESPONSE_CODE_400 = 400; // illegal argument
    static private final int RESPONSE_CODE_401 = 401; // not authenticated
    static private final int RESPONSE_CODE_403 = 403; // not authorized
    static private final int RESPONSE_CODE_404 = 404; // not found
    static private final int RESPONSE_CODE_409 = 409; // resource collision

    static private final String USER_INES = "ines";
    static private final String PASSWORD_INES = "ines";
    private Person testPerson = new Person();
    private Person person;

    @Test
    public void testCriteriaQueries() {
        // getPeople

        // authentication
        WebTarget webTarget = newWebTarget(USER_INES, "").path("people");
        assertEquals(webTarget.request().get().getStatus(), RESPONSE_CODE_401);

        webTarget = newWebTarget(USER_INES, PASSWORD_INES).path("people");
        // illegal arguments
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("offset", -1).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("limit", -1).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("alias", new String(new char[0])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("family-name", "").request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("family-name", new String(new char[0])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("family-name", new String(new char[32])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("given-name", "").request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("given-name", new String(new char[0])).request().get().getStatus());
        assertEquals(RESPONSE_CODE_400, webTarget.queryParam("given-name", new String(new char[32])).request().get().getStatus());
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
//        assertEquals(RESPONSE_CODE_200, webTarget.request(MediaType.APPLICATION_XML).get().getStatus());

        String persons = webTarget.request(MediaType.APPLICATION_JSON).get().readEntity(String.class);
        assertFalse(persons.length() == 0);
    }


    @Test
    public void testIdentityQueries() {
        WebTarget webTarget = newWebTarget(USER_INES, "");
        Response response = webTarget.path("people/500").request().get();
        assertEquals(RESPONSE_CODE_401, response.getStatus());

        webTarget = newWebTarget(USER_INES, PASSWORD_INES);
        response = webTarget.path("people/0").request().get();
        assertEquals(RESPONSE_CODE_404, response.getStatus());

        response = webTarget.path("people/1").request().get();
        assertEquals(RESPONSE_CODE_200, response.getStatus());
        Person person = response.readEntity(Person.class);
        assertEquals(1, person.getIdentity());
    }

    @Test
    public void testAuctionRelationQueries() {
        WebTarget webTarget = newWebTarget(USER_INES, "");
        Response response = webTarget.path("people/1/auctions").request().get();
        assertEquals(RESPONSE_CODE_401, response.getStatus());

        webTarget = newWebTarget(USER_INES, PASSWORD_INES);
        response = webTarget.path("people/4/auctions").request().get();
        assertEquals(RESPONSE_CODE_404, response.getStatus());

        response = webTarget.path("people/1/auctions").request().get();
        assertEquals(RESPONSE_CODE_200, response.getStatus());
    }

    @Test
    public void testBidRelationQueries() {
        WebTarget webTarget = newWebTarget(USER_INES, "");
        Response response = webTarget.path("people/1/bids").request().get();
        assertEquals(RESPONSE_CODE_401, response.getStatus());

        webTarget = newWebTarget(USER_INES, PASSWORD_INES);
        response = webTarget.path("people/4/bids").request().get();
        assertEquals(RESPONSE_CODE_404, response.getStatus());

        response = webTarget.path("people/1/bids").request().get();
        assertEquals(RESPONSE_CODE_200, response.getStatus());

    }

    @Test
    public void testLifeCycle() {
        // create, update, delete...
        try {
            Person person = populateTestPerson();
            WebTarget webTarget = newWebTarget(USER_INES, PASSWORD_INES);
            Response response = webTarget.path("people").request().header("Set-password", "test123").put(Entity.json(person));
            long identity = response.readEntity(Long.class);
            getWasteBasket().add(identity);
            assertEquals(response.getStatus(), RESPONSE_CODE_200);

            person = webTarget.path("people/" + identity).request().get().readEntity(Person.class);
            person.setAlias("ichangedmyalias5");
            response = webTarget.path("people").request().put(Entity.json(person));
            assertEquals(response.getStatus(), RESPONSE_CODE_200);
            Person updatedPerson = webTarget.path("people/" + identity).request().get().readEntity(Person.class);
            assertEquals(updatedPerson.getAlias(), "ichangedmyalias5");

            assertEquals(RESPONSE_CODE_204, newWebTarget("ines", "ines").path("entities/" + identity).request().delete().getStatus());

        } finally {
            emptyWasteBasket();
        }


    }

    private Person populateTestPerson() {
        testPerson.setAlias("testAlias11");
        testPerson.setGroup(Person.Group.USER);
        testPerson.setPasswordHash(Person.getHash("123".getBytes()));
        testPerson.getName().setFamily("testFamilyName11");
        testPerson.getName().setGiven("testGivenName11");
        testPerson.getAddress().setCity("testCity1");
        testPerson.getContact().setEmail("testEmail@test11.de");
        testPerson.getContact().setPhone("01234567899");
        return testPerson;
    }

}
