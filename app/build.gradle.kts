plugins {
    id("com.android.application")
    kotlin("android")
}

val takeoffProductionEndpoint = providers.environmentVariable("TAKEOFF_PRODUCTION_ENDPOINT")
    .orElse("https://takeoff-seven-puce.vercel.app").get().trim().trimEnd('/')
val escapedTakeoffProductionEndpoint = takeoffProductionEndpoint.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "ai.takeoff.insightscompanion"
    compileSdk = 35
    defaultConfig {
        applicationId = "ai.takeoff.insightscompanion"
        minSdk = 26; targetSdk = 35
        versionCode = 61
        versionName = "0.20.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TAKEOFF_PRODUCTION_ENDPOINT", "\"$escapedTakeoffProductionEndpoint\"")
    }
    signingConfigs { create("release") { val p=System.getenv("KEYSTORE_PATH"); if(p!=null){storeFile=file(p);storePassword=System.getenv("KEYSTORE_PASSWORD");keyAlias=System.getenv("KEY_ALIAS");keyPassword=System.getenv("KEY_PASSWORD")} } }
    buildFeatures { buildConfig = true }
    buildTypes {
        debug { isMinifyEnabled=true;isShrinkResources=true;proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro") }
        release { isMinifyEnabled=true;isShrinkResources=true;signingConfig=if(System.getenv("KEYSTORE_PATH").isNullOrBlank()) signingConfigs.getByName("debug") else signingConfigs.getByName("release");proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro") }
    }
    compileOptions { sourceCompatibility=JavaVersion.VERSION_17;targetCompatibility=JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget="17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
}

// 0.19.2: tactile 3D glass depth, multi-endpoint failover, and elevated visual intelligence.
