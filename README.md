# Docker Container Objects

[![Build Status](https://travis-ci.org/dockercontainerobjects/dockercontainerobjects.svg?branch=master)](https://travis-ci.org/dockercontainerobjects/dockercontainerobjects)
[![Latest Version](https://api.bintray.com/packages/dockercontainerobjects/maven/dockercontainerobjects-core/images/download.svg) ](https://bintray.com/dockercontainerobjects/maven/dockercontainerobjects-core/_latestVersion)

This projects promotes testing using docker containers, in an "Object Oriented" way.

Supports both JUnit 4 and JUnit Platform 1, a.k.a. JUnit 5

It is based on the concept of Container Objects defined in [Arquillian Cube](http://arquillian.org/arquillian-cube/) extension

## How to use

### JUnit Platform 1, a.k.a. JUnit 5

JUnit Platform 1 is supported by an extension.

If using Maven, add the following dependency:

```xml
<dependency>
    <groupId>org.dockercontainerobjects</groupId>
    <artifactId>dockercontainerobjects-junit-platform1</artifactId>
    <version>${dockerContainerObjectsVersion}</version>
    <scope>test</scope>
</dependency>
```

If using Gradle, add the following dependency:

```groovy
testCompile "org.dockercontainerobjects:dockercontainerobjects-junit-platform1:${dockerContainerObjectsVersion}"
```

Or download the required JARs:

[![Latest Version](https://api.bintray.com/packages/dockercontainerobjects/maven/dockercontainerobjects-core/images/download.svg) ](https://bintray.com/dockercontainerobjects/maven/dockercontainerobjects-core/_latestVersion)
[![Latest Version](https://api.bintray.com/packages/dockercontainerobjects/maven/dockercontainerobjects-junit-platform1/images/download.svg) ](https://bintray.com/dockercontainerobjects/maven/dockercontainerobjects-junit-platform1/_latestVersion)

them create a test class, for example:

```java
@ExtendWith(DockerContainerObjectsExtension.class) //(1)
class MyContainerTest {

    @ContainerObject TomcatContainer tomcatContainer; //(2)

    @Test
    void testContainerCreated() {
        assertNotNull(tomcatContainer); //(3)
        assertNotNull(tomcatContainer.addr);
    }

    @RegistryImage("tomcat:jre8") //(4)
    public static class TomcatContainer {

        @Inject @ContainerAddress InetAddress addr; //(5)
        @Inject @ContainerId String containerId;
    }
}
```

1. Create a JUnit test annotated with `@ExtendWith`, referencing the extension class `DockerContainerObjectsExtension`.
2. Add an attribute annotated with `@ContainerObject`, referencing your container object.
3. Your test methods can reference your container object methods and attributes.
4. Define a container object class, that can be annotated with `@RegistryImage` to define a docker image.
5. The container object class can have fields annotated with `@Inject` and different other annotations to inject information from the docker container.

### JUnit 4

JUnit 4 is supported in two different ways: By a runner, and by rules.

If using Maven, add the following dependency:

```xml
<dependency>
    <groupId>org.dockercontainerobjects</groupId>
    <artifactId>dockercontainerobjects-junit-junit4</artifactId>
    <version>${dockerContainerObjectsVersion}</version>
    <scope>test</scope>
</dependency>
```

If using Gradle, add the following dependency:

```groovy
testCompile "org.dockercontainerobjects:dockercontainerobjects-junit-junit4:${dockerContainerObjectsVersion}"
```

Or download the required JARs:

[![Latest Version](https://api.bintray.com/packages/dockercontainerobjects/maven/dockercontainerobjects-core/images/download.svg) ](https://bintray.com/dockercontainerobjects/maven/dockercontainerobjects-core/_latestVersion)
[![Latest Version](https://api.bintray.com/packages/dockercontainerobjects/maven/dockercontainerobjects-junit-platform1/images/download.svg) ](https://bintray.com/dockercontainerobjects/maven/dockercontainerobjects-junit-platform1/_latestVersion)

#### JUnit 4 runner

The JUnit 4 runner is the simplest option. It works in the same way as the JUnit Platform 1 extension. This runner supports rules identical the default JUnit4 runner.

Create a test class, for example:

```java
@RunWith(DockerContainerObjectsRunner.class) //(1)
class MyContainerTest {

    @ContainerObject TomcatContainer tomcatContainer; //(2)

    @Test
    public void testContainerCreated() {
        assertNotNull(tomcatContainer); //(3)
        assertNotNull(tomcatContainer.addr);
    }

    @RegistryImage("tomcat:jre8") //(4)
    public static class TomcatContainer {

        @Inject @ContainerAddress InetAddress addr; //(5)
        @Inject @ContainerId String containerId;
    }
}
```

1. Create a JUnit test annotated with `@RunWith`, referencing the runner class `DockerContainerObjectsRunner`.
2. Add an attribute annotated with `@ContainerObject`, referencing your container object.
3. Your test methods can reference your container object methods and attributes.
4. Define a container object class, that can be annotated with `@RegistryImage` to define a docker image.
5. The container object class can have fields annotated with `@Inject` and different other annotations to inject information from the docker container.

#### JUnit 4 rules

The rules are an alternative to the Runner. This may be useful if another runner is needed, as JUnit 4 only supports one runner per test class.

Create a test class, for example:

```java
class MyContainerTest { //(1)

    @ClassRule
    public static ContainerObjectsEnvironmentResource envResource =
            new ContainerObjectsEnvironmentResource(); //(2)

    @Rule
    public ContainerObjectResource<SimpleContainer> tomcatContainerResource =
            new ContainerObjectResource<>(envResource, SimpleContainer.class); //(3)

    @Test
    public void testContainerCreated() {
        assertNotNull(tomcatContainerResource.getContainerInstance()); //(4)
        assertNotNull(tomcatContainerResource.getContainerInstance().addr);
    }

    @RegistryImage("tomcat:jre8") //(5)
    public static class TomcatContainer {

        @Inject @ContainerAddress InetAddress addr; //(6)
        @Inject @ContainerId String containerId;
    }
}
```

1. Create a regular JUnit test, no runner annotation required.
2. Add an attribute of type `ContainerObjectsEnvironmentResource` annotated with `@ClassRule`.
3. Add one attribute of type `ContainerObjectResource` annotated with `@Rule`, referencing your container object.
4. Your test methods can reference your container object methods and attributes by calling `ContainerObjectResource.getContainerInstance()`.
5. Define a container object class, that can be annotated with `@RegistryImage` to define a docker image.
6. The container object class can have fields annotated with `@Inject` and different other annotations to inject information from the docker container.

## Container Objects in tests

Container Objects can be defined as class fields or instance fields.
Class fields will be created and started before any test method is executed and stopped and removed after all test methods are executed.
Instance fields will be created and started before every test method is executed and stopped and removed after the method finishes.

Container Objects can also be embedded inside other container objects.
All container objects linked from inside a container object will be started before starting the containing container object.
They will also be stopped after the containing container object is stopped.
A link will be created from the containing container object to each of the contained container objects.

## Container Objects lifecycle

Container objects can define methods annotated with any of `@BeforeCreating`, `@AfterCreated`, `@BeforeStarting`, `@AfterStarted`, `@BeforeStopping`, `@AfterStopped`, `@BeforeRemoving`, `@AfterRemoved`.
Such methods will be invoked on each stage of the container creation lifecycle.
Methods must be defined as instance methods, accepting no parameters and returning void.

## Injecting data inside the container object from docker

The container object class can define fields annotated with `@Inject` to receive information from docker.

- Fields of type `DockerClient` will receive a reference to the `DockerClient` instance used to talk to Docker.
- Fields annotated with `@ContainerId` and of type `String` will receive the container id after the container is created.
- Fields annotated with `@ContainerAddress` and of type either `String` or `Inet4Address` will receive the container internal IPv4 after the container is started.
- Fields annotated with `@ContainerAddress` and of type `Inet6Address` will receive the container global IPv6 after the container is started.
- Fields of type `NetworkSettings` will receive a reference to the `NetworkSettings` container information after starting it.
- Fields of type `ContainerObjectsManager` will receive a reference to a `ContainerObjectsManager` used to do some opperations on container objects.
