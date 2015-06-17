package com.outbrain.ob1k.security.server;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by gmarom on 6/9/15
 * <p/>
 * <p>
 * Provides services related with path->authenticator associations.
 * </p>
 * <p>
 * This class cannot be constructed directly, only through the {@code PathAssociationsBuilder} class.
 * </p>
 *
 * @param <T> the type of the class used in the authenticators associated with the paths,
 *           such as the {@code UserPasswordToken}
 */
public class PathAssociations<T> {

  public static final String PATH_DELIMITER = "/";
  //Key: An endpoint relative path
  //Value: An instance of a CredentialsAuthenticator
  //This is a multi map, so a URL might have several CredentialAuthenticators associated with it
  private final SetMultimap<String, CredentialsAuthenticator<T>> associations = HashMultimap.create();

  private PathAssociations(final SetMultimap<String, CredentialsAuthenticator<T>> associations) {
    this.associations.putAll(associations);
  }

  /**
   * @return all the {@code CredentialAuthenticators} associated with the given {@code path}
   * and an empty set if none exist
   */
  public Set<CredentialsAuthenticator<T>> getAuthenticators(final String path) {
    final Set<CredentialsAuthenticator<T>> authenticators = new HashSet<>(associations.size());
    return accumulateAuthenticators(validatePath(path), authenticators);
  }

  private Set<CredentialsAuthenticator<T>> accumulateAuthenticators(final String path,
                                                                    final Set<CredentialsAuthenticator<T>> authenticators) {
    authenticators.addAll(associations.get(path));

    if (isRootPath(path)) return authenticators;
    else return accumulateAuthenticators(parentPath(path), authenticators);
  }

  /**
   * <p>
   * Checks whether or not {@code authenticator} may authorize access to {@code path}. Such an authenticator exists
   * if it was associated with {@code path} or with any of its parents. For example for the path "/a/b/c" Any authenticator
   * assigned with either "/a/b/c", "/a/b", "/a", "/" is valid
   * </p>
   * <p/>
   * <p>
   * <table>
   * <tr>
   * <th>Authenticator associated with</th>
   * <th>Authorized paths</th>
   * <th>Unauthorized paths</th>
   * </tr>
   * <tr>
   * <td>/</td>
   * <td>/*</td>
   * <td>N/A</td>
   * </tr>
   * <tr>
   * <td>/service1</td>
   * <td>/service1/endpoint1<br>/service1/sub1/endpoint2</td>
   * <td>/service2/endpoint2</td>
   * </tr>
   * </table>
   * </p>
   */
  public boolean isAuthorized(final CredentialsAuthenticator authenticator, final String path) {
    if (StringUtils.isBlank(path) || isRootPath(path)) {
      return associations.containsEntry(PATH_DELIMITER, authenticator);
    } else {
      return associations.containsEntry(path, authenticator) || isAuthorized(authenticator, parentPath(path));
    }
  }

  private static boolean isRootPath(final String path) {
    return path.equals(PATH_DELIMITER);
  }

  /**
   * Returns a validated path that
   * <pre>
   *   1. Starts with a slash "/"
   *   2. Does not end with a slash "/"
   * </pre>
   * <pre>
   *   validatePath("")       = "/"
   *   validatePath("path")   = "/path"
   *   validatePath("/path/") = "/path"
   * </pre>
   */
  private String validatePath(final String path) {
    String validatedPath = StringUtils.removeEnd(path, PATH_DELIMITER);
    if (!validatedPath.startsWith(PATH_DELIMITER)) validatedPath = PATH_DELIMITER + validatedPath;
    return validatedPath;
  }

  /**
   * <p>
   * Returns a path's parent
   * </p>
   * <p/>
   * <pre>
   *   parentPath(null)                        = "/"
   *   parentPath("")                          = "/"
   *   parentPath("/")                         = "/"
   *   parentPath("part1")                     = "/"
   *   parentPath("part1/")                    = "/"
   *   parentPath("/part1")                    = "/"
   *   parentPath("/part1/")                   = "/"
   *   parentPath("/part1/part2/part3")        = "/part1/part2"
   *   parentPath("/part1/part2/part3/part4/") = "/part1/part2/part3"
   * </pre>
   */
  private String parentPath(final String path) {
    final String formattedPath = validatePath(path);
    if (StringUtils.containsNone(formattedPath, PATH_DELIMITER)) {
      return PATH_DELIMITER;
    } else if (isRootPath(formattedPath)) {
      return PATH_DELIMITER;
    } else if (StringUtils.countMatches(formattedPath, PATH_DELIMITER) == 1) {
      return PATH_DELIMITER;
    } else {
      return (formattedPath.substring(0, formattedPath.lastIndexOf(PATH_DELIMITER)));
    }
  }

  public static class PathAssociationsBuilder<T> {

    //Key: An endpoint relative path
    //Value: An instance of a CredentialsAuthenticator
    //This is a multi map, so a URL might have several CredentialAuthenticators associated with it
    private final SetMultimap<String, CredentialsAuthenticator<T>> asociations = HashMultimap.create();

    /**
     * Associates {@code path} with {@code authenticator}
     */
    public PathAssociationsBuilder<T> associate(final String path,
                                                final CredentialsAuthenticator<T> authenticator) {
      asociations.put(path, authenticator);
      return this;
    }

    public PathAssociations<T> build() {
      if (asociations.size() == 0)
        throw new IllegalStateException("No associates between authenticators and paths were made");

      return new PathAssociations<>(asociations);
    }

  }

}
