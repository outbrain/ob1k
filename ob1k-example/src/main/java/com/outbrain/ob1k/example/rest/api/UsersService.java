package com.outbrain.ob1k.example.rest.api;

import com.outbrain.ob1k.Response;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import rx.Observable;

import java.util.List;
import java.util.Map;

/**
 * UsersService Interface
 * @author marenzon
 */
public interface UsersService extends Service {
  ComposableFuture<List<User>> fetchAll();

  ComposableFuture<User> fetchUser(final int id);

  ComposableFuture<User> createUser(final User userData);

  ComposableFuture<Response> updateUser(final int id, final User userData);

  ComposableFuture<User> deleteUser(final int id);

  Observable<Map<Integer, User>> subscribeChanges();
}
