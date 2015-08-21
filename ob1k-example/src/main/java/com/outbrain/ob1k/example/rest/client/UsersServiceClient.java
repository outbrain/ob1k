package com.outbrain.ob1k.example.rest.client;

import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.client.Clients;
import com.outbrain.ob1k.client.targets.SimpleTargetProvider;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.ErrorHandler;
import com.outbrain.ob1k.example.rest.api.User;
import com.outbrain.ob1k.example.rest.api.UsersService;
import com.outbrain.ob1k.example.rest.server.RestServer;
import rx.Observable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.outbrain.ob1k.HttpRequestMethodType.*;

/**
 * UsersService Client
 * Creates new RPC client for Users service, and calling some endpoints
 *
 * @author marenzon
 */
public class UsersServiceClient {

  public static void main(final String[] args) throws Exception {

    // Initializing new server for the client
    final RestServer server = new RestServer();
    server.start(RestServer.PORT);

    System.out.println("Started new server on port " + RestServer.PORT);

    final String target = "http://localhost:" + RestServer.PORT + RestServer.CONTEXT_PATH + RestServer.SERVICE_PATH;
    final UsersService userService = new ClientBuilder<>(UsersService.class).
      setRequestTimeout(500).
      setConnectionTimeout(500).
      setRetries(3).
      setTargetProvider(new SimpleTargetProvider(target)).
      bindEndpoint("fetchAll", GET, "/").
      bindEndpoint("createUser", POST, "/").
      bindEndpoint("fetchUser", GET, "/{id}").
      bindEndpoint("updateUser", PUT, "/{id}").
      bindEndpoint("deleteUser", DELETE, "/{id}").
      bindEndpoint("subscribeChanges", ANY, "/subscribe").
      build();

    // Subscribing to all changes
    final AtomicInteger eventsCounter = new AtomicInteger(0);
    final Observable<Map<Integer, User>> changesObservable = userService.subscribeChanges();
    changesObservable.forEach(action -> eventsCounter.incrementAndGet());

    // Create new user
    final ComposableFuture<User> creationFuture = userService.createUser(new User("Jonathan Uberjar", "/dev/null", "Build Manager"));
    // Delete it once created
    final ComposableFuture<User> deletionFuture = creationFuture.continueOnSuccess((User result) -> {
      return userService.deleteUser(result.getId());
    });

    deletionFuture.consume(result -> System.out.println("Deleted Jonathan after creating: " + result.isSuccess()));
    deletionFuture.get();

    final List<User> users = userService.fetchAll().get();
    System.out.println("Current users: " + users);

    userService.fetchUser(10).continueOnError((ErrorHandler<User>) error -> {
      System.out.println("Tried fetch bad user: " + error.toString());
      return null;
    }).get();

    System.out.println("Total events counted: " + eventsCounter.get());

    System.out.println("Closing the client, and the server");
    // Closing down the client
    Clients.close(userService);
    // Shutting down the server
    server.stop();
  }
}
