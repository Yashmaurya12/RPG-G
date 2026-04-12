#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Maven2 Toolkit for modular projects
#
# Any positional parameters will be passed as-is to Maven
# ----------------------------------------------------------------------------

# Argument splitting based on POSIX shell
# (adapted from https://stackoverflow.com/a/13350435)
case "$(uname)" in
    CYGWIN* | MINGW*)
        [ -n "$JAVA_HOME" ] && JAVA_HOME=$(cygpath --unix "$JAVA_HOME")
        ;;
esac

# For bash, ksh, zsh, etc.
if [ -n "$BASH_VERSION" ] || [ -n "$KSH_VERSION" ] || [ -n "$ZSH_VERSION" ]; then
    set -f
fi

# Locate the Maven wrapper jar
wrapper_jar=".mvn/wrapper/maven-wrapper.jar"
if [ ! -f "$wrapper_jar" ]; then
    echo "Error: Could not find maven-wrapper.jar at $wrapper_jar" >&2
    exit 1
fi

# Locate java
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Check if java is available
if ! command -v "$JAVACMD" >/dev/null 2>&1; then
    echo "Error: java command not found." >&2
    exit 1
fi

# Execute the wrapper
exec "$JAVACMD" \
  $MAVEN_OPTS \
  -jar "$wrapper_jar" \
  "$@"
