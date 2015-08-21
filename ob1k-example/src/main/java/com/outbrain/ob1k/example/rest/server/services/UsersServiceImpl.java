package com.outbrain.ob1k.example.rest.server.services;

import com.outbrain.ob1k.Response;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.example.rest.api.User;
import com.outbrain.ob1k.example.rest.api.UsersException;
import com.outbrain.ob1k.example.rest.api.UsersService;
import com.outbrain.ob1k.server.netty.ResponseBuilder;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromError;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;

/**
 * Simple endpoint which exposes Users resource
 * and provides restful access
 * <p>
 * All endpoints returns ComposableFuture, contains the result
 *
 * @author marenzon
 */
public class UsersServiceImpl implements UsersService {

  private static final Logger logger = LoggerFactory.getLogger(UsersServiceImpl.class);

  private final PublishSubject<Map<Integer, User>> subject = PublishSubject.create();
  private final AtomicInteger userIdCounter = new AtomicInteger(1);
  private final ConcurrentMap<Integer, User> users = new ConcurrentHashMap<Integer, User>() {{
    final int id = userIdCounter.getAndIncrement();
    final User user = new User("Mister Null", "Somewhere over the rainbow", "Coding for beer");
    user.setId(id);
    put(id, user);
  }};

  @Override
  public ComposableFuture<List<User>> fetchAll() {
    final List<User> usersList = users.values().stream().collect(Collectors.toList());
    return fromValue(usersList);
  }

  @Override
  public ComposableFuture<User> fetchUser(final int id) {
    if (!users.containsKey(id)) {
      return fromError(new UsersException("User " + id + " does not exists"));
    }

    return fromValue(users.get(id));
  }

  @Override
  public ComposableFuture<User> createUser(final User userData) {
    final int id = userIdCounter.getAndIncrement();
    final User user = new User(userData.getName(), userData.getAddress(), userData.getProfession());
    user.setId(id);
    users.put(id, user);
    logger.info("Created user: " + user.toString());
    publishChanged();
    return fromValue(user);
  }

  @Override
  public ComposableFuture<Response> updateUser(final int id, final User userData) {
    final User user = users.get(id);
    if (user == null) {
      return fromError(new UsersException("User not exists"));
    }

    user.updateFrom(userData);
    logger.info("Updated user: " + user.getId());
    publishChanged();
    return fromValue(ResponseBuilder.fromStatus(HttpResponseStatus.NO_CONTENT).build());
  }

  @Override
  public ComposableFuture<User> deleteUser(final int id) {
    if (!users.containsKey(id)) {
      return fromError(new UsersException("User not exists"));
    }

    final User removed = users.remove(id);
    logger.info("Deleted user: " + id);
    publishChanged();
    return fromValue(removed);
  }

  /**
   * Stream of events each time action occurred on the users map
   */
  @Override
  public Observable<Map<Integer, User>> subscribeChanges() {
    return subject;
  }

  private void publishChanged() {
    subject.onNext(users);
  }
}