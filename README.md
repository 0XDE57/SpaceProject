# a space project
Welcome to A Space Project. A project involving space...
> Enjoy Galactic Space Exploration in a sate-of-the-art, hyper-realistic physics simulation of the entire universe!

> Get a realistic sense of the cosmic scale; there's literally dozens of planets and traveling between them could take up to entire minutes!

![screenshot](/Capture.PNG?raw=true)

***Game is in pre-alpha prototype phase. More of a sandbox than a game, there is not much content yet***

## Features
* Fly around the starsystem in a spaceship
* Faster Than Light Travel! (yeah it's real, cuz like quantum anti-dark matter n' stuff yo)
* Discover a plethora of astronomical bodies including:
    * Destructible asteroids using cutting edge - ***t r i ▲ n g l e s*** -
    * Unary star systems, Binary star systems, Trinary star systems, and even ~~Quadri... quatro? quadrino-ary?~~
    * lonely rogue planets who lost their sun :(
* Fight against other ship (disabled for now)
  * combat broken placeholder AI while I figure out the rest of the engine.
* Land on and explore planets (in theory, probably out of scope):
   * The planets are flat (ha, take that Round Earthers!)
      * Actually the planets are ***donuts*** (ha, take that Flat Earthers!)
* Controller Support (works in game but not menus)
   * hot plugging!
* Sound?
  * there is no sound in the vacuum of space silly
* Unit Tests?
   * pfft... my code is perfect. the first time. every time.
* Developer Tools (in progress)
* Cross-Platform Desktop and Mobile Support
  * Windows, OSX, Linux, Android, IOS
  * (it may be difficult to find sensible touchscreen controls, focus is desktop and controller)
* Feature Creep and Unrealistic Scope!
   * Multiplayer is out of current scope :(


### Controls
| Control                        | Desktop       | Controller/Gamepad    | Mobile (Android, iOS)                   |
|------------------------------- | ------------  | ------------------    | ----------------------------------------|
| Movement                       | WASD          | Left Stick + L1/R1    | Left Stick                              |
| Aim                            | Mouse         | Left Stick            | Left Stick                              |
| Boost                          | Space         | A                     | todo: needs button                      |
| Brakes                         | S             | X                     | todo: needs button                      |
| Attack: Shoot                  | Left-Click    | RT                    | bottom right button                     |
| Defense: Shield                | Shift         | LT                    | todo: needs button                      |
| Defense: Dodge (Barrel Roll)   | Double Tap A/D | Double Tap R1/L1     | todo: swipe gesture?                    |
| Switch Weapon                  | E             | D-Pad Right           | idk at this point, mobile might not have space for controls
| Toggle HyperDrive              | Hold 1        | Hold B                | todo: needs button                      |
| Interact (Undock from space station) + (Land/Take Off) | T             | D-Pad Down            | top center when over planet |
| Enter/Exit vehicle             | G             | Y                     | bottom right small button when in/near vehicle |
| Zoom                           | Scroll Wheel  | Right JoyStick        | todo: Pinch Zoom                        |
| Reset Zoom                     | Middle-Click  | Click in Right stick  | todo: double tap                        |
| Toggle Map State               | M             |                       | top left corner button                  |
| Toggle HUD                     | H             |                       |                                         |
| Full screen                    | F11           |                       |                                         |
| Menu (Pause)                   | Escape        | Start                 | top right corner button                 |
| Vsync                          | F8            |                       |                                         |
| ECS Debug Viewer               | F9            |                       |                                         |
| Misc debug keys I am too lazy to document rn and won't be permanent anyway |                                     |


## Current Status
A work in progress engine toy sandbox thing: scaffolding for what will hopefully one day be a game.
The code is bit rough in some places, littered with todo's, half-baked features, and the occasional bug.
For more documentation and current status see:

wiki: https://github.com/0XDE57/SpaceProject/wiki

project: https://github.com/users/0XDE57/projects/1


## License
Apache 2.0: see [LICENSE.md](/LICENSE.md)

Credit appreciated.


## Libraries
- [libGDX](https://github.com/libgdx/libgdx)
- [Box2D](https://box2d.org/)
- [Ashley](https://github.com/libgdx/ashley/wiki)
- [OpenSimplexNoise](https://gist.github.com/KdotJPG/b1270127455a94ac5d19)
- [VisUI](https://github.com/kotcrab/vis-ui)


## Building
**General**
* Set up your Development Environment: https://libgdx.com/wiki/start/setup
* Make sure Android SDK is installed.
* Import project in IDE of choice using gradle.
* If a "File not found" error occurs, check the working directory. Append "\assets" to the working directory in run configurations.


**IntelliJ / Android Studio**
* Desktop
  * create Run Configuration
  * main class = com.spaceproject.desktop.DesktopLauncher
  * use classpath of module 'desktop'
  * working directory = ...\SpaceProject\assets
      * (must ensure working directory includes assets so data like fonts, particles, shaders, configs can be loaded)
  * build and run!
* Android
  * enable dev options, enable usb debugging
  * connect phone, android studio should detect it
  * build and run!
* IOS
  * https://libgdx.com/dev/import_and_running/#ios

