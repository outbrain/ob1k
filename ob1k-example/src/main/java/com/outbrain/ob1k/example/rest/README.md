#Rest Server Example
This example attempts to show a usage of ob1k with the option of binding endpoints with specific http method type.

To start the server, do one of the following:
* Go to project root and run `mvn exec:java -Dexec.mainClass="com.outbrain.ob1k.example.rest.server.RestServer" -pl ob1k-example`
* Open the example via your favorite IDE and run RestServer main

##Registration phase
As you can see in RestServer, we have the following bindings:

```java
serviceBuilder.addEndpoint(GET, "fetchAll", "/");
serviceBuilder.addEndpoint(POST, "createUser", "/");
serviceBuilder.addEndpoint(GET, "fetchUser", "/{id}");
serviceBuilder.addEndpoint(PUT, "updateUser", "/{id}");
serviceBuilder.addEndpoint(DELETE, "deleteUser", "/{id}");
serviceBuilder.addEndpoint(ANY, "subscribeChanges", "/subscribe");
```

Each binding describes what method to call on what uri. We see that for uri "/", the method "fetchAll"
from UsersService class will be called. Also, in this example we're also limiting to a specific method type
for the requests.

**Note**: Defining the http method type is not required, calling just addEndpoint without the method will lead
to a default method of ANY - which means that the method does not care about its method type.

##How it works
Having the following method signature: `updateUser(final int id, final User userData)` bounded to the path: `/{id}`,
we see that the method receives its id from a path param, while the User object comes from the request body.
This behavior basically gives us the ability of expressing **which** resource we're updating, with **what** data.

**Important note**: When using path params in POST or PUT, each path param should always be in the beginning of your method signature, else you'll the server
won't initialize. For example: for the following path: `/hello/{world}`, the method bounded to it should be: `hello(String world, String moreData)`

##How to use
The following example defines a resource service called **UsersService**
which exposes the following endpoints:

> [**GET**]: "/" => Fetches all users
> 
> [**POST**]: "/" => Creates a new user (Required fields: name, address, profession)
>
> [**GET**]: "/{id}" => Fetches a specific user
>
> [**DELETE**]: "/{id}" => Deletes an user
>
> [**PUT**]: "/{id}" => Updates an user (Optional fields: name, address, profession)
>
> [**ANY**]: "/subscribe" => Stream of users map, on each modification

The requests should be in either JSON or MessagePack.

##RPC client
This example contains RPC client bounded to our Users service.
Go ahead and run: `mvn exec:java -Dexec.mainClass="com.outbrain.ob1k.example.rest.client.UsersServiceClient" -pl ob1k-example` (or via your IDE)