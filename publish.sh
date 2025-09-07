#!/usr/bin/env sh

set -ex

if [ $# -ne 1 ] ; then
  echo "version is required"
  exit 1
fi

git checkout main
git pull

version="$1"
tag="v${version}"

git checkout -b "$version"

echo "version=$version" >> gradle.properties

git add gradle.properties
git commit -m "set version $version"

git tag -a "$tag" -m "json-repairj v$version"
git push origin $tag
