package de.sb.broker.model;

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
		try{	
				// Initials
				entityManager.getTransaction().begin();
				populateTestPerson();
				entityManager.persist(testBidder);
				entityManager.getTransaction().commit();
				this.getWasteBasket().add(testBidder.getIdentity());
				entityManager.clear();
				assertNotNull(testBidder);	//test create
				
				entityManager.getTransaction().begin();
				populateTestAuction();
				entityManager.persist(testAuction);
				entityManager.getTransaction().commit();
				this.getWasteBasket().add(testAuction.getIdentity());
				entityManager.clear();
				assertNotNull(testAuction);	//test create
			
				entityManager.getTransaction().begin();
				populateTestBid();
				entityManager.persist(testBid);
				entityManager.getTransaction().commit();
				this.getWasteBasket().add(testBid.getIdentity());
				entityManager.clear();
				assertNotNull(testBid);	//test create
				
				//mapped by Test
				entityManager.refresh(testBid);
				assertEquals(testAuction.getIdentity(), testBid.getAuctionReference());
				assertEquals(testBidder.getIdentity(), testBid.getBidderReference()); 
				
				//Inequal Test
				assertNotSame(testAuction.getSeller(), testBidder);
				assertNotSame(testAuction.getAskingPrice(), testBid.getPrice());
				
				//Foreign Key Test
				Bid bid1 = entityManager.find(Bid.class, testBid.getIdentity());
				assertEquals(testBid.getIdentity(), bid1.getIdentity());
				
	            // Update Test
				entityManager.getTransaction().begin();
	            testBid = entityManager.find(Bid.class, testBid.getIdentity());
	            long oldPrice = testBid.getPrice();
	            testBid.setPrice(567);
	            this.getWasteBasket().add(testBid.getIdentity());
	            entityManager.flush();
	            long newPrice = entityManager.find(Bid.class, testBid.getIdentity()).getPrice();
	            assertNotSame(oldPrice, newPrice);
	            entityManager.clear();
	            
	            // Delete Cascade Test
	            long refAuction = entityManager.find(Bid.class, testBid.getIdentity()).getAuctionReference();
	            long refPerson = entityManager.find(Bid.class, testBid.getIdentity()).getBidderReference();
	            entityManager.remove(entityManager.find(Bid.class, testBid.getIdentity()));
	            entityManager.flush();
	            assertNotNull(entityManager.find(Auction.class, refAuction));
	            assertNotNull(entityManager.find(Person.class, refPerson));
				
		}catch(Exception e){
			if(entityManager.getTransaction().isActive())entityManager.getTransaction().rollback();
		}finally{
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
