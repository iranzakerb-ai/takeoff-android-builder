# Keep WorkManager-instantiated worker constructors and the MediaProjection service/activity names.
-keep class ai.takeoff.insightscompanion.PendingWorker { public <init>(...); }
-keep class ai.takeoff.insightscompanion.CaptureService { *; }
-keep class ai.takeoff.insightscompanion.CaptureTriggerActivity { *; }
