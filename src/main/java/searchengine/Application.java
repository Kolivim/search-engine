package searchengine;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.Driver;
import java.util.Properties;

@EnableTransactionManagement
@SpringBootApplication
//@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class Application
{
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    //Вар.1.1
//     ЗАПУСТИЛОСЬ НО ВЫЛЕТАЕТ С ОШИБКОЙ В ПРОЦЕССЕ
//    @Bean
//    public DataSource dataSource(){
//        DriverManagerDataSource dataSource = new DriverManagerDataSource();
//        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver"); // "com.mysql.cj.jdbc.Driver"
//        dataSource.setUrl("jdbc:mysql://localhost:3306/search_engine");
//        dataSource.setUsername("root");
//        dataSource.setPassword("171605mEi");
//        dataSource.setSchema("search_engine");
//        return dataSource;
//    }

    //Вар.1.2 Работает
//    @Bean
//    public DataSource dataSource()
//    {
//        return DataSourceBuilder.create()
//                .driverClassName("com.mysql.cj.jdbc.Driver")
//                .url("jdbc:mysql://localhost:3306/search_engine")
//                .username("root")
//                .password("171605mEi")
//                .build();
//    }
//
//    @Bean
//    public PlatformTransactionManager transactionManager(){
//        return new DataSourceTransactionManager(dataSource());
//    }
    //


//    private final Properties hibernateProperties() {
//        final Properties hibernateProperties = new Properties();
//        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", env.getProperty("hibernate.hbm2ddl.auto"));
//        hibernateProperties.setProperty("hibernate.dialect", env.getProperty("hibernate.dialect"));
//
//        hibernateProperties.setProperty("hibernate.show_sql", "true");
//        hibernateProperties.setProperty("hibernate.format_sql", "true");
//        // hibernateProperties.setProperty("hibernate.globally_quoted_identifiers", "true");
//        hibernateProperties.setProperty("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
//
//        // Envers properties
//        hibernateProperties.setProperty("org.hibernate.envers.audit_table_suffix", env.getProperty("envers.audit_table_suffix")); // TODO: Really need this?
//
//        return hibernateProperties;
//    }

//    @Bean
//    public LocalSessionFactoryBean sessionFactory() {
//        final LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
//        sessionFactory.setDataSource(dataSource());
//        sessionFactory.setPackagesToScan(new String[] { "com.hibernate.query.performance.persistence.model" });
//        sessionFactory.setHibernateProperties(hibernateProperties());
//
//        return sessionFactory;
//    }

//    @Bean
//    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
//        final LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
//        emf.setDataSource(dataSource());
//        emf.setPackagesToScan(new String[] { "com.hibernate.query.performance.persistence.model" });
//
//        final JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
//        emf.setJpaVendorAdapter(vendorAdapter);
//        emf.setJpaProperties(hibernateProperties());
//
//        return emf;
//    }



//    @Bean
//    public PlatformTransactionManager hibernateTransactionManager() { // TODO: Really need this?
//        final HibernateTransactionManager transactionManager = new HibernateTransactionManager();
//        transactionManager.setSessionFactory(sessionFactory().getObject());
//        return transactionManager;
//    }

//    @Bean
//    public PlatformTransactionManager jpaTransactionManager() { // TODO: Really need this?
//        final JpaTransactionManager transactionManager = new JpaTransactionManager(); // http://stackoverflow.com/questions/26562787/hibernateexception-couldnt-obtain-transaction-synchronized-session-for-current
//        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
//        return transactionManager;
//    }





//    @Bean
//    @ConfigurationProperties("app.spring.datasource")
//    public DataSource dataSource() {
//        return DataSourceBuilder.create().build();
//    }

    /*
    //Вар. 2
    @Autowired
    @Bean(name = "sessionFactory")
    public SessionFactory getSessionFactory(DataSource dataSource) {
        LocalSessionFactoryBuilder sessionBuilder = new LocalSessionFactoryBuilder(dataSource);
        sessionBuilder.addProperties(getHibernateProperties());
        sessionBuilder.addAnnotatedClasses(UploadFile.class);
        return sessionBuilder.buildSessionFactory();
    }

    @Autowired
    @Bean(name = "transactionManager")
    public HibernateTransactionManager getTransactionManager(
            SessionFactory sessionFactory) {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager(
                sessionFactory);
        return transactionManager;
    }
    */



    /*
    // Вар.3
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/test?serverTimezone=UTC");
        dataSource.setUsername( "username" );
        dataSource.setPassword( "password" );
        return dataSource;
    }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory emf) {
      return new JpaTransactionManager(emf);
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
      HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
      jpaVendorAdapter.setDatabase(Database.MYSQL);
      jpaVendorAdapter.setGenerateDdl(true);
      return jpaVendorAdapter;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
      LocalContainerEntityManagerFactoryBean lemfb = new LocalContainerEntityManagerFactoryBean();
      lemfb.setDataSource(dataSource());
      lemfb.setJpaVendorAdapter(jpaVendorAdapter());
      lemfb.setPackagesToScan("packages.containing.entity.classes");
      return lemfb;
    }
     */



//    @Autowired
////    @PersistenceContext
//    EntityManagerFactory entityManagerFactory;


//    @Bean
//    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
//        //...
//    }

//    @Bean
//    public PlatformTransactionManager transactionManager() {
//        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("com.baeldung.movie_catalog");
//        JpaTransactionManager transactionManager = new JpaTransactionManager();
//        transactionManager.setEntityManagerFactory(entityManagerFactory); // transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
//        return transactionManager;
//    }


//    @Bean
//    public PlatformTransactionManager txManager(DataSource dataSource) {return new DataSourceTransactionManager(dataSource);}

    /*
    @Bean
    public DataSource dataSource() {

        return new SimpleDriverDataSource() {
            {
                setDriverClass(com.mysql.cj.jdbc.Driver);
                setUsername("root");
                setUrl("jdbc:mysql://localhost:3306/search_engine?useSSL=false&requireSSL=false&allowPublicKeyRetrieval=true");
                setPassword("171605mEi");
            }
        };

//        return new MysqlDataSource(); // configure and return the necessary JDBC DataSource
    }
    */






//    @Bean
//    public DataSource getDataSource()
//    {
//        DataSourceBuilder dsBuilder = DataSourceBuilder.create ();
//        dsBuilder.driverClassName ("com.mysql.cj.jdbc.Driver");
//        dsBuilder.url ("jdbc:mysql://localhost:3306/search_engine?useSSL=false&requireSSL=false&allowPublicKeyRetrieval=true");
//        dsBuilder.username ("root");
//        dsBuilder.password ("171605mEi");
//        return (DataSource) dsBuilder.build ();
//    }

    /*
    @Bean
    public PlatformTransactionManager txManager() {
        DataSourceBuilder dsBuilder = DataSourceBuilder.create ();
        dsBuilder.driverClassName ("com.mysql.cj.jdbc.Driver");
        dsBuilder.url ("jdbc:mysql://localhost:3306/search_engine?useSSL=false&requireSSL=false&allowPublicKeyRetrieval=true");
        dsBuilder.username ("root");
        dsBuilder.password ("171605mEi");
        return new DataSourceTransactionManager(dsBuilder.build());
    }
    */

    // Может рабочий??? 25.04.23
    /*
    @Bean//(name = "entityManagerFactoryT")
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }
    */

    /*
    @Bean
    DataSource dataSource() {
        return new SimpleDriverDataSource()
        {
            {
                try {
                    setDriverClass((Class<? extends Driver>) Class.forName(" com.mysql.cj.jdbc.Driver"));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                setUsername("root");
                setUrl("jdbc:mysql://localhost:3306/search_engine");
                setPassword("171605mEi");
            }
        };
    }
    */




    /*
    @Autowired
    private DataSource dataSource;
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean factory() {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan(
                new String[] {"your.package"});
        factory.setJpaVendorAdapter(
                new HibernateJpaVendorAdapter());
        return factory;
    }
    */

    /*
    @Autowired
    private DataSource dataSource;

    @Bean(name = "transactionManagerT")
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager tm =
                new JpaTransactionManager();
        tm.setEntityManagerFactory(entityManagerFactory);
        tm.setDataSource(dataSource);
        return tm;
    }
    */

}



