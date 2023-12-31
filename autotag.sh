#!/bin/bash

ls

#get highest tag number
VERSION=$(git describe --abbrev=0 --tags)

#replace . with space so can split into an array
set -f
VERSION="${VERSION//./ }"
VERSION_BITS=($VERSION)

#get number parts and increase last one by 1
VNUM=${VERSION_BITS[0]}
VNUM2=${VERSION_BITS[1]}
VNUM3=${VERSION_BITS[2]}

VNUM="${VNUM//v/ }"

# Check for #major or #minor in commit message and increment the relevant version number
MAJOR=$(git log --format=%B -n 1 HEAD | grep '#major')
MINOR=$(git log --format=%B -n 1 HEAD | grep '#minor')

if [ ! -z "$MAJOR" ]; then
    echo "Update major version"
    VNUM=$((VNUM+1))
    VNUM2=0
    VNUM3=0
elif [ ! -z "$MINOR" ]; then
    echo "Update minor version"
    VNUM2=$((VNUM2+1))
    VNUM3=0
else
    echo "Update patch version"
    VNUM3=$((VNUM3+1))
fi


#create new tag
NEW_TAG="$VNUM.$VNUM2.$VNUM3"

echo "Updating $VERSION to $NEW_TAG"

#get current hash and see if it already has a tag
GIT_COMMIT=$(git rev-parse HEAD)
NEEDS_TAG=$(git describe --contains "$GIT_COMMIT")

#only tag if no tag already (would be better if the git describe command above could have a silent option)
if [ -z "$NEEDS_TAG" ]; then
    echo "Tagged with $NEW_TAG (Ignoring fatal:cannot describe - this means commit is untagged) "
    git tag $NEW_TAG
    git remote set-url origin "https://gitlab-ci-token:${ACCESS_TOKEN}@${CI_SERVER_HOST}/${CI_PROJECT_PATH}.git"
    git push --tags
else
    echo "Already a tag on this commit"
fi

