/*
 * Copyright 2021 Cloudera, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.cod.examples.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class StockAppConfiguration extends Configuration {
  private static final Logger LOG = LoggerFactory.getLogger(StockAppConfiguration.class);

  private AtomicReference<Connection> conn = new AtomicReference<>();

  @Valid
  @NotNull
  private DatabaseConfiguration databaseConf = new DatabaseConfiguration();

  @Valid
  @NotNull
  private CredentialsConfiguration credentialsConf = new CredentialsConfiguration();

  @JsonProperty("database")
  public DatabaseConfiguration getDatabaseConfiguration() {
    return databaseConf;
  }

  @JsonProperty("database")
  public void setDatabaseConfiguration(DatabaseConfiguration conf) {
    this.databaseConf = conf;
  }

  @JsonProperty("credentials")
  public CredentialsConfiguration getCredentialsConfiguration() {
    return credentialsConf;
  }

  @JsonProperty("credentials")
  public void setCredentialsConfiguration(CredentialsConfiguration conf) {
    this.credentialsConf = conf;
  }

  public String buildJdbcUrlWithAuthentication() {
    String baseUrl = databaseConf.getJdbcUrl();
    StringJoiner joiner = new StringJoiner(";");
    joiner.add(baseUrl).add("avatica_user=" + credentialsConf.getUsername())
        .add("avatica_password=" + credentialsConf.getPassword());
    return joiner.toString();
  }

  public Connection getConnection() {
    Connection c = conn.get();
    if (c == null) {
      throw new IllegalStateException("Call createConnection() first");
    }
    return c;
  }

  public synchronized void closeConnection() throws SQLException {
    Connection toClose = conn.getAndSet(null);
    if (toClose != null) {
      toClose.close();
    }
  }

  public synchronized void createConnection() throws SQLException {
    if (conn.get() != null) {
      return;
    }
    String url = buildJdbcUrlWithAuthentication();
    LOG.debug("Connecting to {}", url);
    conn.set(DriverManager.getConnection(url));
  }
}
