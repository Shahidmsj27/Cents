# Keep Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
