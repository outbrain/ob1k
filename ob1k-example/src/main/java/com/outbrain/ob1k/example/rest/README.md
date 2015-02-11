#Rest - A Simple Rest Server Example
This example attempts to show a usage of ob1k with the option of binding endpoints with specific http method type.
To start the service, open the example via your favorite IDE and run "RestServer.java", then go to: `http://localhost:8080/api/users`

##Abstract - Server Endpoints Registration
As you may see in RestServer, we have the following bindings:

```java
builder.addEndpoint(HttpRequestMethodType.GET, "getAll", "/");
builder.addEndpoint(HttpRequestMethodType.GET, "fetchUser", "/{id}");
builder.addEndpoint(HttpRequestMethodType.POST, "updateUser", "/{id}");
builder.addEndpoint(HttpRequestMethodType.DELETE, "deleteUser", "/{id}");
builder.addEndpoint(HttpRequestMethodType.PUT, "createUser", "/");
```

Each addEndpoint describes its path (i.e. for getAll, it's "/") with its method inside the service class
and with the http method type. This example shows you how to register different http method types on the same uri
path, with an usage of path params.

**Note**: Defining the http method type is not required, calling just addEndpoint without the method will lead
to a default method of ANY - which means that the method doesn't care about its request method type.

##Explaining Method Signatures
Having the following method signature: `updateUser(final int id, final User userData)` bounded to the path: `/{id}`
We see that the method receives its id from the path param, while the User object (after the un-marshalling) comes in the request body.
This behavior basically gives us the ability of expressing of **which** resource we're updating, with **what** data.

**Important note**: When using path params in POST or PUT, each path param should always be in the beginning of your method signature, else you'll be having
exceptions in your ServiceBuilder. That means if your path looks like: `/hello/{world}`, the method bounded to it should look like `hello(String world, String moreData)`


##Client API Usage
The following example defines a resource service called **UsersService**
which exposes the following endpoints:

> [**GET**]: "/" => Fetches all users
> 
> [**PUT**]: "/" => Creates a new user (Required fields: name, address,
> profession)
> 
> [**DELETE**]: "/{id}" => Deletes an user
> 
> [**GET**]: "/{id}" => Fetches a specific user
> 
> [**POST**]: "/{id}" => Updates an user (Optional fields: name, address,
> profession)

The requests should be in either JSON format or MessagePack.

##Interface
This example doesn't contains with it some web interface which interacts with the service.
You're encouraged to use a simple nice tool called [Postman](http://www.getpostman.com/).

##Todo
Add ob1k client example
