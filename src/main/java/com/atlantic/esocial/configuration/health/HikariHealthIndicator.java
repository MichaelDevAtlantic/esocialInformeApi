package com.atlantic.esocial.configuration.health;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import jakarta.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
/**
 ** @author  Tony Caravana Campos
 ** @apiNote Adicionando observabilidade 
 **/
@Component("hikariPoolHealthIndicator")
public class HikariHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private HttpServletRequest request;

    @Override
    public Health health() {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        Health.Builder healthBuilder = Health.up();

        // Coletando metricas
        int minConnections      = hikariDataSource.getMinimumIdle();
        int maxConnections      = hikariDataSource.getMaximumPoolSize();
        int idleConnections     = hikariDataSource.getHikariPoolMXBean().getIdleConnections(); //Available connections
        int borrowedConnections = hikariDataSource.getHikariPoolMXBean().getActiveConnections();

        // Adicionando metricas ao health status
        healthBuilder.withDetail("minPoolSize",          minConnections)
                     .withDetail("maxPoolSize",          maxConnections)
                     .withDetail("availableConnections", idleConnections)
                     .withDetail("borrowedConnections",  borrowedConnections);

        // Opcionalmnete verificando status da conexao para verificacoes mais detalhadas se o pool ainda esta dando conexoes
        if (request != null && request.getParameter("testConnection") != null && ((String)request.getParameter("testConnection")).equals("true")) {
            try (Connection connection = dataSource.getConnection()) {
                    
                if (request.getParameter("testSQL") != null && ((String)request.getParameter("testSQL")).equals("true")) {
                    healthBuilder.withDetail("connectionStatus", "UP");
                    if (!testDatabaseQuery(connection)) {
                        healthBuilder.down().withDetail("connectionQueryStatus", "DOWN");
                    } else {
                        healthBuilder.withDetail("connectionQueryStatus", "UP");
                    }
                } else {
                    healthBuilder.withDetail("connectionStatus", "UP");
                }

            } catch (Exception e) {
                healthBuilder.down().withDetail("connectionStatus", "DOWN");
            }
        }

        return healthBuilder.build();
    }

    private boolean testDatabaseQuery(Connection connection) {
        String sql = "SELECT 1 FROM DUAL"; // Uma query simples e leve para testar a conexao mais profundamente
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getInt(1) == 1; // Verifica se o resultado e o esperado
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // Retorna false se a query falhar
    }    
}
