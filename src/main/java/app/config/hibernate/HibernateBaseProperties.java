package app.config.hibernate;

import java.util.Properties;

public final class HibernateBaseProperties
{

    private HibernateBaseProperties()
    {
    }

    public static Properties createBase()
    {
        Properties props = new Properties();
        props.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        props.put("hibernate.current_session_context_class", "thread");
        props.put("hibernate.show_sql", "false");
        props.put("hibernate.format_sql", "false");
        props.put("hibernate.use_sql_comments", "false");
        props.put("hibernate.hikari.maximumPoolSize", "10");
        props.put("hibernate.hikari.minimumIdle", "2");
        props.put("hibernate.hikari.connectionTimeout", "20000");
        return props;
    }
}