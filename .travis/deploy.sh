#!/bin/bash
set -e
ssh deploy@ventas2.kazer.es 'bash -s' < .travis/ssh_script.sh