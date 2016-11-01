package de.sb.brokertest.model;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

public class BasicTest {
	public static void main(String[] args) {
		final EntityManager entityManager = Persistence.createEntityManagerFactory("broker").createEntityManager();
	}
}
