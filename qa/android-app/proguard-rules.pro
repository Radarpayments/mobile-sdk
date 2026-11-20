# The journal and the run report name an exception by its class; without this R8 turns those names into letters.
-keepnames class * extends java.lang.Throwable
