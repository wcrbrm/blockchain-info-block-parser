#!/bin/bash
if [ -z "$@" ]; then
    sudo docker-compose down
else 
    sudo docker-compose kill $@
fi

