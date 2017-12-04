@file:JvmName("Members")
@file:Suppress("NOTHING_TO_INLINE")

package org.dockercontainerobjects.util

import java.lang.reflect.Member
import java.lang.reflect.Modifier.isFinal
import java.lang.reflect.Modifier.isStatic
import java.util.function.Predicate

inline val Member.isOnClass get() = isStatic(modifiers)
inline fun <T: Member> onClass() = Predicate<T> { it.isOnClass }

inline val Member.isOnInstance get() = !isStatic(modifiers)
inline fun <T: Member> onInstance() = Predicate<T> { it.isOnInstance }

inline val Member.isReadOnly get() = isFinal(modifiers)
inline fun <T: Member> readOnly() = Predicate<T> { it.isReadOnly }

inline val Member.isReadWrite get() = !isFinal(modifiers)
inline fun <T: Member> readWrite() = Predicate<T> { it.isReadWrite }
