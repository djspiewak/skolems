/*
 * Copyright 2019 Daniel Spiewak
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

name := "skolems"

ThisBuild / baseVersion := "0.2"

ThisBuild / organization := "com.codecommit"
ThisBuild / publishGithubUser := "djspiewak"
ThisBuild / publishFullName := "Daniel Spiewak"

ThisBuild / homepage := Some(url("https://github.com/djspiewak/skolems"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/djspiewak/skolems"),
    "git@github.com:djspiewak/skolems.git"))

scalacOptions -= "-Ywarn-dead-code"

mimaPreviousArtifacts := {
  // just not going to make binary compatibility guarantees about 2.11
  if (scalaVersion.value.startsWith("2.11"))
    Set()
  else
    mimaPreviousArtifacts.value
}
