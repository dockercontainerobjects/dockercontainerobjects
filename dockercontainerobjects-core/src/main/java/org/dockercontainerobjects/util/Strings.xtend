package org.dockercontainerobjects.util

import static extension java.lang.Character.isUpperCase
import static extension java.lang.Character.toLowerCase

class Strings {

    private static val char UNDERSCORE = '_'

    @Pure
    public static def <<< (String format, Object... params) {
        String.format(format, params)
    }

    @Pure
    public static def toCapitalCase(String text) {
        text.substring(0, 1).toUpperCase+text.substring(1).toLowerCase
    }

    @Pure
    public static def toSnakeCase(String text) {
        val result = new StringBuilder()
        val chars = text.toCharArray
        for (var index = 0; index < chars.length; index++) {
            val ch = chars.get(index)
            if(ch.isUpperCase) {
                if ((index > 0) && (!chars.get(index-1).isUpperCase || ((index+1 < chars.length) && !chars.get(index+1).isUpperCase)))
                    result.append(UNDERSCORE)

                result.append(ch.toLowerCase)
            } else
                result.append(ch)
        }
        result.toString
    }
}
