package com.outbrain.ob1k.example.rest.server.endpoints;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.example.rest.model.IUserService;
import com.outbrain.ob1k.example.rest.model.User;
import com.outbrain.ob1k.example.rest.model.UserActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;

/**
 * Simple endpoint which exposes Users resource
 * and provides restful access
 *
 * @author marenzon
 */
public class UsersService implements IUserService {

  private static final Logger logger = LoggerFactory.getLogger(UsersService.class);

  private final AtomicInteger userIdCounter = new AtomicInteger(1);
  private final Map<Integer, User> users = new ConcurrentHashMap<>();

  public ComposableFuture<List<User>> getAll() {
    final List<User> usersList = new ArrayList<>();

    for (final User user : users.values()) {
      usersList.add(user);
    }

    return fromValue(usersList);
  }

  public ComposableFuture<User> fetchUser(final int id) {
    final User user = users.containsKey(id) ? users.get(id) : new User();
    return fromValue(user);
  }

  public ComposableFuture<UserActions> createUser(final User userData) {
    final int id = userIdCounter.getAndIncrement();
    final User user = new User(id, userData.getName(), userData.getAddress(), userData.getProfession());

    users.put(id, user);
    logger.info("Created user: " + user.toString());
    return fromValue(new UserActions(id, UserActions.Actions.CREATE));
  }

  public ComposableFuture<UserActions> updateUser(final int id, final User userData) {
    final User user = users.get(id);
    if (user == null) {
      return fromValue(new UserActions(id, UserActions.Actions.ERROR));
    }

    user.updateFrom(userData);
    logger.info("Updated user: " + user.getId());
    return fromValue(new UserActions(id, UserActions.Actions.UPDATE));
  }

  public ComposableFuture<UserActions> deleteUser(final int id) {
    if (!users.containsKey(id)) {
      return fromValue(new UserActions(id, UserActions.Actions.ERROR));
    }

    users.remove(id);
    logger.info("Deleted user: " + id);
    return fromValue(new UserActions(id, UserActions.Actions.DELETE));
  }
}