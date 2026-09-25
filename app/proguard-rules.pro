# My Health Companion 2 - ProGuard Rules

# -------------------------------------------------------------------------
# Gson Rules (Critical for Backup/Restore)
# -------------------------------------------------------------------------
# Keep model classes used for JSON serialization to prevent breakage
-keep class com.besomuncu.healthcompanion.data.model.** { *; }

# Keep Gson specific attributes
-keepattributes Signature, *Annotation*, EnclosingMethod

# -------------------------------------------------------------------------
# Room Rules
# -------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * { @androidx.room.PrimaryKey *; }

# -------------------------------------------------------------------------
# General Optimization Rules
# -------------------------------------------------------------------------
# Add any project-specific rules below
