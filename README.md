<img src="https://github.com/ActualTomato/Chip8Emu/assets/73549035/12184a9b-ce9b-4372-bd30-b04b3c753e5d" width="48">
# Chip8 Virtual Machine / Emulator
## Background
I created this virtual machine/emulator as an exercise, and to familiarize myself with JavaFX, emulator design, and machine code.

## Features
* Emulation of the original Chip8 specification
* Clean JavaFX-based user interface
* Ability to enable and disable "quirks"
  * Implemented quirks:
    * Shift Quirk
    * Memory Increment by X Quirk
    * Leave I Unchanged Quirk
    * Jump Quirk
    * Wrap Quirk
    * Math VF Reset Quirk
* Save States
* Sound (simple beeper)
* Pausing
* Configuration of emulation as well as emulator itself
  * Saved to system using Java Preferences API, persistent when reopening
  * Configurable screen size multiplication factor
  * Configurable emulation speed
  * Configurable keybinds for hexadecimal keypad
  * Configurable colors

## Limitations
* SuperChip and other specifications are not implemented
* Save states made on older versions are not compatible with newer releases of the emulator
  * Limitation of Java serialization
* Beeper may not sound properly on certain devices
  * Was a hasty modification of an existing sound implementation from [Michael Arnauts](https://github.com/michaelarnauts)
* Emulator is only compatible with compiled binaries in .rom format, not chip8 assembly files
* Emulation speed may not be consistent
  * Keypad input may cause a momentary stutter in emulation

## Notes
The "LOAD GAME" screen that shows when opening the emulator is actually a Chip8 ROM that I programmed myself in hex! The emulator loads it up by default and runs it after starting up.

Project was created and tested on an M2 Macbook Pro 14 running MacOS.

## Screenshots and Videos
![java-17062024-19-06@2x](https://github.com/ActualTomato/Chip8Emu/assets/73549035/3f7959a4-a07a-4c2b-bf27-8d30c1d5fd07)
![java-17412024-19-41@2x](https://github.com/ActualTomato/Chip8Emu/assets/73549035/a2b85210-eb99-452c-aa92-d167b9c62b7e)
![java-17432024-19-43@2x](https://github.com/ActualTomato/Chip8Emu/assets/73549035/43ab8f33-c67f-4fc7-937b-b4b08e18632c)
![ezgif-7-7c68fc0bcb](https://github.com/ActualTomato/Chip8Emu/assets/73549035/90e86ed4-4d79-43f8-9343-5f6cfe290d50)

## Resources
Below is a short list of resources I referenced in order to create this implementation:
* https://en.wikipedia.org/wiki/CHIP-8
* https://github.com/chip-8/chip-8-database
* https://github.com/michaelarnauts/chip8-java/blob/master/Source/src/be/khleuven/arnautsmichael/chip8/ISound.java
* http://devernay.free.fr/hacks/chip8/C8TECH10.HTM
* https://github.com/Timendus/chip8-test-suite
* https://austinmorlan.com/posts/chip8_emulator/
* https://multigesture.net/articles/how-to-write-an-emulator-chip-8-interpreter/
* EmuDev discord server
