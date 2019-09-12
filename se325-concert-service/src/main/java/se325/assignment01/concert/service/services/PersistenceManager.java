package se325.assignment01.concert.service.services;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Singleton class that manages an EntityManagerFactory. When a
 * PersistenceManager is instantiated, it creates an EntityManagerFactory. An
 * EntityManagerFactory is required to create an EntityManager, which represents
 * a persistence context (session with a database).
 * <p>
 * When a Web service application component (e.g. a resource object) requires a
 * persistence context, it should call the PersistentManager's
 * createEntityManager() method to acquire one.
 * <p>
 * This class is complete - you do not need to modify it.
 */
public class PersistenceManager {
    private static PersistenceManager _instance = null;

    private EntityManagerFactory entityManagerFactory;

    protected PersistenceManager() {
        entityManagerFactory = Persistence.createEntityManagerFactory("se325.assignment01.concert");
    }

    public EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    public static PersistenceManager instance() {
        if (_instance == null) {
            _instance = new PersistenceManager();
        }
        return _instance;
    }

    // FOR TESTING ONLY! Will wipe the database.
    public void reset() {
        entityManagerFactory.close();
        entityManagerFactory = Persistence.createEntityManagerFactory("se325.assignment01.concert");
    }

}
