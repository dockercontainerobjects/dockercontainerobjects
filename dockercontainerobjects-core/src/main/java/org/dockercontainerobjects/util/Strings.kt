package org.dockercontainerobjects.util

object Strings {
    private const val UNDERSCORE: Char = '_'

    fun String.toCapitalCase() = substring(0, 1).toUpperCase()+substring(1).toLowerCase()

    fun String.toSnakeCase(): String {
        val result = StringBuilder()
        val chars = toCharArray()

        var index = 0
        while (index < chars.size) {
            val ch = chars[index]
            if (ch.isUpperCase()) {
                if ((index > 0) && (!chars[index-1].isUpperCase() || ((index+1 < chars.size) && !chars[index+1].isUpperCase())))
                    result.append(UNDERSCORE)
                result.append(ch.toLowerCase())
            } else
                result.append(ch)
            index++
        }
        return result.toString()
    }
}
