package de.sb.broker.model;

import org.junit.Test;

import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.validation.Validator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class PersonEntityTest extends EntityTest {
	Person testPerson = new Person();

	@Test
	public void testConstraints() {
		final Validator validator = this.getEntityValidatorFactory().getValidator();
		Person testPerson = populateTestPerson();
		Document avatar = getTestAvatar();

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

		testPerson.setAvatar(avatar);
		assertEquals(0, validator.validate(testPerson).size());
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
		testAuction.setClosureTimestamp(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 14);
		testAuction.setDescription("testDescription");

		Document avatar = getTestAvatar();

		try {

			//initial write test
			entityManager.getTransaction().begin();
			entityManager.persist(testPerson);
			entityManager.getTransaction().commit();
			this.getWasteBasket().add(testPerson.getIdentity());
			long personIdentity = testPerson.getIdentity();
			entityManager.clear();

			entityManager.getTransaction().begin();
			entityManager.persist(testAuction);
			entityManager.getTransaction().commit();
			this.getWasteBasket().add(testAuction.getIdentity());
			long auctionIdentity = testAuction.getIdentity();
			entityManager.clear();

			entityManager.getTransaction().begin();
			entityManager.persist(avatar);
			entityManager.merge(testPerson);
			entityManager.getTransaction().commit();
			this.getWasteBasket().add(avatar.getIdentity());
			entityManager.clear();

			entityManager.getTransaction().begin();
			testPerson.setAvatar(avatar);
			entityManager.merge(testPerson);
			entityManager.getTransaction().commit();
			System.out.println("######################" + testPerson.getDocumentReference() + "     " + avatar.getIdentity());
			assertEquals(testPerson.getDocumentReference(), avatar.getIdentity());
			entityManager.clear();

			entityManager.getTransaction().begin();
			testPerson = entityManager.find(Person.class, personIdentity);

			avatar = entityManager.find(Document.class, testPerson.getDocumentReference());
			System.out.println("######################" + testPerson.getDocumentReference() + "     " + avatar.getIdentity());
			entityManager.getTransaction().rollback();
			entityManager.clear();

			//Update Test
			entityManager.getTransaction().begin();
			testPerson = entityManager.find(Person.class, personIdentity);
			String oldAlias = testPerson.getAlias();
			testPerson.setAlias("new Alias");
			this.getWasteBasket().add(testPerson.getIdentity());
			entityManager.getTransaction().commit();
            entityManager.getTransaction().begin();
			String newAlias = entityManager.find(Person.class, testPerson.getIdentity()).getAlias();
			assertNotSame(oldAlias, newAlias);
			entityManager.clear();

			// Delete Cascade Test
			long refAuction = entityManager.find(Auction.class, auctionIdentity).getSellerReference();
			entityManager.remove(entityManager.find(Person.class, personIdentity));
			assertNull(entityManager.find(Auction.class, refAuction));
			entityManager.clear();
		} catch (Exception e) {
			if (entityManager.getTransaction().isActive())
				entityManager.getTransaction().rollback();
			throw e;
		} finally {
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			this.emptyWasteBasket();
			entityManager.close();
		}

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

	public Document getTestAvatar() {
		Document avatar = new Document();
		avatar.setType("png");

		BufferedImage originalImage = null;
		try {
			originalImage = ImageIO.read(new File("src/META-INF/klein.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// convert BufferedImage to byte array
		byte[] imageInByte = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(originalImage, "png", baos);
			baos.flush();
			imageInByte = baos.toByteArray();
			baos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		avatar.setContent(imageInByte);
		avatar.setHash(Document.getHash(avatar.getContent()));
		return avatar;
	}
}
