#!/bin/bash

function etcdctl_cmd() {
    etcdctl --endpoints="http://docker:23791,http://docker:23792,http://docker:23793" $*
}

function up() {
docker-compose up -d
}

function down() {
docker-compose stop && docker-compose rm -s -f
}

function restart() {
     down;
     up;
}

command=$1;
shift;

case "$command" in
    up)
        up
        echo ""
    ;;
    down)
        down
        echo ""
    ;;
    restart)
        restart
        echo ""
    ;;
    etcdctl)
        etcdctl_cmd $@
        echo ""
    ;;
    *)
        echo "Usage: ./manage.sh etcdctl|up|down|restart"
        exit 1
esac

exit 0