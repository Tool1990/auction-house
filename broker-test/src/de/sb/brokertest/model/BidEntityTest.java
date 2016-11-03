package de.sb.brokertest.model;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Bid;
import de.sb.broker.model.Person;

import org.junit.Test;

import java.util.Calendar;
import java.util.Set;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import static org.junit.Assert.*;		

public class BidEntityTest extends EntityTest {
	
	Person testSeller = new Person();
	Person testBidder = new Person();
	Auction testAuction = new Auction(testSeller);
	Bid testBid = new Bid(testAuction, testBidder);
	
	@Test
	public void testConstraints() {
		populateTestPerson(testSeller);
		populateTestPerson(testBidder);
		populateTestAuction(testAuction);
		populateTestBid(testBid);
		
		Validator validator = this.getEntityValidatorFactory().getValidator();
		Set<ConstraintViolation<Bid>> constraintViolations;
		
		//Error: During synchronization a new object was found through a relationship that was not marked cascade PERSIST: de.sb.broker.model.Person@796065aa.
		
		constraintViolations = validator.validate(testBid);
		assertEquals(0, constraintViolations.size());
		
		testBid.setPrice(10);	
		constraintViolations = validator.validate(testBid);
		assertEquals(0, constraintViolations.size());
		populateTestBid(testBid);
		
		testBid.setPrice(0);
		constraintViolations = validator.validate(testBid);
		assertEquals(0, constraintViolations.size());
		populateTestBid(testBid);
		
		testBid.setPrice(-1);
		constraintViolations = validator.validate(testBid);
		assertEquals(1, constraintViolations.size());
		populateTestBid(testBid);
	}
	
	@Test
	public void testLifeCycle1(){
		EntityManager entityManager = this.getEntityManagerFactory().createEntityManager();
		try{	//Test-Case 1
				entityManager.getTransaction().begin();
				testBidder = populateTestPerson(testBidder);
				entityManager.persist(testBidder);
				entityManager.getTransaction().commit();
				this.getWasteBasket().add(testBidder.getIdentity());
				assertNotNull(testBidder);		//test create
				
				entityManager.getTransaction().begin();
				testAuction = populateTestAuction(testAuction);
				entityManager.persist(testAuction);
				entityManager.getTransaction().commit();
				this.getWasteBasket().add(testAuction.getIdentity());
				assertNotNull(testAuction);		//test create
			
				entityManager.getTransaction().begin();
				testBid = populateTestBid(testBid);
				entityManager.persist(testBid);
				entityManager.getTransaction().commit();
				this.getWasteBasket().add(testBid.getIdentity());
				
				assertNotNull(testBid);		//test create
				
				Bid bid1 = entityManager.find(Bid.class, testBid.getIdentity());
				assertEquals(testBid.getIdentity(), bid1.getIdentity());	//test key	
				
		}catch(Exception e){
			if(entityManager.getTransaction().isActive())entityManager.getTransaction().rollback();
		}finally{
			//assertNull(testBid);		//test delete
			entityManager.close();
		}
	}
	
	static Person populateTestPerson(Person testPerson) {
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
	
	static Auction populateTestAuction(Auction testAuction) {
		testAuction.setAskingPrice(0);
		testAuction.setDescription("test");
		testAuction.setTitle("testAuction");
		testAuction.setUnitCount((short) 1);
		Calendar cal = Calendar.getInstance();
        cal.set(2017,11,03);
        testAuction.setClosureTimestamp(cal.getTime().getTime());
		//testAuction.setClosureTimestamp(System.currentTimeMillis());
		return testAuction;
	}
	
	static Bid populateTestBid(Bid testBid){
		testBid.setPrice(1);
		return testBid;
	}
}
