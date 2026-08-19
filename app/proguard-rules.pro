# R8 / ProGuard rules for :app (release builds only).
#
# Most keep rules are supplied automatically as consumer rules by the libraries we use — Jetpack
# Compose, Hilt/Dagger, AndroidX Lifecycle & Navigation, DataStore and kotlinx-coroutines all ship
# their own — so this file stays small. Add app-specific keeps here if a release build misbehaves.

# Keep source/line metadata so release crash reports de-obfuscate to real file:line, while still
# renaming the stored source file name to hide the original.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Persisted data (profiles, reminders, japa sittings) is encoded with hand-rolled codecs that read
# each enum's constant name (e.g. ProfileRelation.SELF -> "SELF"). The default enum rule in
# proguard-android-optimize.txt keeps values()/valueOf() and the constant fields, so those stored
# strings stay valid across releases — no extra keep needed. This block documents that dependency:
# if the enum-keep default is ever removed, restore it here.
# -keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
