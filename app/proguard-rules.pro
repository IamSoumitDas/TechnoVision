# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in getDefaultProguardFile('proguard-android-optimize.txt').

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep data models
-keepclassmembers class com.soumit.technovision.data.model.** { *; }
-keep class com.soumit.technovision.data.model.** { *; }

# Keep Room database entities
-keep class com.soumit.technovision.data.local.room.** { *; }

# Baseline Profile installer
-keep class androidx.profileinstaller.** { *; }
