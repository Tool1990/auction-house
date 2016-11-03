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
	Auction testAuction;
	Bid testBid;
	
	@Test
	public void testConstraints() {
		populateTestPerson();
		populateTestPerson2();
		populateTestAuction();
		populateTestBid();
		
		Validator validator = this.getEntityValidatorFactory().getValidator();
		Set<ConstraintViolation<Bid>> constraintViolations;
		
		//Error: During synchronization a new object was found through a relationship that was not marked cascade PERSIST: de.sb.broker.model.Person@796065aa.
		
		constraintViolations = validator.validate(testBid);
		assertEquals(1, constraintViolations.size());
		
		testBid.setPrice(10);	
		constraintViolations = validator.validate(testBid);
		assertEquals(2, constraintViolations.size());
		populateTestBid();
		
		testBid.setPrice(10000000);
		constraintViolations = validator.validate(testBid);
		assertEquals(1, constraintViolations.size());
		populateTestBid();
		
		testBid.setPrice(-1);
		constraintViolations = validator.validate(testBid);
		assertEquals(3, constraintViolations.size());
		populateTestBid();
	}
	
	@Test
	public void testLifeCycle1(){
		EntityManager entityManager = this.getEntityManagerFactory().createEntityManager();
		try{	//Test-Case 1
				entityManager.getTransaction().begin();
				populateTestPerson();
				entityManager.persist(testBidder);
				entityManager.getTransaction().commit();
				this.getWasteBasket().add(testBidder.getIdentity());
				assertNotNull(testBidder);		//test create
				
				entityManager.getTransaction().begin();
				populateTestAuction();
				entityManager.persist(testAuction);
				entityManager.getTransaction().commit();
				this.getWasteBasket().add(testAuction.getIdentity());
				assertNotNull(testAuction);		//test create
			
				entityManager.getTransaction().begin();
				populateTestBid();
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
	
	public Person populateTestPerson() {
		this.testSeller.setAlias("testAlias");
		this.testSeller.setGroup(Person.Group.USER);
		this.testSeller.setPasswordHash(Person.passwordHash("123"));
		this.testSeller.getName().setFamily("testFamilyName");
		this.testSeller.getName().setGiven("testGivenName");
		this.testSeller.getAddress().setCity("testCity");
		this.testSeller.getContact().setEmail("testEmail@test.de");
		this.testSeller.getContact().setPhone("0123456789");
		return testSeller;
	}

	public Person populateTestPerson2() {
		this.testBidder.setAlias("testAlias2");
		this.testBidder.setGroup(Person.Group.USER);
		this.testBidder.setPasswordHash(Person.passwordHash("1234"));
		this.testBidder.getName().setFamily("testFamilyName2");
		this.testBidder.getName().setGiven("testGivenName2");
		this.testBidder.getAddress().setCity("testCity2");
		this.testBidder.getContact().setEmail("testEmail2@test.de");
		this.testBidder.getContact().setPhone("01234567892");
		return testBidder;
	}
	
	public Auction populateTestAuction() {
		this.testAuction = new Auction(testBidder);
		this.testAuction.setAskingPrice(100);
		this.testAuction.setDescription("test");
		this.testAuction.setTitle("testAuction");
		this.testAuction.setUnitCount((short) 1);
		Calendar cal = Calendar.getInstance();
        cal.set(2017,11,03);
        testAuction.setClosureTimestamp(cal.getTime().getTime());
		//testAuction.setClosureTimestamp(System.currentTimeMillis());
		return testAuction;
	}
	
	public Bid populateTestBid(){
		this.testBid= new Bid(populateTestAuction(), populateTestPerson());
		testBid.setPrice(200);
		return testBid;
	}
}
