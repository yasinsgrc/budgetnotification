# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# NotificationListenerService sistem tarafindan cagrilir
-keep class com.bildirimbutce.app.service.NotificationService { *; }
