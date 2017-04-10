package org.dockercontainerobjects.util

class Strings {

    public static def <<< (String format, Object... params) {
        String.format(format, params)
    }
}
