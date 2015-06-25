package com.outbrain.ob1k.example.rest.model;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import java.util.List;

/**
 * Interface for UserService client
 */
public interface IUserService extends Service {
  ComposableFuture<List<User>> getAll();
  ComposableFuture<User> fetchUser(final int id);
  ComposableFuture<UserActions> createUser(final User userData);
  ComposableFuture<UserActions> updateUser(final int id, final User userData);
  ComposableFuture<UserActions> deleteUser(final int id);
}
