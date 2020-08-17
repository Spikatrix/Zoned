#!/bin/bash

read -p "Old version name: " oldVersionName
read -p "New version name: " newVersionName
read -p "Are you sure you wanna do this? (Y/N) " proceed

if [ "$proceed" == "Y" ]; then
	grep -l "$oldVersionName" android/build.gradle | while read filename
	do
		sed -i -e "s/$oldVersionName/$newVersionName/g" "$filename"
	done

	grep -l "$oldVersionName" desktop/build.gradle | while read filename
	do
		sed -i -e "s/$oldVersionName/$newVersionName/g" "$filename"
	done

	grep -l "$oldVersionName" core/src/com/cg/zoned/Constants.java | while read filename
	do
		sed -i -e "s/$oldVersionName/$newVersionName/g" "$filename"
	done

	hyphenPos=`echo "$oldVersionName" | grep '-' -oba --color=never | cut -c1`
	oldHyphenDoubled=`echo "${oldVersionName:0:$hyphenPos}-${oldVersionName:$hyphenPos}"`
	newHyphenDoubled=`echo "${newVersionName:0:$hyphenPos}-${newVersionName:$hyphenPos}"`

	grep -l "$oldHyphenDoubled" README.md | while read filename
	do
		sed -i -e "s/$oldHyphenDoubled/$newHyphenDoubled/g" "$filename"
	done

	grep -l "$oldVersionName" README.md | while read filename
	do
		sed -i -e "s/$oldVersionName/$newVersionName/g" "$filename"
	done

	grep -l "ENABLE_DISCORD_RPC = false" core/src/com/cg/zoned/Constants.java | while read filename
	do
		sed -i -e "s/ENABLE_DISCORD_RPC = false/ENABLE_DISCORD_RPC = true/g" "$filename"
	done
else
	echo "Alright, aborted"
fi
