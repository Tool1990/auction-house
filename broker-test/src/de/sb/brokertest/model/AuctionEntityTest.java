package de.sb.brokertest.model;


import de.sb.broker.model.Auction;
import de.sb.broker.model.Person;
import org.junit.After;
import org.junit.Test;
import org.junit.runners.Parameterized;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import static org.junit.Assert.assertEquals;


public class AuctionEntityTest extends EntityTest {


    Auction auction;
    Person testPerson = new Person();

    private Auction createTestAuction(){
        auction.setAskingPrice(200);
        Calendar cal = Calendar.getInstance();
        cal.set(2017,11,03);
        auction.setClosureTimestamp(cal.getTime().getTime());
        auction.setDescription("This is a Test Auction");
        auction.setTitle("testAuction");
        auction.setUnitCount((short) 1);

        return auction;
    }

    private Person populateTestPerson(){
        testPerson.setAlias("testAlias");
        testPerson.setGroup(Person.Group.USER);
        testPerson.setPasswordHash(Person.passwordHash("123"));
        testPerson.getName().setFamily("testFamilyName");
        testPerson.getName().setGiven("testGivenName");
        testPerson.getAddress().setCity("testCity");
        testPerson.getContact().setEmail("testEmail@test.de");
        testPerson.getContact().setPhone("0123456789");
        return testPerson;
    }

    @Test
    public void testConstraints(){
        Validator val = this.getEntityValidatorFactory().getValidator();
        auction = new Auction(populateTestPerson());
        Set<ConstraintViolation<Auction>> constraintViolations;
        createTestAuction();

        constraintViolations = val.validate(auction);
        assertEquals(0, constraintViolations.size());
        createTestAuction();

        auction.setAskingPrice(-1);
        constraintViolations = val.validate(auction);
        assertEquals(1, constraintViolations.size());
        createTestAuction();

        auction.setUnitCount((short) -1);
        constraintViolations = val.validate(auction);
        assertEquals(1, constraintViolations.size());
        createTestAuction();

        auction.setClosureTimestamp(auction.getCreationTimestamp());
        constraintViolations = val.validate(auction);
        assertEquals(1, constraintViolations.size());
        createTestAuction();

        auction.setDescription(null);
        constraintViolations = val.validate(auction);
        assertEquals(1, constraintViolations.size());
        createTestAuction();

        auction.setTitle(null);
        constraintViolations = val.validate(auction);
        assertEquals(1, constraintViolations.size());
        createTestAuction();

    }

    @Test
    public void testLifeCycle(){

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("broker");
        EntityManager em = emf.createEntityManager();


        try{

        em.getTransaction().begin();

         final Person testPerson = populateTestPerson();
         em.persist(testPerson);
         em.getTransaction().commit();


         em.getTransaction().begin();

         this.auction = new Auction(testPerson);
         createTestAuction();
         em.persist(auction);
         em.getTransaction().commit();



         //add created entity to waste basket
         this.getWasteBasket().add(auction.getIdentity());
         this.getWasteBasket().add(testPerson.getIdentity());
         //empty waste basket
         this.emptyWasteBasket();
        }

        finally{
            if(em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            //close Manager
            em.close();
            emf.close();
        }

    }


}
