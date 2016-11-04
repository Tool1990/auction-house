package de.sb.broker.model;

import org.junit.Test;

import javax.persistence.EntityManager;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PersonEntityTest extends EntityTest {
	Person testPerson = new Person();

	@Test
	public void testConstraints() {
		final Validator validator = this.getEntityValidatorFactory().getValidator();
		Person testPerson = populateTestPerson();

		assertEquals(0, validator.validate(testPerson).size());

		testPerson.setAlias(null);
		assertEquals(1, validator.validate(testPerson).size());
		populateTestPerson();

		testPerson.setAlias("");
		assertEquals(1, validator.validate(testPerson).size());
		populateTestPerson();

		testPerson.setAlias(this.generateString(17));
		assertEquals(1, validator.validate(testPerson).size());
		populateTestPerson();

		testPerson.getName().setFamily(null);
		testPerson.getName().setGiven(null);
		assertEquals(2, validator.validate(testPerson).size());
		populateTestPerson();

		testPerson.getContact().setPhone(null);
		testPerson.getContact().setEmail("test@test.t");
		assertEquals(2, validator.validate(testPerson).size());
		populateTestPerson();

		testPerson.getAddress().setCity(null);
		assertEquals(1, validator.validate(testPerson).size());
		populateTestPerson();

		testPerson.setGroup(null);
		assertEquals(1, validator.validate(testPerson).size());
		populateTestPerson();
	}

	@Test
	public void testLifeCycle() {
		EntityManager entityManager = this.getEntityManagerFactory().createEntityManager();

		Person testPerson = populateTestPerson();

		Auction testAuction = new Auction(testPerson);
		testAuction.setTitle("testAuction");
		testAuction.setUnitCount((short) 1);
		testAuction.setAskingPrice(100);
		testAuction.setClosureTimestamp(System.currentTimeMillis() + 1000*60*60*24*14);
		testAuction.setDescription("testDescription");

		try {
			entityManager.getTransaction().begin();
			entityManager.persist(testPerson);
			entityManager.getTransaction().commit();
			this.getWasteBasket().add(testPerson.getIdentity());
			entityManager.clear();

			entityManager.getTransaction().begin();
			entityManager.persist(testAuction);
			entityManager.getTransaction().commit();
			this.getWasteBasket().add(testAuction.getIdentity());
		} catch (Exception e) {
			entityManager.getTransaction().rollback();
			throw e;
		} finally {
			entityManager.close();
		}

		entityManager.clear();
		testAuction = entityManager.find(Auction.class, testAuction.getIdentity());
		assertNotNull(testAuction);



		entityManager = this.getEntityManagerFactory().createEntityManager();
		boolean exceptionMode = false;

		//update, clear, find, löschen, find

		try {
			entityManager.getTransaction().begin();
			entityManager.persist(testAuction);
			entityManager.getTransaction().commit();

			entityManager.refresh(testPerson);
			assertEquals(1, testPerson.getAuctions().size());
		} catch (Exception e) {
			exceptionMode = true;
			entityManager.getTransaction().rollback();
		} finally {
			entityManager.close();
		}

		assertTrue(exceptionMode);
	}

	private Person populateTestPerson() {
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
}
