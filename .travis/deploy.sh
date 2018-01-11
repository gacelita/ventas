#!/bin/bash
set -e
ssh travis@kazer.es <<'ENDSSH'
cd ventas
./build.sh
systemctl --user restart ventas
ENDSSH