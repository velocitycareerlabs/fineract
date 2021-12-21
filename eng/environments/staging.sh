#!/bin/bash
VER_NUM="testnet-$CURR_VERSION-build.$GITHUB_SHA_SHORT"
NPM_VER_NUM="$CURR_VERSION-build.$GITHUB_SHA_SHORT"
CONTAINER_MAIN_TAG="testnet"
VNF_OAUTH_TOKENS_ENDPOINT="https://vnf-staging.us.auth0.com/oauth/token"

echo "VER_NUM=$VER_NUM" >> $GITHUB_ENV
echo "NPM_VER_NUM=$NPM_VER_NUM" >> $GITHUB_ENV
echo "CONTAINER_MAIN_TAG=$CONTAINER_MAIN_TAG" >> $GITHUB_ENV
echo "VNF_OAUTH_TOKENS_ENDPOINT=$VNF_OAUTH_TOKENS_ENDPOINT" >> $GITHUB_ENV
