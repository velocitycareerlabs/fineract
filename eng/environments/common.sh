#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#


CURR_VERSION="1.15.1"
TARGET_CONTAINER_REG="ghcr.io"
TARGET_ORG="velocitycareerlabs"
TARGET_REPO="packages"
CONTAINER_REPOSITORY_NAME="$TARGET_ORG/$SERVER_NAME"
AWS_REGION="us-east-1"

echo "CURR_VERSION=$CURR_VERSION" >> $GITHUB_ENV
echo "TARGET_CONTAINER_REG=$TARGET_CONTAINER_REG" >> $GITHUB_ENV
echo "TARGET_ORG=$TARGET_ORG" >> $GITHUB_ENV
echo "TARGET_REPO=$TARGET_REPO" >> $GITHUB_ENV
echo "CONTAINER_REPOSITORY_NAME=$CONTAINER_REPOSITORY_NAME" >> $GITHUB_ENV
echo "AWS_REGION=$AWS_REGION" >> $GITHUB_ENV
