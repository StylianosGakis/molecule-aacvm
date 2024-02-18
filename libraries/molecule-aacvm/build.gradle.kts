plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.molecule)
  alias(libs.plugins.nmcp)
  `maven-publish`
}

version = property("VERSION_NAME") as String

kotlin {
  explicitApi()

  androidTarget {
    publishLibraryVariants("release")
    compilations.all {
      kotlinOptions {
        jvmTarget = "1.8"
      }
    }
  }
  jvm()

  applyDefaultHierarchyTemplate()
  sourceSets {
    commonMain.dependencies {
      implementation(libs.coroutines)
    }
    androidMain.dependencies {
      api(libs.androidx.viewmodel)
    }
  }
}

android {
  namespace = "com.stylianosgakis.molecule.aacvm"
  compileSdk = libs.versions.compileSdk.get().toInt()
  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}

nmcp {
  publishAllPublications {
    username = findProperty("MAVEN_CENTRAL_USERNAME") as String
    password = findProperty("MAVEN_CENTRAL_PASSWORD") as String
    publicationType = "USER_MANAGED"
  }
}
