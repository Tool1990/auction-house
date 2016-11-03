package de.sb.brokertest.model;

import de.sb.broker.model.Auction;
import de.sb.broker.model.Person;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class PersonEntityTest extends EntityTest {
	Person testPerson = new Person();

	@Test
	public void testConstraints() {
		Validator validator = this.getEntityValidatorFactory().getValidator();
		Person testPerson = populateTestPerson();
		Set<ConstraintViolation<Person>> constraintViolations;

		constraintViolations = validator.validate(testPerson);
		assertEquals(0, constraintViolations.size());

		testPerson.setAlias(null);
		constraintViolations = validator.validate(testPerson);
		assertEquals(1, constraintViolations.size());
		populateTestPerson();

		testPerson.setAlias("");
		constraintViolations = validator.validate(testPerson);
		assertEquals(1, constraintViolations.size());
		populateTestPerson();

		testPerson.setAlias(this.generateString(17));
		constraintViolations = validator.validate(testPerson);
		assertEquals(1, constraintViolations.size());
		populateTestPerson();

		testPerson.getName().setFamily(null);
		testPerson.getName().setGiven(null);
		constraintViolations = validator.validate(testPerson);
		assertEquals(2, constraintViolations.size());
		populateTestPerson();

		testPerson.getContact().setPhone(null);
		testPerson.getContact().setEmail("test@test.t");
		constraintViolations = validator.validate(testPerson);
		assertEquals(2, constraintViolations.size());
		populateTestPerson();

		testPerson.getAddress().setCity(null);
		constraintViolations = validator.validate(testPerson);
		assertEquals(1, constraintViolations.size());
		populateTestPerson();

		testPerson.setGroup(null);
		constraintViolations = validator.validate(testPerson);
		assertEquals(1, constraintViolations.size());
		populateTestPerson();
	}

	@Test
	public void testLifeCycle() {
		final EntityManager entityManager = this.getEntityManagerFactory().createEntityManager();

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

			entityManager.getTransaction().begin();
			entityManager.persist(testAuction);
			entityManager.getTransaction().commit();
			//this.getWasteBasket().add(testAuction.getIdentity());

			entityManager.refresh(testPerson);
			assertEquals(1, testPerson.getAuctions().size());
		} catch (Exception e) {
			entityManager.getTransaction().rollback();
			e.printStackTrace();
		} finally {
			entityManager.close();
		}
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
