#!/bin/sh

##############################################################################
## Gradle start up script for UN*X
##############################################################################

APP_HOME=$(cd "${0%/*}" >/dev/null 2>&1 && pwd -P)
APP_NAME="Gradle"

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

MAX_FD="maximum"

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MSYS* | MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar


if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/bin/java" ] ; then
        JAVACMD=$JAVA_HOME/bin/java
    else
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD=java
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

if [ "$cygwin" = "false" -a "$darwin" = "false" -a "$nonstop" = "false" ] ; then
    case $MAX_FD in
      max*)
        MAX_FD=$(ulimit -H -n) ||
            warn "Could not query maximum file descriptor limit"
    esac
    case $MAX_FD in
      '' | soft) :;;
      *)
        ulimit -n "$MAX_FD" ||
            warn "Could not set maximum file descriptor limit to $MAX_FD"
    esac
fi

if [ "$darwin" = "false" -a "$msys" = "false" ] ; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

if [ "$cygwin" = "false" -a "$msys" = "false" ] ; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Djava.io.tmpdir=$APP_HOME/tmp\""
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = "true" -o "$msys" = "true" ] ; then
    APP_HOME=$(cygpath --path --mixed "$APP_HOME")
    CLASSPATH=$(cygpath --path --mixed "$CLASSPATH")

    JAVACMD=$(cygpath --unix "$JAVACMD")

    for arg do
        if
            case $arg in
              -*)   false ;;
              /?*)  t=${arg#/} t=/${t%%/*}
                    [ -e "$t" ] ;;
              *)    false ;;
            esac
        then
            arg=$(cygpath --path --ignore --mixed "$arg")
        fi
        shift
        set -- "$@" "$arg"
    done
fi

set -- \
        "-Dorg.gradle.appname=$APP_NAME" \
        -classpath "$CLASSPATH" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"

exec "$JAVACMD" "$@"
