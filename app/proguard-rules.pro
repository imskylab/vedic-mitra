# R8 / ProGuard rules for :app (release builds only).
#
# Most keep rules are supplied automatically as consumer rules by the libraries we use — Jetpack
# Compose, Hilt/Dagger, AndroidX Lifecycle & Navigation, DataStore and kotlinx-coroutines all ship
# their own — so this file stays small. Add app-specific keeps here if a release build misbehaves.

# Keep source/line metadata so release crash reports de-obfuscate to real file:line, while still
# renaming the stored source file name to hide the original.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Offline time-zone lookup (us.dustinj.timezonemap): it loads bundled polygon data, parses it with
# flatbuffers, and decompresses it with zstd-jni.
#
# zstd-jni is the critical one: its native library (libzstd-jni.so) reaches back into the JVM and
# looks up instance fields — ZstdInputStreamNoFinalizer.srcPos / .dstPos — *by name* via JNI
# GetFieldID. R8 renames those fields by default (proguard-android-optimize keeps native *method*
# names but not the fields native code reads), so in a minified build the lookup fails with
# `NoSuchFieldError: no "J" field "srcPos"`. That error surfaces on a native thread and the native
# code immediately throws again, so ART aborts the whole process (SIGABRT) — an *uncatchable* crash
# that a Kotlin try/catch or runCatching around the lookup cannot stop. Keeping the classes AND their
# members (names included) is what makes the JNI field lookup resolve. Manifested as a crash when
# picking a birthplace, which is the only screen that triggers a time-zone lookup.
-keep class us.dustinj.timezonemap.** { *; }
-keep class com.google.flatbuffers.** { *; }
-keep class com.github.luben.zstd.** { *; }
-keepclassmembers class com.github.luben.zstd.** { *; }
-dontwarn us.dustinj.timezonemap.**
-dontwarn com.google.flatbuffers.**
-dontwarn com.github.luben.zstd.**

# Persisted data (profiles, reminders, japa sittings) is encoded with hand-rolled codecs that read
# each enum's constant name (e.g. ProfileRelation.SELF -> "SELF"). The default enum rule in
# proguard-android-optimize.txt keeps values()/valueOf() and the constant fields, so those stored
# strings stay valid across releases — no extra keep needed. This block documents that dependency:
# if the enum-keep default is ever removed, restore it here.
# -keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
