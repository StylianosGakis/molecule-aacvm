pluginManagement {
  repositories {
    google()
    gradlePluginPortal()
  }
}

rootProject.projectDir
  .resolve("libraries")
  .listFiles()!!
  .filter { it.isDirectory }
  .filter { File(it, "build.gradle.kts").exists() }
  .forEach {
    include(it.name)
    project(":${it.name}").projectDir = it
  }

rootProject.name = "molecule-aacvm"

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
    google()
  }
}
