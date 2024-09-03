/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// repositories of plugins
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

// repositories of project dependencies
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
    id("com.android.sts.sdksubmission") version "1.0.0" apply false
    id("com.android.sts.apptest") version "1.0.0" apply false
    id("com.android.sts.javahosttest") version "1.0.0" apply false
}

rootProject.name = "sts-sdk-example"

// glob and include gradle projects
fileTree(rootDir) {
        // only include subprojects of ":submission"
        include("submission/**/build.gradle*")
        // don't include subprojects from build directories
        exclude("**/build")
    }
    .forEach {
        val path = rootDir.toPath().relativize(it.toPath().getParent())
        val gradlePath = path.toString().replace('/', ':')
        include(gradlePath)
    }
