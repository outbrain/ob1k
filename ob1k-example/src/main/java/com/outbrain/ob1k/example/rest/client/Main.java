package com.outbrain.ob1k.example.rest.client;

import static com.outbrain.ob1k.HttpRequestMethodType.*;
import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.client.Clients;
import com.outbrain.ob1k.client.targets.SimpleTargetProvider;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.example.rest.model.IUserService;
import com.outbrain.ob1k.example.rest.model.User;
import com.outbrain.ob1k.example.rest.model.UserActions;
import com.outbrain.ob1k.example.rest.server.UsersServer;
import com.outbrain.ob1k.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Main class which launches an Ob1k server and then issues client requests  to it via Http.
 */
public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);
  private static final int PORT = 8080;

  public static void main(final String[] args) throws Exception {
    //starts the server
    final Server server = UsersServer.buildServer(PORT);
    server.start();
    logger.info("** "+UsersServer.class.getSimpleName() + " Have Been Started On Port: {} **", PORT);

    runClient();

    server.stop();
    System.exit(0); // TODO: Add capability for Ob1k to shutdown in a programmtic clean way
  }

  private static void runClient() throws Exception {
    final String target = "http://localhost:" + PORT + UsersServer.CONTEXT_PATH + UsersServer.SERVICE_PATH;
    final IUserService userService = new ClientBuilder<>(IUserService.class).
      setRequestTimeout(500).
      setConnectionTimeout(500).
      setRetries(3).
      setTargetProvider(new SimpleTargetProvider(target)).
      bindEndpoint("getAll",     GET, "/").
      bindEndpoint("fetchUser",  GET, "/{id}").
      bindEndpoint("updateUser", POST, "/{id}").
      bindEndpoint("deleteUser", DELETE, "/{id}").
      bindEndpoint("createUser", PUT, "/").
      build();
    final ComposableFuture<UserActions> user1 = userService.createUser(
      new User(1, "Harry Potter", "Somewhere over the rainbow", "coding for beer"));
    final ComposableFuture<UserActions> user2 = userService.createUser(
      new User(2, "Hairy Plotter", "Somewhere over the rainbow", "coding for beer"));
    final ComposableFuture<List<UserActions>> all = ComposableFutures.all(user1, user2);

    final ComposableFuture<User> myUser = all.continueOnSuccess((List<UserActions> result) -> {
      return userService.fetchUser(1);
    });
    final String address = myUser.get().getAddress();
    System.out.println("*** The address is "+address);
    Clients.close(userService);
  }
}
