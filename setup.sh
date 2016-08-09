#!/usr/bin/env bash

echo -e "${green}Install or update npm dependencies for explainer-server${plain}"

# Install explainer-server dependencies and run a front end build
pushd explainer-server
npm install
npm run build
popd