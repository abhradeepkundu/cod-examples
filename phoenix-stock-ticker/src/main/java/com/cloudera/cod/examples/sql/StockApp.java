// Copyright (c) 2020 Cloudera, Inc. All rights reserved.
package com.cloudera.cod.examples.sql;

import java.sql.SQLException;

import com.cloudera.cod.examples.sql.cli.CreateTablesCommand;
import com.cloudera.cod.examples.sql.cli.DeleteTablesCommand;
import com.cloudera.cod.examples.sql.cli.WriteAllTickerValuesCommand;
import com.cloudera.cod.examples.sql.health.CompanyHealthCheck;
import com.cloudera.cod.examples.sql.resources.CompanyResource;
import com.cloudera.cod.examples.sql.resources.CompanyValueResource;

import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import io.dropwizard.Application;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

public class StockApp extends Application<StockAppConfiguration> {

  public static void main(final String[] args) throws Exception {
    new StockApp().run(args);
  }

  @Override
  public String getName() {
    return "COD StockApp";
  }

  @Override
  public void initialize(final Bootstrap<StockAppConfiguration> bootstrap) {
    bootstrap.addCommand(new CreateTablesCommand());
    bootstrap.addCommand(new DeleteTablesCommand());
    bootstrap.addCommand(new WriteAllTickerValuesCommand());
    // Environment-substitution in configuration
    bootstrap.addBundle(new TemplateConfigBundle());
    // UI templating
    bootstrap.addBundle(new ViewBundle<StockAppConfiguration>());
  }

  @Override
  public void run(final StockAppConfiguration configuration, final Environment environment) {
    try {
      configuration.createConnection();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    environment.lifecycle().manage(new Managed() {
      @Override
      public void start() {}

      @Override
      public void stop() {
        try {
          configuration.closeConnection();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }
    });

    final DatabaseConfiguration databaseConf = configuration.getDatabaseConfiguration();
    final CompanyResource resource = new CompanyResource(configuration);
    environment.jersey().register(resource);
    final CompanyValueResource valueResource = new CompanyValueResource(configuration);
    environment.jersey().register(valueResource);

    final CompanyHealthCheck healthCheck = new CompanyHealthCheck(
        configuration.buildJdbcUrlWithAuthentication());
    environment.healthChecks().register("company", healthCheck);
  }

}
