# Zoned &nbsp; [![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0) [![Version: 0.0.3-dev](https://img.shields.io/badge/version-0.0.3--dev-orange)](https://github.com/Spikatrix/Zoned/releases/tag/v0.0.3-dev)

<img src="https://i.stack.imgur.com/cunrq.png" alt="Zoned Icon" align="left" style="margin: 10px 20px 10px 10px; border-radius: 20%; box-shadow: 0 6px 20px 2px black" height="162px">

Zoned is a cross-platform grid based multiplayer strategy game powered by [libGDX][libGDX]. The game is primarily targeted for Android, but it will work just fine on Windows and Linux as well.

It is available to download on the [Google Play Store][play_store_page] (Android) as well as on [GitHub][github_release_page] (Windows, Linux and Android)

<p>
	<a href="https://play.google.com/store/apps/details?id=com.cg.zoned">
		<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" alt="Download from Google Play" height="80px">
	</a>
	<a href="https://github.com/Spikatrix/Zoned/releases">
		<img src="https://i.stack.imgur.com/S4MQt.png" alt="Download from GitHub" height="80px">
	</a>
</p>

## About

Zoned is multiplayer strategy game. Every game is played on a grid based map where the objective is to capture as many cells in the grid as possible. Currently, it supports splitscreen multiplayer as well as cross-platform local network multiplayer. In addition to the default internal maps, Zoned also supports [custom external maps][custom_external_map_wiki] which you can play on if you get bored of the default ones. The best part is that Zoned is free of ads and will never have ads!

The game is currently in beta. I've made a [list of work][beta_work] to be done before v1.0.0 stable is released. There may be additional changes as well.

## Screenshots

<img src="https://lh3.googleusercontent.com/SKRnPCRlO8k8-Ks8A22rI3spRsJ6G-r73kZOgUGrbOsBG04esMiOnRtzvRhyK1rRWA=w720-h310-rw" width="46%"> &nbsp; &nbsp;
<img src="https://lh3.googleusercontent.com/6uD4h1HkZLwxhmt2gtZnRiCTLW-QVmF-F9XsVK9HHCy5GUbRpvJm8aLKLNTlnlnok3Bw=w720-h310-rw" width="46%"> &nbsp; &nbsp;

More screenshots are available in the [Google Play Store page][play_store_page]

## Build from source

If you wish to build from source, clone the repo and run gradle's `android:assembleDebug` and `desktop:dist` tasks for building the Android and Desktop binaries respectively:

    $ git clone https://github.com/Spikatrix/Zoned
    $ cd Zoned
    $ ./gradlew android:assembleDebug // Android
    $ ./gradlew desktop:dist          // Desktop

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
[beta_work]: https://github.com/Spikatrix/Zoned/projects/1
[issue_tracker]: https://github.com/Spikatrix/Zoned/issues
[email_feedback]: mailto:cg.devworks@gmail.com?subject=Zoned+Feedback&body=Your+feedback+here
[custom_external_map_wiki]: https://github.com/Spikatrix/Zoned/wiki/Custom-External-Maps
[project_license]: https://github.com/Spikatrix/Zoned/blob/master/LICENSE
