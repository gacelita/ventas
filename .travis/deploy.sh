#!/bin/bash
set -e
ssh travis@kazer.es <<'ENDSSH'
cd ventas
echo "Pulling from github"
git pull
echo "Building Docker images"
./build.sh
echo "Restarting service"
systemctl --user restart ventas
echo "Done"
ENDSSH