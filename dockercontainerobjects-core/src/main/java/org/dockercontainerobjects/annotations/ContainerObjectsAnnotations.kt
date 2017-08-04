package org.dockercontainerobjects.annotations

import java.lang.annotation.Inherited
import java.lang.annotation.Repeatable
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION

@MustBeDocumented
@Retention(RUNTIME)
@Target(FIELD, AnnotationTarget.VALUE_PARAMETER)
@Inherited
annotation class ContainerObject

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class BeforePreparingImage

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class BeforeBuildingImage

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class AfterImageBuilt

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class AfterImagePrepared

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class BeforeCreatingContainer

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class AfterContainerCreated

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class BeforeStartingContainer

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class AfterContainerStarted

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class BeforeStoppingContainer

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class AfterContainerStopped

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class BeforeRestartingContainer

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class AfterContainerRestarted

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class BeforeRemovingContainer

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class AfterContainerRemoved

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class BeforeReleasingImage

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class BeforeRemovingImage

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class AfterImageRemoved

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class AfterImageReleased

@MustBeDocumented
@Retention(RUNTIME)
@Target(FIELD)
@Inherited
@Qualifier
annotation class ContainerId

@MustBeDocumented
@Retention(RUNTIME)
@Target(FIELD)
@Inherited
@Qualifier
annotation class ContainerAddress

@MustBeDocumented
@Retention(RUNTIME)
@Target(CLASS, FUNCTION)
@Inherited
annotation class RegistryImage(val value: String = "", val forcePull: Boolean = false, val autoRemove: Boolean = false)

@MustBeDocumented
@Retention(RUNTIME)
@Target(CLASS, FUNCTION)
@Inherited
annotation class BuildImage(val value: String = "", val tag: String = "", val forcePull: Boolean = false)

@MustBeDocumented
@Retention(RUNTIME)
@Target(CLASS, FUNCTION)
@Inherited
annotation class Environment(vararg val value: EnvironmentEntry = arrayOf())

@MustBeDocumented
@Retention(RUNTIME)
@Target(CLASS, FUNCTION)
@Inherited
@Repeatable(Environment::class)
annotation class EnvironmentEntry(val name: String = "", val value: String = "")

@MustBeDocumented
@Retention(RUNTIME)
@Target(CLASS, FUNCTION)
@Inherited
annotation class BuildImageContent(vararg val value: BuildImageContentEntry = arrayOf())

@MustBeDocumented
@Retention(RUNTIME)
@Target(CLASS, FUNCTION)
@Inherited
@Repeatable(BuildImageContent::class)
annotation class BuildImageContentEntry(val name: String = "", val value: String = "")

@MustBeDocumented
@Retention(RUNTIME)
@Target(FUNCTION)
@Inherited
annotation class OnLogEntry(val includeStdOut: Boolean = true, val includeStdErr: Boolean = true, val includeTimestamps: Boolean = false)
