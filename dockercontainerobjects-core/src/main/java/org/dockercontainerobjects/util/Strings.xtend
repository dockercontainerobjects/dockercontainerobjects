package org.dockercontainerobjects.util

class Strings {

    public static def <<< (String format, Object... params) {
        String.format(format, params)
    }

    public static def toCapitalCase(String text) {
        text.substring(0, 1).toUpperCase+text.substring(1).toLowerCase
    }
}
