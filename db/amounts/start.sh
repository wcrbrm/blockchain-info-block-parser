#!/bin/bash
URL=http://localhost:9000
sudo docker-compose up -d --build $@

if [ -z "$@" ]; then
  if which xdg-open > /dev/null
  then
    xdg-open $URL
  fi
fi

# start watching logs
sudo docker-compose logs -f $@

