# R8 rules for the Wear OS app. Play services, Compose and the complication
# library ship their own consumer rules; shared models are plain data classes.
-keep class com.example.booktracker.shared.models.** { *; }

-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
