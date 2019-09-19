package se.redbridge.codeone;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.backends.BackendRegistries;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PersonVerticle extends AbstractVerticle {
  private static final Logger LOG = Logger.getLogger(PersonVerticle.class.getName());

  private static final String CREATE_TABLE_SQL = "create table PERSON(ID int primary key, FIRST_NAME varchar, LAST_NAME varchar);";
  private static final String INSERT_SQL = "INSERT INTO person(id, first_name, last_name) VALUES (?, ?, ?);";
  private static final String UPDATE_SQL = "UPDATE person SET first_name = ?, last_name = ? WHERE id = ?;";
  private static final String SELECT_ALL_SQL = "SELECT id, first_name, last_name FROM person;";
  private static final String SELECT_ONE_SQL = "SELECT id, first_name, last_name FROM person WHERE id = ?";
  private static final String DELETE_ALL_SQL = "DELETE FROM person";
  private static final String DELETE_ONE_SQL = "DELETE FROM person WHERE id = ?";

  private static final String PARAM_PERSON_ID = "personId";

  private static final String ROUTE_ALL = "/persons";
  private static final String ROUTE_ONE = "/persons/:personId";
  private static final String ROUTE_HEALTH = "/health";
  private static final String ROUTE_METRICS = "/metrics";

  private final AtomicLong numPeepz = new AtomicLong(0L);

  private JDBCClient jdbcClient;

  @Override
  public void start(final Promise<Void> startPromise) throws Exception {
    jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", "jdbc:h2:mem:persons;DB_CLOSE_ON_EXIT=TRUE")
      .put("driver_class", "org.h2.Driver")
      .put("max_pool_size", 30));

    jdbcClient.getConnection(cn -> {
      cn.result().execute(CREATE_TABLE_SQL, res -> {
        if (res.failed()) {
          throw new IllegalStateException("Could not create database tables. Bailing out!", res.cause());
        }
      });
    });

    final var healthCheckHandler = HealthCheckHandler.create(vertx);
    healthCheckHandler.register("databaseOk", future -> {
      jdbcClient.getConnection(cn -> {
        if (cn.succeeded()) {
          future.complete(Status.OK());
        } else {
          future.complete(Status.KO());
        }
      });
    });

    MeterRegistry registry = BackendRegistries.getDefaultNow();
    Gauge.builder("numPersons", this::numPeepz)
      .description("How many persons do we have in our database?")
      .register(registry);

    final var router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.get(ROUTE_HEALTH).handler(healthCheckHandler);
    router.route(ROUTE_METRICS).handler(PrometheusScrapingHandler.create());

    router.get(ROUTE_ALL).handler(this::handleListAll);
    router.get(ROUTE_ONE).handler(this::handleGetOne);
    router.post(ROUTE_ALL).handler(this::handleAdd);
    router.post(ROUTE_ONE).handler(this::handleUpdate);
    router.delete(ROUTE_ONE).handler(this::handleDeleteOne);
    router.delete(ROUTE_ALL).handler(this::handleDeleteAll);

    vertx.createHttpServer().requestHandler(router).listen(8080, httpServer -> {
      if (httpServer.succeeded()) {
        LOG.info("Server is up and listening on 8080! Hooray!");
      } else {
        LOG.severe("Couldn't start server on 8080, booooo! :(");
      }
    });
  }

  private Number numPeepz() {
    return numPeepz;
  }

  private void handleDeleteOne(final RoutingContext routingContext) {
    final var personId = routingContext.request().getParam(PARAM_PERSON_ID);
    if (personId == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      final var params = new JsonArray().add(Integer.parseInt(personId));
      jdbcClient.updateWithParams(DELETE_ONE_SQL, params, res -> {
        if (res.succeeded()) {
          numPeepz.decrementAndGet();
          routingContext.response().setStatusCode(204).end();
        } else {
          routingContext.response().setStatusCode(500).end();
        }
      });
    }
  }

  private void handleDeleteAll(final RoutingContext routingContext) {
    jdbcClient.update(DELETE_ALL_SQL, res -> {
      if (res.succeeded()) {
        numPeepz.set(0);
        routingContext.response().setStatusCode(204).end();
      } else {
        routingContext.response().setStatusCode(500).end();
      }
    });
  }

  private void handleAdd(final RoutingContext routingContext) {
    final var bodyAsJson = routingContext.getBodyAsJson();
    if (bodyAsJson != null) {
      if (bodyAsJson.getValue("id") != null) {
        routingContext.response().setStatusCode(400).putHeader("Content-Type", "text/plain").end("POST is for create, use PUT if you have an ID.");
      }
      final var generatedId = new Random().nextInt();
      final var jsonArray = new JsonArray()
        .add(generatedId)
        .add(bodyAsJson.getString("firstName"))
        .add(bodyAsJson.getString("lastName"));
      jdbcClient.updateWithParams(INSERT_SQL, jsonArray, res -> {
        if (res.succeeded()) {
          numPeepz.incrementAndGet();
          routingContext.response().setStatusCode(201).putHeader("Location", "http://localhost:8080/persons/" + generatedId).end();
        } else {
          routingContext.response().setStatusCode(500).end();
        }
      });
    }
  }

  private void handleUpdate(final RoutingContext routingContext) {
    final var bodyAsJson = routingContext.getBodyAsJson();
    if (bodyAsJson != null) {
      if (bodyAsJson.getValue("id") == null) {
        routingContext.response().setStatusCode(400).putHeader("Content-Type", "text/plain").end("PUT is for update, use POST if you have no ID, yet.");
      }
      final var jsonArray = new JsonArray()
        .add(bodyAsJson.getInteger("firstName"))
        .add(bodyAsJson.getString("lastName"))
        .add(bodyAsJson.getString("id"));
      jdbcClient.updateWithParams(UPDATE_SQL, jsonArray, res -> {
        if (res.succeeded()) {
          routingContext.response().setStatusCode(204).end();
        } else {
          routingContext.response().setStatusCode(500).end();
        }
      });
    }
  }

  private void handleGetOne(final RoutingContext routingContext) {
    final var personId = routingContext.request().getParam(PARAM_PERSON_ID);
    if (personId == null) {
      routingContext.response().setStatusCode(400).putHeader("Content-Type", "text/plain").end("Must supply 'personId'");
    } else {
      int personIdNumber = Integer.parseInt(personId);
      final var params = new JsonArray().add(personIdNumber);
      jdbcClient.queryWithParams(SELECT_ONE_SQL, params, res -> {
        if (res.succeeded()) {
          final var collect = res.map(ResultSet::getRows)
            .result()
            .stream()
            .findFirst()
            .map(original -> {
              final var entry = new JsonObject();
              entry.put("id", original.getInteger("ID"));
              entry.put("firstName", original.getString("FIRST_NAME"));
              entry.put("lastName", original.getString("LAST_NAME"));
              return entry;
            });
          if (collect.isPresent()) {
            routingContext.response().putHeader("Content-Type", "application/json").end(Json.encodePrettily(collect.get()));
          } else {
            routingContext.response().setStatusCode(400).end();
          }
        } else {
          routingContext.response().setStatusCode(500).end();
        }
      });
    }
  }

  private void handleListAll(final RoutingContext routingContext) {
    jdbcClient.query(SELECT_ALL_SQL, res -> {
      if (res.succeeded()) {
        final var collect = res.map(ResultSet::getRows)
          .result()
          .stream()
          .map(original -> {
            final var entry = new JsonObject();
            entry.put("id", original.getInteger("ID"));
            entry.put("firstName", original.getString("FIRST_NAME"));
            entry.put("lastName", original.getString("LAST_NAME"));
            return entry;
          }).collect(Collectors.toList());
        routingContext.response().putHeader("Content-Type", "application/json").end(Json.encodePrettily(collect));
      } else {
        routingContext.response().setStatusCode(500).end();
      }
    });
  }
}
