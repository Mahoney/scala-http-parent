#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

action=${1:-}; shift

MVN=mvn

if [ -f .env ]; then
  . .env
fi

now=$(date +%Y-%m-%dT%H:%M)

function doBuild {
    set +e
    env MAVEN_OPTS="-Xmx1g" time ${MVN} "$@" | tee "/tmp/financials-buildoutput-$now.txt"
    result=$?
    set -e
    if [ $result -eq 0 ]
    then echo "********* BUILD SUCCESSFUL *********"
    else echo "!!!!!!!!! BUILD FAILED !!!!!!!!!"
    fi
    echo "Output can be found at /tmp/financials-buildoutput-$now.txt"
    exit $result
}

case $action in
     "push")
        for D in */; do pushd "$D" && git push $@ && popd; done
        ;;
     
     "pull")
        for D in */; do pushd "$D" && git pull $@ && popd; done
        ;;

     "build")
        doBuild -T 1.5C verify $@
        ;;

    "compile")
        doBuild -T 1.5C package -DskipTests $@
        ;;

    "run")
        doBuild verify -DskipTests -Dexec.args="$@"
        ;;

    *)
        echo "Sorry, no idea what you mean by '$action'"
        echo "Usage:"
        echo "$0 build | compile | run"
        exit 1
    ;;
esac

