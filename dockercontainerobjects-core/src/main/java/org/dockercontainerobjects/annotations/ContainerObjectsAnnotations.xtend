package org.dockercontainerobjects.annotations

import java.lang.^annotation.Documented
import java.lang.^annotation.Inherited
import java.lang.^annotation.Repeatable
import java.lang.^annotation.Retention
import java.lang.^annotation.Target
import javax.inject.Qualifier

@Documented
@Retention(RUNTIME)
@Target(#[FIELD, PARAMETER])
@Inherited
annotation ContainerObject {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation BeforePreparingImage {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation BeforeBuildingImage {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation AfterImageBuilt {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation AfterImagePrepared {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation BeforeCreatingContainer {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation AfterContainerCreated {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation BeforeStartingContainer {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation AfterContainerStarted {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation BeforeStoppingContainer {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation AfterContainerStopped {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation BeforeRestartingContainer {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation AfterContainerRestarted {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation BeforeRemovingContainer {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation AfterContainerRemoved {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation BeforeReleasingImage {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation BeforeRemovingImage {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation AfterImageRemoved {}

@Documented
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
annotation AfterImageReleased {}

@Documented
@Retention(RUNTIME)
@Target(FIELD)
@Inherited
@Qualifier
annotation ContainerId {}

@Documented
@Retention(RUNTIME)
@Target(FIELD)
@Inherited
@Qualifier
annotation ContainerAddress {}

@Documented
@Retention(RUNTIME)
@Target(#[TYPE, METHOD])
@Inherited
annotation RegistryImage {
    String value = ''
    boolean forcePull = false
    boolean autoRemove = false
}

@Documented
@Retention(RUNTIME)
@Target(#[TYPE, METHOD])
@Inherited
annotation BuildImage {
    String value = ''
    String tag = ''
    boolean forcePull = false
}

@Documented
@Retention(RUNTIME)
@Target(#[TYPE, METHOD])
@Inherited
annotation Environment {
    EnvironmentEntry[] value = #[]
}

@Documented
@Retention(RUNTIME)
@Target(#[TYPE, METHOD])
@Inherited
@Repeatable(Environment)
annotation EnvironmentEntry {
    String name = ''
    String value = ''
}

@Documented
@Retention(RUNTIME)
@Target(#[TYPE, METHOD])
@Inherited
annotation BuildImageContent {
    BuildImageContentEntry[] value = #[]
}

@Documented
@Retention(RUNTIME)
@Target(#[TYPE, METHOD])
@Inherited
@Repeatable(BuildImageContent)
annotation BuildImageContentEntry {
    String name = ''
    String value = ''
}
