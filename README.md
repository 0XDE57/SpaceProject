# a space project
Welcome to A Space Project. A project involving space...
> Enjoy Galactic Space Exploration in a sate-of-the-art, hyper-realistic physics simulation of the entire universe!

> Get a realistic sense of the cosmic scale; there's literally dozens of planets and traveling between them could take up to entire minutes!

![screenshot](/Capture.PNG?raw=true)

***This game is in pre-alpha prototype phase. More of an sandbox than a game, there's not much content***

## Features
* Fly around the starsystem in a spaceship
* Faster Than Light Travel! (yeah it's real, cuz like quantum anti-dark matter n' stuff yo)
* Destructable asteroids using cutting edge - ***t r i ▲ n g l e s*** -
* Discover a plethora of astronomical bodies including:
    * Unary star systems, Binary star systems, Trinary star systems, and even ~~Quadri... quatro? quadrino-ary?~~ 
    * as-many-as-you-want star systems!
    * lonely rogue planets who lost their sun :(
* Fight against other ship (disabled for now)
  * combat broken placeholder AI while I figure out the rest of the engine.
* Then when you're bored of that you can land on a planet I guess (in theory, probably out of scope):
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
* Feature Creep and Unrealistic Scope!
   * Multiplayer is out of current scope :(


### Controls
| Control                        | Desktop       | Controller/Gamepad    | Mobile (Android, iOS)                   |
|------------------------------- | ------------  | ------------------    | ----------------------------------------|
| Movement                       | WASD          | Left Joystick + L1/R1 | Left Joystick                           |
| Aim                            | Mouse         | Left Joystick         | Left Joystick                           |
| Boost                          | Space         | X                     | todo: needs button                      |
| Attack: Shoot                  | Right-Click   | A                     | bottom right button                     |
| Defense: Shield                | Shift         | B                     | todo: needs button                      |
| Defense: Dodge (Barrel Roll)   | A/D + Space (or) Double Tap A/D | Double Tap R1/L1 | todo: swipe gesture?       |
| Toggle HyperDrive              | Hold 1        | Hold D-Pad Up         | todo: needs button                      |
| Land/Take Off                  | T             | D-Pad Down            | top center when over planet             |
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

It may be difficult to find sensible touchscren controls, I am focused more on desktop and controller.


## Current Status
A work in progress engine toy sandbox thing: scaffolding for what will hopefully one day be a game.
Currently focused on figuring out some rendering issues with how shaders work.
The code is bit rough in some places, littered with todo's, half-baked features, and of course the occasional bug.
  



## License
   Apache 2.0: see [LICENSE.md](/LICENSE.md)
   Credit appreciated.

## Libraries
- [libGDX](https://github.com/libgdx/libgdx)
- [Ashley](https://github.com/libgdx/ashley/wiki)
- [OpenSimplexNoise](https://gist.github.com/KdotJPG/b1270127455a94ac5d19)
- [VisUI](https://github.com/kotcrab/vis-ui)


## Building
**General**
* Set up your [Development Environment](https://libgdx.badlogicgames.com/documentation/gettingstarted/Setting%20Up.html)
* Make sure Android SDK is installed.
* Import project in IDE of choice using gradle.
* If a "File not found" error occurs, check the working directory. Append "\assets" to the working directory in run configurations.


**Android Studio**
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

