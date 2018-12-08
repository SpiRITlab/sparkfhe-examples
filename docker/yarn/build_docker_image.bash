#!/bin/bash

image_name="spiritlab/sparkfhe-hadoop-cluster"
# image_name="$image_name:new"

network_name="hadoop"

echo -e "Building docker hadoop image\n"

# Remove the relevant image to prevent dangling
docker rmi $image_name

# Create Docker Image
docker build -t $image_name .