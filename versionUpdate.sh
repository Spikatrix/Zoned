#!/bin/bash

read -p "Old version name: " oldVersionName
read -p "New version name: " newVersionName
read -p "Are you sure you wanna update the version? (Y/N) " proceed

if [ "$proceed" == "Y" ]; then
	files=(
		"android/build.gradle"
		"desktop/build.gradle"
		"core/src/com/cg/zoned/Constants.java"
		"README.md"
	)

	sed -i -e "s/$oldVersionName/$newVersionName/g" "${files[@]}"

	hyphenPos=`echo "$oldVersionName" | grep '-' -oba --color=never | cut -c1`
	oldHyphenDoubled=`echo "${oldVersionName:0:$hyphenPos}-${oldVersionName:$hyphenPos}"`
	newHyphenDoubled=`echo "${newVersionName:0:$hyphenPos}-${newVersionName:$hyphenPos}"`

	sed -i -e "s/$oldHyphenDoubled/$newHyphenDoubled/g" "README.md"

	sed -i -e "s/ENABLE_DISCORD_RPC = false/ENABLE_DISCORD_RPC = true/g" "core/src/com/cg/zoned/Constants.java"

	echo "Done :D"
else
	echo "Alright, aborted :/"
fi
