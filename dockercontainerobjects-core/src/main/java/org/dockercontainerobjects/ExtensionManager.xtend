package org.dockercontainerobjects

import static extension org.dockercontainerobjects.util.Fields.updateFields
import static extension org.dockercontainerobjects.util.Loggers.debug
import static extension org.dockercontainerobjects.util.Predicates.operator_and
import static extension org.dockercontainerobjects.util.Strings.operator_tripleLessThan

import static org.dockercontainerobjects.util.Fields.annotatedWith
import static org.dockercontainerobjects.util.Fields.onInstance
import static org.dockercontainerobjects.util.Predicates.NOTHING

import java.util.ArrayList
import java.util.List
import java.util.ServiceLoader
import javax.inject.Inject
import org.eclipse.xtend.lib.annotations.Accessors
import org.slf4j.LoggerFactory

class ExtensionManager {

    private static val l = LoggerFactory.getLogger(ContainerObjectsManagerImpl)

    @Accessors(PUBLIC_GETTER)
    static val instance = new ExtensionManager(loadExtensions)

    val List<ContainerObjectsExtension> extensions

    protected new(List<ContainerObjectsExtension> extensions) {
        this.extensions = extensions
    }

    protected static def loadExtensions() {
        val loader = ServiceLoader.load(ContainerObjectsExtension)
        val List<ContainerObjectsExtension> result = new ArrayList
        for (e: loader) result.add(e)
        result
    }

    protected def void setupEnvironment(ContainerObjectsEnvironment env) {
        extensions.forEach [ setupEnvironment(env) ]
    }

    protected def void teardownEnvironment(ContainerObjectsEnvironment env) {
        extensions.forEach [ teardownEnvironment(env) ]
    }

    protected def <T> void updateContainerObjectFields(ContainerObjectContext<T> ctx) {
        extensions.forEach [ e |
            l.debug [ "Requesting field filter on stage '%s' to extension '%s'" <<< #[ctx.stage, e.class.simpleName] ]
            val extensionSelector = e.getFieldSelector(ctx)
            if (extensionSelector !== null && extensionSelector !== NOTHING) {
                val selector = onInstance && annotatedWith(Inject) && extensionSelector
                ctx.instance.updateFields(selector) [ field |
                    l.debug [ "Injecting value on stage '%s' from extension '%s' to field '%s'" <<< #[ctx.stage, e.class.simpleName, field.name] ]
                    e.getFieldValue(ctx, field)
                ]
            }
        ]
    }
}
