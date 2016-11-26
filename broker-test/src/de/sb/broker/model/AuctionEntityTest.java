package de.sb.broker.model;


import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.validation.Validator;
import java.util.Calendar;

import static org.junit.Assert.*;


public class AuctionEntityTest extends EntityTest {


    Auction auction;
    Person testPerson = new Person();

    private Auction createTestAuction() {
        auction.setAskingPrice(200);
        Calendar cal = Calendar.getInstance();
        cal.set(2017, 11, 03);
        auction.setClosureTimestamp(cal.getTime().getTime());
        auction.setDescription("This is a Test Auction");
        auction.setTitle("testAuction");
        auction.setUnitCount((short) 1);

        return auction;
    }

    private Person populateTestPerson() {
        testPerson.setAlias("testAlias");
        testPerson.setGroup(Person.Group.USER);
        testPerson.setPasswordHash(Person.getHash("123".getBytes()));
        testPerson.getName().setFamily("testFamilyName");
        testPerson.getName().setGiven("testGivenName");
        testPerson.getAddress().setCity("testCity");
        testPerson.getContact().setEmail("testEmail@test.de");
        testPerson.getContact().setPhone("0123456789");
        return testPerson;
    }

    @Test
    public void testConstraints() {
        final Validator val = this.getEntityValidatorFactory().getValidator();
        auction = new Auction(populateTestPerson());
        createTestAuction();


        assertEquals(0, val.validate(auction).size());
        createTestAuction();

        auction.setAskingPrice(-1);
        assertEquals(1, val.validate(auction).size());
        createTestAuction();

        auction.setUnitCount((short) -1);
        assertEquals(1, val.validate(auction).size());
        createTestAuction();

        auction.setClosureTimestamp(auction.getCreationTimestamp());
        assertEquals(1, val.validate(auction).size());
        createTestAuction();

        auction.setDescription(null);
        assertEquals(1, val.validate(auction).size());
        createTestAuction();

        auction.setTitle(null);
        assertEquals(1, val.validate(auction).size());
        createTestAuction();

        auction.setTitle(this.generateString(257));
        assertEquals(1, val.validate(auction).size());
        createTestAuction();

        auction.setDescription(this.generateString(8190));
        assertEquals(1, val.validate(auction).size());
        createTestAuction();

    }

    @Test
    public void testLifeCycle() {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("broker");
        EntityManager em = emf.createEntityManager();

        // Write test
        try {

            em.getTransaction().begin();


            Person testPerson = populateTestPerson();
            em.persist(testPerson);
            em.getTransaction().commit();
            this.getWasteBasket().add(testPerson.getIdentity());
            em.clear();

            // Initital write test
            em.getTransaction().begin();
            this.auction = new Auction(testPerson);
            createTestAuction();
            em.persist(auction);
            em.getTransaction().commit();
            this.getWasteBasket().add(auction.getIdentity());
            long auctionIdentity = auction.getIdentity();
            em.clear();

            em.getTransaction().begin();
            auction = em.find(Auction.class, auction.getIdentity());
            testPerson = em.find(Person.class, auction.getSellerReference());

            System.out.println("######################" + auction.getSellerReference() + "     " + testPerson.getIdentity());
            em.getTransaction().rollback();
            em.clear();


            // Update Test
            em.getTransaction().begin();
            auction = em.find(Auction.class, auctionIdentity);
            String oldTitle = auction.getTitle();
            auction.setTitle("new Title");
            this.getWasteBasket().add(auction.getIdentity());
            em.flush();
            String newTitle = em.find(Auction.class, auction.getIdentity()).getTitle();
            assertNotSame(oldTitle, newTitle);
            em.clear();

            // Delete Cascade Test
            long refPerson = em.find(Auction.class, auctionIdentity).getSellerReference();
            System.out.println(refPerson);
            em.remove(em.find(Auction.class, auctionIdentity));
            em.flush();
            assertNull(em.find(Person.class, refPerson));
            em.clear();


        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            this.emptyWasteBasket();
            em.close();
            emf.close();
        }


    }


}
