package com.valspar.interfaces.common.hibernate;

import com.valspar.interfaces.common.enums.DataSource;
import java.util.*;
import javax.persistence.Entity;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.reflections.Reflections;

public class HibernateUtil
{
  private static Map<String, SessionFactory> sessionFactoryMap = new HashMap<String, SessionFactory>();
  private static Logger log4jLogger = Logger.getLogger(HibernateUtil.class);
  private static Set<Class<?>> entityClasses;

  public HibernateUtil()
  {
  }

  public static void initialize()
  {
    Reflections reflections = new Reflections("com.valspar.interfaces");
    entityClasses = reflections.getTypesAnnotatedWith(Entity.class);
  }

  public static Session getHibernateSession(DataSource dataSource)
  {
    Session session = null;

    try
    {
      if (sessionFactoryMap.get(dataSource.getHibernateConfigLocation()) == null)
      {
        Configuration configuration = new Configuration();
        configuration.configure("/com/valspar/interfaces/common/hibernate/" + dataSource.getHibernateConfigLocation());
        
        for (Class clazz: entityClasses)
        {
          configuration.addPackage(clazz.getPackage().getName());
          configuration.addAnnotatedClass(clazz);
        }
        
        ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
        SessionFactory sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        sessionFactoryMap.put(dataSource.getHibernateConfigLocation(), configuration.buildSessionFactory(serviceRegistry));
        session = sessionFactory.openSession();
      }
      else
      {
        session = sessionFactoryMap.get(dataSource.getHibernateConfigLocation()).openSession();
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return session;
  }
  
  public static Session getHibernateSessionAndBeginTransaction(DataSource dataSource)
  {
    Session session = null;
    try
    {
      session = getHibernateSession(dataSource);
      session.beginTransaction();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return session;
  }
  
  public static void closeHibernateSession(Session session)
  {
    if (session != null)
    {
      try
      {
        session.close();
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
    }
  }

  public static void closeHibernateSessionAndCommitTransaction(Session session)
  {
    if (session != null)
    {
      try
      {
        session.getTransaction().commit();
      }
      catch (Exception e)
      {
        log4jLogger.error(e);
      }
      finally
      {
        closeHibernateSession(session);
      }
    }
  }
}