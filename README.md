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
[![Latest Version](https://api.bintray.com/packages/dockercontainerobjects/maven/dockercontainerobjects-junit-junit4/images/download.svg) ](https://bintray.com/dockercontainerobjects/maven/dockercontainerobjects-junit-junit4/_latestVersion)

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

Container Objects can be defined as class fields or instance fields. (Only if using JUnit4 `@RunWidth` or JUnit5 `@ExtendsWith`)
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

## Container configuration

### Using existing images

The annotation `@RegistryImage` can be applied to the class to define the image that will be used to start the container.
For example, if a container object class is annotated with `@RegistryImage("tomcat:jre8")` then the container will be based on the image `"tomcat:jre8"`.
If the image is not found locally, or if the parameter `forcePull` is set to `true`, the image will be downloaded from the registry.
If the image did not exist locally, and was pulled from the registry, and the parameter `autoRemove` is set to true, when the container is removed, the image will be removed.
(unless there is another container depending on the image)

The annotation `@RegistryImage` can also be applied to a method.
In that case the `value` attribute will be ignored, and the result of invoking the method will be used instead.
The method must be defined as expecting no parameters and returning a `String`.
The result of the method will be used as the image to instantiate.
The annotation parameters `forcePull` and `autoRemove` will be read and used in the same way as if the annotation were applied to the class.

### Building new images

The annotation `@BuildImage` can be applied to the class to define the location of a Dockerfile to be used to build a new image for the container object to be created.
For example, if a container object class is annotated with `@BuildImage("classpath:///service/MyServiceDockerfile")`, a dockerfile will be looked in the specified location in the classpath.
The protocols `file:`, `http:`, `https:` and `classpath:` are currently supported.
For `classpath:`, `http:` and `https:`, the Dockerfile will be downloaded and the image will be created from the dockerfile only (no relative resources will be added).
For `file:`, the value could be a reference to a file or to a folder.
In that case, resources relative to the Dockerfile mentioned on it will be added to the image.

The annotation also has the attributes `imageTag` and `forcePull`.
The `imageTag` attribute can be used to specify a name for the image to be generated.
If the `imageTag` contains an `*`, its position on the name will be substituted by a random `UUID` value.
The `forcePull` attribute is used to force docker to try to download a new copy of the base image even if a new copy is downloaded.
All images generated with `@BuildImage` annotations are auto removables. They will be removed after the container object is destroyed.

The annotation `@BuildImage` can also be applied to a method.
In that case the value attribute will be ignored and the result of invoking the method will be used instead.
The method must be defined as expecting no parameters and returning either `String`, `URL`, `URI` or `InputStream`.
For the types `String`, `URL` or `URI`, it will be assumed that the value points to the Dockerfile.
For the type `InputStream`, it will be assumed that the content of the `InputStream` is the Dockerfile.

### Other configuration options

The annotation `@EnvironmentEntry` can be used to specify environment paramenters.
This annotation is repeatable, which means it can be applied multiple times to the container object class.
For example, if a container object class is annotated with `@EnvironmentEntry(key="DEFAULT_USER", value="TEST")`, the environment entry `DEFAULT_USER=TEST` will be added to the container at start.
If the `key` attribute is not set, the entry can be specified in the value, for example `@EnvironmentEntry("DEFAULT_USER=TEST")`.

## Injecting data inside the container object from docker

The container object class can define fields annotated with `@Inject` to receive information from docker.

- Fields of type `DockerClient` will receive a reference to the `DockerClient` instance used to talk to Docker.
- Fields annotated with `@ContainerId` and of type `String` will receive the container id after the container is created.
- Fields annotated with `@ContainerAddress` and of type either `String` or `Inet4Address` will receive the container internal IPv4 after the container is started.
- Fields annotated with `@ContainerAddress` and of type `Inet6Address` will receive the container global IPv6 after the container is started.
- Fields of type `NetworkSettings` will receive a reference to the `NetworkSettings` container information after starting it.
- Fields of type `ContainerObjectsManager` will receive a reference to a `ContainerObjectsManager` used to do some opperations on container objects.
