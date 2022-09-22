package ubc.pavlab.rdp.util;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.SessionFactoryImpl;

import javax.persistence.EntityManager;

public class HibernateUtils {

    public static Dialect getDialect( EntityManager em ) {
        Session session = (Session) em.getDelegate();
        SessionFactoryImpl sessionFactory = (SessionFactoryImpl) session.getSessionFactory();
        return sessionFactory.getJdbcServices().getDialect();
    }
}
