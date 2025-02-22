# Zoned &nbsp; [![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0) [![Version: 0.0.5-beta](https://img.shields.io/badge/version-0.0.5--beta-orange)](https://github.com/Spikatrix/Zoned/releases/tag/v0.0.5-beta)

<img src="https://github.com/Spikatrix/Zoned/assets/12792882/327b3260-ffea-4ca2-84b8-f22ea4fb13bc" alt="Zoned Icon" align="left" style="margin: 10px 20px 10px 10px; border-radius: 20%; box-shadow: 0 6px 20px 2px black" height="162px">

Zoned is a cross-platform grid based multiplayer strategy game powered by [libGDX][libGDX]. The game is primarily targeted for Android, but will work on Windows and Linux as well.

It is available to download on [GitHub][github_release_page] (Windows, Linux, Android and [Web][gwt_release_page])

<p>
	<a href="https://github.com/Spikatrix/Zoned/releases">
		<img src="https://github.com/Spikatrix/LRC-Editor/assets/12792882/cf29751a-93bf-4f47-96b2-99716653e3ba" alt="Download from GitHub" height="80px">
	</a>
</p>

## About

Zoned is a multiplayer strategy game. Every game is played on a grid based map where the objective is to capture as many cells in the grid as possible. Zoned supports both splitscreen multiplayer as well as cross-platform local network multiplayer. In addition to the default internal maps, Zoned also supports [custom external maps][custom_external_map_wiki] which you can play on if you ever get bored of the default ones. The best part is that Zoned is free of ads and will never have ads!

The game is in beta. However, I've stopped working on Zoned and have moved on to other projects. 

## Screenshots

<img src="https://user-images.githubusercontent.com/12792882/112144302-0a42bf80-8bff-11eb-8390-aadf9f4bcabf.png" width="46%"> &nbsp; &nbsp;
<img src="https://user-images.githubusercontent.com/12792882/112144297-07e06580-8bff-11eb-87c9-e9e2f39318f0.png" width="46%"> &nbsp; &nbsp;

## Build from source

If you wish to build from source, clone the repo and run gradle's `android:assembleDebug` and `desktop:dist` tasks for building the Android and Desktop binaries respectively:

    git clone https://github.com/Spikatrix/Zoned
    cd Zoned
    ./gradlew android:assembleDebug # Android
    ./gradlew desktop:dist          # Desktop

(Use `gradlew.bat` if you're on Windows)

You can then find the generated binaries at
 - Desktop: `Zoned/desktop/build/libs/*.jar`
 - Android: `Zoned/android/build/outputs/apk/debug/*.apk`

## Contributing

Contributions are always welcome.

Here are a few ways you can help:
 * Report bugs via the [Issue Tracker][issue_tracker] and suggestions via [email][email_feedback]
 * Send in pull requests improving the code quality and fixing bugs among others

## License

This project is licensed under the [GNU GPLv3 License][project_license]

<!-- Link references -->
[libGDX]: https://github.com/libgdx/libgdx
[play_store_page]: https://play.google.com/store/apps/details?id=com.cg.zoned
[github_release_page]: https://github.com/Spikatrix/Zoned/releases
[gwt_release_page]: https://spikatrix.github.io/Zoned/
[issue_tracker]: https://github.com/Spikatrix/Zoned/issues
[email_feedback]: mailto:cg.devworks@gmail.com?subject=Zoned+Feedback&body=Your+feedback+here
[custom_external_map_wiki]: https://github.com/Spikatrix/Zoned/wiki/Custom-External-Maps
[project_license]: https://github.com/Spikatrix/Zoned/blob/master/LICENSE

