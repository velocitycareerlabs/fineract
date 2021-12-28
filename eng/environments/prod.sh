#!/bin/bash
VER_NUM="$CURR_VERSION"
CONTAINER_MAIN_TAG="latest"
FINERACT_SECURITY_AUTH0_DOMAIN="vnf-prod.us.auth0.com"
FINERACT_SECURITY_AUTH0_ISSUER_URI="https://vnf-prod.us.auth0.com"
FINERACT_SECURITY_AUTH0_AUDIENCE="https://vnf-prod.us.auth0.com/userinfo" 


echo "VER_NUM=$VER_NUM" >> $GITHUB_ENV
echo "NPM_VER_NUM=$NPM_VER_NUM" >> $GITHUB_ENV
echo "CONTAINER_MAIN_TAG=$CONTAINER_MAIN_TAG" >> $GITHUB_ENV
echo "FINERACT_SECURITY_AUTH0_DOMAIN=$FINERACT_SECURITY_AUTH0_DOMAIN" >> $GITHUB_ENV
echo "FINERACT_SECURITY_AUTH0_ISSUER_URI=$FINERACT_SECURITY_AUTH0_ISSUER_URI" >> $GITHUB_ENV
echo "FINERACT_SECURITY_AUTH0_AUDIENCE=$FINERACT_SECURITY_AUTH0_AUDIENCE" >> $GITHUB_ENV
