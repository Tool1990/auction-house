package de.sb.broker.model;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.Query;

public class BasicTest {
	public static void main(String[] args) {
		final EntityManager entityManager = Persistence.createEntityManagerFactory("broker").createEntityManager();
		entityManager.getTransaction().begin();
		Query query = entityManager.createQuery("select p from Person p");
		System.out.println(query.getResultList());
		entityManager.getTransaction().commit();
	}
}
